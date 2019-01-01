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

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This {@code ResponseProducer} specialization is responsible for invoking the JavaScript endpoint to produce a response,
 * translate that response (if need be) into an asynchronous form, and then send that data back to the client.
 */
@SuppressWarnings("restriction")
public class ScriptResponseProducer extends ResponseProducer {
	protected static final Logger Logger = LoggerFactory.getLogger(ScriptResponseProducer.class.getPackage().getName());

	/**
	 * Primary constructor.
	 *
	 * @param endpoint The actual JavaScript endpoint to invoke.
	 * @param request  The request that was returned earlier by the endpoint's {@code inspectRequest} method.
	 * @param context  The context of this request / response transaction.
	 * @param buffer   The buffer from which the response's content data will be asynchronously read and sent back to the client.
	 */
	public ScriptResponseProducer(ScriptObjectMirror endpoint, HttpRequest request, HttpContext context, BufferIOController buffer) {
		super("endpoint", context);
		this.endpoint = endpoint;
		this.request = request;
	}

	private final ScriptObjectMirror endpoint;
	private final HttpRequest request;
	private volatile ScriptObjectMirror completionCallback;

	/**
	 * {@inheritDoc}
	 * This method actually does all the work of this class by invoking the endpoint, and processing it's result.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public HttpResponse generateResponse() {
		Object result;
		ScriptObjectMirror som;
		// First call the endpoint to obtain a result which will be either an HttpResponse (in which case our work is done), or a ScriptObjectMirror with properties to create an HttpResponse ourselves.
		try {
			result = endpoint.callMember("generateResponse", request, context);
			if (result instanceof HttpResponse) {
				this.setResponse((HttpResponse) result);
				return response;
			}
			som = (ScriptObjectMirror) result;
		} catch (Exception ex) {
			setException(ex);
			return response;
		}
		Object obj;
		// Interpret the http statusCode
		int statusCode;
		obj = som.getMember("statusCode");
		if ((obj == null) || ScriptObjectMirror.isUndefined(obj))
			statusCode = HttpStatus.SC_OK;
		else if (obj instanceof Number)
			statusCode = ((Number) obj).intValue();
		else if (obj instanceof ScriptObjectMirror)
			statusCode = (int) (((ScriptObjectMirror) obj).toNumber() + Double.MIN_VALUE);
		else
			statusCode = Integer.parseInt(obj.toString());
		// Interpret the http reasonPhrase
		String reasonPhrase;
		obj = som.getMember("reasonPhrase");
		if ((obj == null) || ScriptObjectMirror.isUndefined(obj))
			reasonPhrase = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.US);
		else
			reasonPhrase = obj.toString();
		// Create a basic response
		BasicHttpResponse response = new BasicHttpResponse(request.getProtocolVersion(), statusCode, reasonPhrase);
		// Interpret the headers supplied by the endpoint.
		obj = som.getMember("headers");
		if ((obj != null) && (!ScriptObjectMirror.isUndefined(obj))) {
			List<ScriptObjectMirror> headers;
			if (obj instanceof List<?>)
				headers = (List<ScriptObjectMirror>) obj;
			else {
				headers = new ArrayList<>();
				if ((obj instanceof ScriptObjectMirror) && ((ScriptObjectMirror) obj).isArray()) {
					for (Object sobj : ((ScriptObjectMirror) obj).values())
						headers.add((ScriptObjectMirror) sobj);
				}
				else
					Logger.error("The endpoint at " + request.getRequestLine().getUri() + " returned an illegal headers list [class=" + obj.getClass().getName() + "]");
			}
			for (ScriptObjectMirror hdr : headers) {
				for (String key : hdr.keySet()) {
					Object value = hdr.getMember(key);
					response.addHeader(key, ConvertHeaderToString(value));
				}
			}
		}
		// Interpret the content type of the response data.
		ContentType ct = ContentType.DEFAULT_TEXT;
		if (response.getFirstHeader("Content-Type") != null)
			ct = ContentType.parse(response.getFirstHeader("content-type").getValue());
		obj = som.getMember("mimeType");
		if ((obj != null) && (!ScriptObjectMirror.isUndefined(obj)))
			ct = ContentType.create(obj.toString(), ct.getCharset());
		obj = som.getMember("charset");
		if ((obj != null) && (!ScriptObjectMirror.isUndefined(obj)))
			ct = ContentType.create(ct.getMimeType(), obj.toString());
		obj = som.getMember("completion");
		if ((obj != null) && (!ScriptObjectMirror.isUndefined(obj)))
			if (((ScriptObjectMirror) obj).isFunction())
				completionCallback = (ScriptObjectMirror) obj;

		// Create an HttpEntity to represent the content.
		obj = som.getMember("content");
		if ((obj != null) && (!ScriptObjectMirror.isUndefined(obj))) {
			HttpEntity entity;
			if ((obj instanceof ScriptObjectMirror) && ((ScriptObjectMirror) obj).isFunction())
				entity = new NJavascriptFunctionEntity(request.getRequestLine().getUri(), endpoint, (ScriptObjectMirror) obj, ct, response, context);
			else {
				entity = Utils.WrapObjWithHttpEntity(obj, ct);
				if (entity == null)
					Logger.error("The endpoint at " + request.getRequestLine().getUri() + " returned an unknown content type [class=" + obj.getClass().getName() + "]");
			}
			if (entity != null)
				response.setEntity(entity);
		}
		else    // Normally setting the HttpEntity into the response would set the content-type, but we will have to specify it ourselves in this case.
			response.setHeader("Content-Type", ct.toString());

		// Let our superclass know what the endpoint has provided.
		this.setResponse(response);

		String id = (String) context.getAttribute("pokerface.txId");
		Logger.info("[client<-endpoint] " + id + " " + response.getStatusLine());
		return response;
	}

	/**
	 * Helper to convert an object returned as an Http Header value from a JavaScript endpoint and convert it into a usable String value for an {@code HttpResponse}
	 * Specifically Date objects (java or ecma) are converted to a GMT string, and all other objects are converted by invoking their {@code toString} method.
	 */
	private static String ConvertHeaderToString(Object value) {
		if (value instanceof Number)
			return value.toString();
		else if (value instanceof Date)
			return Utils.GetHTTPDateFormater().format((Date) value);
		else if (value instanceof ScriptObjectMirror) {
			ScriptObjectMirror som = (ScriptObjectMirror) value;
			String ecmaClass = som.getClassName();
			if ("Date".equals(ecmaClass)) {
				Number num = (Number) som.callMember("getTime");
				Date d = new Date(num.longValue());
				return Utils.GetHTTPDateFormater().format(d);
			}
			return null;
		}
		else
			return value.toString();
	}

	/**
	 * {@inheritDoc}
	 * Invoke the endpoint's responseCompleted function, and close out our {@code ScriptHelper}.
	 */
	@Override
	public void responseCompleted(HttpContext context) {
		try {
			if (completionCallback != null)
				completionCallback.call(endpoint, response, this.context, null);
			Closeable scriptHelper = (Closeable) context.getAttribute("pokerface.scriptHelper");
			if (scriptHelper != null)
				scriptHelper.close();
		} catch (Exception ex) {
			Logger.warn("Endpoint exception: responseCompleted [" + request.getRequestLine().getUri() + "]", ex);
		}
		super.responseCompleted(context);
	}

	/**
	 * {@inheritDoc}
	 * Because the JavaScript endpoint provides it's data synchronously, our {@code setTrigger} method will not yet have been called
	 * *and* we need to make sure that any response (error or otherwise) has it's {@code HttpEntity} (if any) wrapped into an {@code EntityAsyncContentProducer}
	 * so that we can respond to the client in an asynchronous manner.
	 */
	public synchronized boolean setResponse(HttpResponse response) {
		assert response != null;
		if (this.response == null) {
			assert contentProducer == null;
			HttpEntity entity = response.getEntity();
			if (entity != null)
				contentProducer = new EntityAsyncContentProducer(entity);
			this.response = response;
			return true;
		}
		return false;
	}

	/**
	 * Triggers the {@code generateResponse} method and begins responding to the client.
	 */
	@Override
	public synchronized boolean setTrigger(HttpAsyncExchange trigger) {
		assert trigger != null;
		assert this.trigger == null;
		this.trigger = trigger;
		this.trigger.submitResponse(this);
		return true;
	}
}
