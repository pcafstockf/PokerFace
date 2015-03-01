package com.bytelightning.opensource.pokerface;
/*
The MIT License (MIT)

PokerFace: Asynchronous, streaming, HTTP/1.1, scriptable, reverse proxy.

Copyright (c) 2015 Frank Stock

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.commons.pool2.ObjectPool;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes each new request from a client (the content of which will not yet be fully received), 
 * checks for a matching JavaScript endpoint and invokes it's <code>inspectRequest</code> method if it exist, 
 * analyzes the script response (and if appropriate delegates further processing to the script),
 * otherwise resolves the remote Target (if any), 
 * and finally creates either a <code>RequestForScriptConsumer</code> or a <code>RequestForTargetConsumer</code> to actually consume the request.
 * NOTE: This is a singleton (not one per transaction as is the case for the Producers and Consumers).
 */
@SuppressWarnings("restriction")
public class RequestHandler implements HttpAsyncRequestHandler<ResponseProducer> {
	protected static final Logger Logger = LoggerFactory.getLogger(RequestHandler.class.getPackage().getName());

	/**
	 * Primary constructor.
	 * @param executor	<code>HttpAsyncRequester</code> which will perform the actual request to the remote Target and receive it's response.
	 * @param connPool	The client connection pool that will be used by the <code>executor</code>
	 * @param patternTargetMapping	The mapping of relative uri paths to configured Targets.
	 * @param scripts	Mapping of all JavaScript endpoints.  This map *may* be dynamically updated, or it may be null to reflect that JavaScript endpoints are not configured.
	 * @param dynamicHostMap	If non-null, we will allow JavaScript endpoints to proxy to remote Target's not specified in the configuration file.
	 */
	public RequestHandler(HttpAsyncRequester executor, BasicNIOConnPool connPool, ObjectPool<ByteBuffer> bufferPool, 
					Path staticFilesPath, Map<String, TargetDescriptor> patternTargetMapping, NavigableMap<String, ScriptObjectMirror> scripts,
					ConcurrentMap<String, HttpHost> dynamicHostMap) {
		this.executor = executor;
		this.connPool = connPool;
		this.bufferPool = bufferPool;
		this.staticFilesPath = staticFilesPath;
		this.patternTargetMapping = patternTargetMapping;
		this.scripts = scripts;
		this.dynamicHostMap = dynamicHostMap;
		this.idCounter = new AtomicLong(1);
	}
	private final BasicNIOConnPool connPool;
	private final HttpAsyncRequester executor;
	private final ObjectPool<ByteBuffer> bufferPool;
	private final Path staticFilesPath;
	private final ConcurrentMap<String, HttpHost> dynamicHostMap;
	private final Map<String, TargetDescriptor> patternTargetMapping;
	private final NavigableMap<String, ScriptObjectMirror> scripts;
	private final AtomicLong idCounter;
	
	/**
	 * {@inheritDoc}
	 * Main entry point of a client request into this server.
	 * This method creates a unique id for the request / response transaction, looks for a matching javascript endpoint (which if found is given an opportunity to inspect the request).
	 * If an endpoint is found and it wishes to handle the request, an appropriate <code>HttpAsyncRequestConsumer<ResponseProducer></code> is returned to make that happen.
	 * Otherwise the request (which may have been modified by the endpoint) is asynchronously sent off to a matching remote Target via <code>RequestForTargetConsumer</code>.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public HttpAsyncRequestConsumer<ResponseProducer> processRequest(HttpRequest request, HttpContext context) {
		// Create an internal id for this request.
		String txId = String.format("%08X", idCounter.getAndIncrement());
		context.setAttribute("pokerface.txId", txId);
		boolean requestIsFromScript = false;
		HttpContext scriptContext = null;
		ScriptObjectMirror scriptEndpoint = null;
		RequestLine requestLine = request.getRequestLine();
		String uriStr = requestLine.getUri();
		int queryPos = uriStr.lastIndexOf('?');
		if (queryPos > 0)
			uriStr = uriStr.substring(0, queryPos);
		int anchorPos = uriStr.lastIndexOf('#');	// You could have an anchor without a query
		if (anchorPos > 0)
			uriStr = uriStr.substring(0, anchorPos);
		context.setAttribute("pokerface.uripath", uriStr);

		// See if this is a request for one of our static resources.
		if (staticFilesPath != null) {
			String method = requestLine.getMethod();
			if (method.equals("GET") || method.equals("HEAD")) {
				try {
					Path rsrcPath = staticFilesPath.resolve(uriStr.replaceAll("\\.\\./", "/").substring(1));
					if (Files.exists(rsrcPath))
						return new RequestForFileConsumer(context, rsrcPath.toFile());
				}
				catch (Exception ex) {
					Logger.warn("Error resolving URI path", ex);
				}
			}
		}
		
		BufferIOController requestBuffer = new BufferIOController(bufferPool);
		BufferIOController responseBuffer = new BufferIOController(bufferPool);
		
		// If script endpoints are configured, look for the closest match (if any).
		if (scripts != null) {
			uriStr = uriStr.toLowerCase();
			Entry<String, ScriptObjectMirror> entry = scripts.floorEntry(uriStr);
			if ((entry != null) && uriStr.startsWith(entry.getKey())) {
				// We found a matching script, so give it an opportunity to inspect the request.
				StringBuilder sb = new StringBuilder();
				RequestLine reqLine = request.getRequestLine();
				sb.append(reqLine.getMethod());
				sb.append(" ");
				sb.append(reqLine.getUri());
				sb.append(" being inspected by ");
				String logPrefix = sb.toString();
				scriptContext = new BasicHttpContext(context);
				scriptContext.setAttribute("pokerface.scriptHelper", new ScriptHelperImpl(request, context, bufferPool));
				scriptContext.setAttribute("pokerface.scripts", scripts);
				scriptContext.setAttribute("pokerface.scriptLogger", ScriptHelper.ScriptLogger);
//FIXME: Test out the recursively higher script selection.
//FIXME: Add ignore of # directories (don't forget to update the file watcher to ignore them as well)
				// Call recursively higher (closer to the root) scripts until one is interested.
				Object scriptResult = null;
				while (true) {
					sb.setLength(0);
					sb.append(logPrefix);
					String key = entry.getKey();
					sb.append(key);
					if (key.endsWith("/"))
						sb.append("?");
					sb.append(".js");
					Logger.info(sb.toString());
					scriptEndpoint = entry.getValue();
					scriptResult = scriptEndpoint.callMember("inspectRequest", request, scriptContext);
					scriptResult = transformScriptInspectionResult(request, scriptResult);
					if (scriptResult == null) {
						if (uriStr.length() > 1) {
							int lastSlash = uriStr.lastIndexOf('/', uriStr.length());
							if (lastSlash >= 0) {
								uriStr = uriStr.substring(0, lastSlash + 1);	// Add a slash to see if there is a directory script
								entry = scripts.floorEntry(uriStr);
								if ((entry != null) && uriStr.startsWith(entry.getKey()))
									continue;
								if (lastSlash > 0) {
									uriStr = uriStr.substring(0, uriStr.length()-1);	// Drop the slash and try again.
									entry = scripts.floorEntry(uriStr);
									if ((entry != null) && uriStr.startsWith(entry.getKey()))
										continue;
								}
							}
						}
					}
					break;
				}
				// Process the scripts response (if any).
				if (scriptResult != null) {
					if (scriptResult instanceof HttpAsyncRequestConsumer<?>)
						return (HttpAsyncRequestConsumer<ResponseProducer>) scriptResult;
					else if ((scriptResult instanceof ScriptObjectMirror) && scriptEndpoint.equals(scriptResult))	// The script wants to handle the request itself.
						return new RequestForScriptConsumer(scriptContext, requestBuffer, new ScriptResponseProducer(scriptEndpoint, request, scriptContext, responseBuffer));
					else {
						// The script wants to pass along a modified Request to the target
						assert scriptResult instanceof HttpRequest;
						request = (HttpRequest) scriptResult;
						requestIsFromScript = true;
					}
				}
				// else no script cared about this request, so just fall through to normal processing.
			}
		}
		// Create a AbsClientRequestConsumer that can proxy to the remote Target.
		return new RequestForTargetConsumer(scriptContext == null ? context : scriptContext, executor, connPool, patternTargetMapping, requestIsFromScript ? dynamicHostMap : null, request, requestBuffer, responseBuffer, scriptEndpoint);
	}

	/**
	 * This method converts the response from a JavaScript endpoint's 'inspectRequest' method to one of null, <code>HttpRequest</code>, <code>HttpAsyncRequestConsumer<ResponseProducer></code>
	 * @param originalRequest	The original request from the client
	 * @param result	The result from the JavaScript endpoint's 'inspectRequest' method.
	 * @return null, <code>HttpRequest</code>, <code>HttpAsyncRequestConsumer<ResponseProducer></code>
	 */
	@SuppressWarnings("unchecked")
	private Object transformScriptInspectionResult(HttpRequest originalRequest, Object result) {
		if (result == null)
			return null;
		if (result instanceof HttpRequest)
			return (HttpRequest) result;
		if (result instanceof HttpAsyncRequestConsumer<?>)
			return (HttpAsyncRequestConsumer<ResponseProducer>) result;
		if (result instanceof ScriptObjectMirror)
			return (ScriptObjectMirror) result;

		if (result instanceof URL) {
			try {
				result = ((URL) result).toURI();
			}
			catch (URISyntaxException ex) {
				Logger.error("The endpoint at " + originalRequest.getRequestLine().getUri() + " returned an invalid URL instance", ex);
				return null;
			}
		}
		else if (result instanceof String) {
			try {
				result = new URI((String) result);
			}
			catch (URISyntaxException ex) {
				Logger.error("The endpoint at " + originalRequest.getRequestLine().getUri() + " returned an invalid URL String", ex);
				return null;
			}
		}
		if (result instanceof URI) {
			String str = ((URI) result).toString();
			RequestLine origReqLine = originalRequest.getRequestLine();
			if (originalRequest instanceof HttpEntityEnclosingRequest) {
				BasicHttpEntityEnclosingRequest r = new BasicHttpEntityEnclosingRequest(origReqLine.getMethod(), str, origReqLine.getProtocolVersion());
				r.setEntity(((HttpEntityEnclosingRequest) originalRequest).getEntity());
				return r;
			}
			else
				return new BasicHttpRequest(origReqLine.getMethod(), str, origReqLine.getProtocolVersion());
		}
		else {
			Logger.error("The endpoint at " + originalRequest.getRequestLine().getUri() + " returned an illegal result [class=" + result.getClass().getName() + "]");
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * Our <code>processRequest</code> method always returns an instance of <code>HttpAsyncRequestConsumer<ResponseProducer></code> and the <code>ResponseProducer<code> part of that is passed as the first parameter to this method.
	 * This method simply arms the <code>HttpAsyncExchange</code> response trigger with our asynchronous <code>Producer</code>.
	 */
	@Override
	public void handle(ResponseProducer producer, HttpAsyncExchange responseTrigger, HttpContext context) throws HttpException, IOException {
		if (producer.setTrigger(responseTrigger)) {
			String id = (String)context.getAttribute("pokerface.txId");
			Logger.trace("[client<-proxy] " + id + " response triggered");
		}
	}
}
