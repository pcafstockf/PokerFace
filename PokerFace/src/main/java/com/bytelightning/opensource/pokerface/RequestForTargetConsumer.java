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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.protocol.HttpContext;

/**
 * Specialization of <code>AbsClientRequestConsumer</code> to consume a client request that will be forwarded to a remote Target.
 * This class examines the incoming request from the client to see if it is mappable to a remote Target.
 * If no viable mapping can be found for the request, this class instructs it's <code>ResponseProducer</code> to return a 404 NOT_FOUND resoponse the the client.
 */
@SuppressWarnings("restriction")
public class RequestForTargetConsumer extends AbsClientRequestConsumer {

	/**
	 * Primary constructor
	 * @param context	Context of this request / response transaction
	 * @param executor	<code>HttpAsyncRequester</code> which will perform the actual request to the remote Target and recieve it's response.
	 * @param connPool	The client connection pool that will be used by the <code>executor</code>
	 * @param patternTargetMapping	The mapping of relative uri paths to configured Targets.
	 * @param dynamicHostMap	If non-null, this is a request modified by a JavaScript endpoint, *and* we are configured to allow the endpoint's to proxy to remote Target's not specified in the configuration file.
	 * @param requestBuffer	Buffer used to read in the request content from a client (if any) which will then be flipped and sent out to the Target by the <code>TargetRequestProducer</code>
	 * @param responseBuffer	Buffer used to read in the response content from the remote Target (if any) which will then be flipped and sent back to the client by the <code>TargetResponseConsumer</code>
	 * @param endpoint	If non-null, the script endpoint which has interjected itself into this transaction.
	 */
	public RequestForTargetConsumer(HttpContext context, HttpAsyncRequester executor, BasicNIOConnPool connPool, Map<String, TargetDescriptor> patternTargetMapping, ConcurrentMap<String, HttpHost> dynamicHostMap,
					HttpRequest targetRequest, BufferIOController requestBuffer, BufferIOController responseBuffer, ScriptObjectMirror endpoint) {
		super(context, requestBuffer, new ResponseProducer("proxy", context, responseBuffer));
		this.executor = executor;
		this.connPool = connPool;
		this.patternTargetMapping = patternTargetMapping;
		this.dynamicHostMap = dynamicHostMap;
		this.targetRequest = targetRequest;
		this.endpoint = endpoint;
		this.responseBuffer = responseBuffer;
	}
	private final HttpAsyncRequester executor;
	private final BasicNIOConnPool connPool;
	private final ConcurrentMap<String, HttpHost> dynamicHostMap;
	private final Map<String, TargetDescriptor> patternTargetMapping;
	private final ScriptObjectMirror endpoint;
	private final BufferIOController responseBuffer;
	private HttpRequest targetRequest;

	/**
	 * {@inheritDoc}
	 * Find a remote target that matches the requested uri, *or* return a 404 NOT_FOUND response if a mapping is not present.
	 */
	@Override
	public void requestReceived(HttpRequest clientRequest) {
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.info("[client->proxy] " + id + " " + targetRequest.getRequestLine());
		// We have already been given the targetRequest in our constructor.  This method's 'request' parameter is therefore ignored.
		
		// Find the remote Target that we have configured to match this request URI.
		RequestLine reqLine = targetRequest.getRequestLine();
		TargetDescriptor targetDesc = lookupTargetFromUri(reqLine.getUri());
		if (targetDesc != null)	// Execute the async request / response transaction against the remote Target.
			executor.execute(new TargetRequestProducer(targetDesc, targetRequest, context, buffer), new TargetResponseConsumer(producer, responseBuffer, context, endpoint), connPool);
		else	// No Target was matched against this request.
			producer.setResponse(HttpStatus.SC_NOT_FOUND, null);
	}

	/**
	 * Looks up the TargetDescriptor matching the given request path.
	 * NOTE: If 
	 * @param uriStr the request path
	 * @return object or <code>null</code> if no match is found.
	 */
	private TargetDescriptor lookupTargetFromUri(final String uriStr) {
		if (patternTargetMapping == null)
			return null;	// No remote targets configured.
		URI uri;
		try {
			uri = new URI(uriStr);
		}
		catch (URISyntaxException e) {
			Logger.error("Impossibly, the HttpRequest contained an unparsable uri", e);
			return null;
		}
		String key = lookupKeyFromPath(uri.getPath().toLowerCase());
		TargetDescriptor retVal = patternTargetMapping.get(key);
		// If a Target was not found, 
		// 	AND we have been configured to support dynamicHost targeting, 
		// 	AND this request came from our own script, 
		// Then generate a dynamic Target (and remember it).
		if ((retVal == null) && (dynamicHostMap != null)) {
			String[] scheme = { null };
			String[] host = { null };
			int[] port = { 0 };
			String[] path = { null };
			int[] stripPrefixCount = { 0 };
			key = UriToTargetKey(uri, scheme, host, port, path, stripPrefixCount);
			HttpHost httpHost = dynamicHostMap.get(key);
			if (httpHost == null) {
				HttpHost newHost = new HttpHost(host[0], port[0], scheme[0]);
				httpHost = dynamicHostMap.putIfAbsent(key, newHost);
				if (httpHost == null)
					httpHost = newHost;
			}
			retVal = new TargetDescriptor(httpHost, path[0], stripPrefixCount[0]);
		}
		return retVal;
	}

	/**
	 * Finds the best matching key in our configured Target mappings.
	 */
	private String lookupKeyFromPath(String uriPath) {
		String retVal = uriPath;
		// direct match?
		if (patternTargetMapping.get(retVal) == null) {
			// pattern match?
			String bestMatch = null;
			for (Entry<String, TargetDescriptor> entry : patternTargetMapping.entrySet()) {
				final String pattern = entry.getKey();
				if (DoesUriRequestMatchPattern(pattern, uriPath)) {
					// we have a match. is it any better?
					if (IsBetterMatch(bestMatch, pattern))
						retVal = bestMatch = pattern;
				}
			}
		}
		return retVal;
	}

	/**
	 * Is <code>pattern</code> a closer match than <code>bestMatch</code>?
	 */
	private static boolean IsBetterMatch(String bestMatch, String pattern) {
		if (bestMatch == null || (bestMatch.length() < pattern.length()) || (bestMatch.length() == pattern.length() && pattern.endsWith("*")))
			return true;
		return false;
	}

	/**
	 * Well does it ?
	 */
	private static boolean DoesUriRequestMatchPattern(String pattern, String uriPath) {
		if (pattern.equals("*"))
			return true;
		else
			return (pattern.endsWith("*") && uriPath.startsWith(pattern.substring(0, pattern.length() - 1))) || (pattern.startsWith("*") && uriPath.endsWith(pattern.substring(1, pattern.length())));
	}

	/**
	 * @see #UriToTargetKey(URI, String[], String[], int[], String[], int[])
	 */
	public static String UriToTargetKey(String remoteTargetUri, String[] scheme, String[] host, int[] port, String[] path, int[] stripPrefixCount) {
		URI uri;
		try {
			uri = new URI(remoteTargetUri);
		}
		catch (URISyntaxException e) {
			Logger.error("Invalid URI provided for host key", e);
			return null;
		}
		return UriToTargetKey(uri, scheme, host, port, path, stripPrefixCount);
	}

	/**
	 * Break out a <code>URI</code> into it's component parts, and re-assemble them into a lowercase key representing the target.
	 * @param remoteTargetUri	The input
	 * @param scheme	"pass-by-reference" which if non-null will be populated with: https or http (if not explicitly https)
	 * @param host	"pass-by-reference" which if non-null will be populated with: The name of the Target server.
	 * @param port	"pass-by-reference" which if non-null will be populated with: The port the Target server is listening on (if not specified will be 80 for http and 443 for https).
	 * @param path	"pass-by-reference" which if non-null will be populated with: The Path portion of the provided uri.
	 * @param stripPrefixCount	"pass-by-reference" which if non-null will be populated with: This is actually an Integer.parse of the anchor portion of the uri which is used as a means to specify how much of the requesting uri should be stripped off before appending it to the host:port/path of the Target.
	 * @return	A key that may be used to uniquely identify this remote Target.
	 */
	public static String UriToTargetKey(URI remoteTargetUri, String[] scheme, String[] host, int[] port, String[] path, int[] stripPrefixCount) {
		if (remoteTargetUri == null)
			return null;
		String schemeStr = remoteTargetUri.getScheme() != null ? remoteTargetUri.getScheme() : "http";
		String hostStr = remoteTargetUri.getHost();
		int portNum = remoteTargetUri.getPort() > 0 ? remoteTargetUri.getPort() : schemeStr.equalsIgnoreCase("https") ? 443 : 80;
		String key = (schemeStr + "://" + hostStr + ":" + portNum).toLowerCase();
		if (scheme != null)
			scheme[0] = schemeStr;
		if (host != null)
			host[0] = hostStr;
		if (port != null)
			port[0] = portNum;
		if (path != null)
			path[0] = remoteTargetUri.getPath();
		if (stripPrefixCount != null) {
			String frag = remoteTargetUri.getFragment();
			if ((frag == null) || (frag.length() == 0))
				stripPrefixCount[0] = 0;
			else
				stripPrefixCount[0] = Integer.parseInt(frag.trim());
		}
		return key;
	}
}
