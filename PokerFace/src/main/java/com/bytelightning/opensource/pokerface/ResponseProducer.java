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
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produce a response (back to the client) in response to the HttpRequest from the client.
 * It is possible that we can begin producing a response to the client *before* <code>RequestHandler.handle</code> is invoked by the framework.
 * To accommodate that, this class has two methods <code>setResponse</code> and <code>setTrigger</code>.  Please see those methods for details.
 */
public class ResponseProducer implements HttpAsyncResponseProducer {
	protected static final Logger Logger = LoggerFactory.getLogger(ResponseProducer.class.getPackage().getName());

	/**
	 * Primary constructor.
	 * @param context	The context of this transaction
	 * @param role	The role played by this producer (e.g. server, proxy, endpoint).
	 * @param contentProducer	The object that will asynchronously produce the content for this response.
	 */
	public ResponseProducer(String role, HttpContext context, HttpAsyncContentProducer contentProducer) {
		this.context = context;
		this.role = role;
		this.contentProducer = contentProducer;
		this.response = null;
		this.trigger = null;
	}
	protected final HttpContext context;
	protected final String role;
	protected volatile HttpAsyncContentProducer contentProducer;
	protected volatile HttpResponse response;
	protected volatile HttpAsyncExchange trigger;

	/**
	 * Alternate constructor (content to be produced from the supplied IOControlled buffer.
	 * @param context	The context of this transaction
	 * @param role	The role played by this producer (e.g. server, proxy, endpoint).
	 * @param buffer	A buffer from which response content may be asynchronously read and sent back to the client.
	 */
	public ResponseProducer(String role, HttpContext context, BufferIOController buffer) {
		this(role, context, new BufferContentProducer(buffer, (String)context.getAttribute("pokerface.txId"), role));
	}
	
	/**
	 * Alternate constructor (content producer to be extracted later from the HttpResponse entity).
	 * @param context	The context of this transaction
	 * @param role	The role played by this producer (e.g. server, proxy, endpoint).
	 */
	public ResponseProducer(String role, HttpContext context) {
		this(role, context, (HttpAsyncContentProducer)null);
	}

	/**
	 * {@inheritDoc}
	 * Close the content buffer.
	 */
	@Override
	public void close() throws IOException {
		if (contentProducer != null)
			contentProducer.close();
	}

	/**
	 * {@inheritDoc}
	 * Someone else always generates the response and invokes one of our <code>setResponse</code> or <code>setException</code> methods.
	 * This method returns whatever that response was.
	 */
	@Override
	public HttpResponse generateResponse() {
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.info("[client<-" + role + "] " + id + " " + response.getStatusLine());
		return response;
	}

	/**
	 * {@inheritDoc}
	 * This method is only called if the response has content (which is typically the case).
	 * It flips it's buffer (which some other object  has asynchronously written into), reads the response data and encodes it out to the client who requested it.
	 */
	@Override
	public void produceContent(final ContentEncoder encoder, final IOControl ioctrl) throws IOException {
		contentProducer.produceContent(encoder, ioctrl);
		if (encoder.isCompleted()) {
			contentProducer.close();
			contentProducer = null;
			String id = (String)context.getAttribute("pokerface.txId");
			Logger.trace("[client<-" + role + "] " + id + " content fully written");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void responseCompleted(HttpContext context) {
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.debug("[client<-" + role + "] " + id + " response completed");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void failed(Exception ex) {
		setException(ex);
	}

	/**
	 * Send back an appropriate response to the client.
	 */
	public void setException(Exception ex) {
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.warn("[client<-" + role + "] " + id, ex);
		setResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		
	}
	
	/**
	 * Send back the specified response to the client
	 */
	public void setResponse(int statusCode, String message) {
		if (message == null)
			message = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.US);
		BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_0, statusCode, message);
		response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
		response.setEntity(new NStringEntity(message, ContentType.DEFAULT_TEXT));
		if (setResponse(response)) {
			String id = (String)context.getAttribute("pokerface.txId");
			Logger.trace("[client<-" + role + "] " + id + " response triggered [" + message + "]");
		}	
	}

	/**
	 * Determines the HttpResponse that will be sent back to the requesting client.
	 * If the <code>RequestHandler.handle</code> method has already armed us with an <code>HttpAsyncExchange</code> response trigger, then invoke it's <code>submitResponse</code> method.
	 * Otherwise keep track of the response until our <code>setTrigger</code> method is invoked (at which time the trigger's <code>submitResponse</code> method will be called and this response will be sent back to the client).
	 */
	public synchronized boolean setResponse(HttpResponse response) {
		assert response != null;
		if (this.response == null) {
			if (contentProducer == null) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					if (entity instanceof HttpAsyncContentProducer)
						contentProducer = (HttpAsyncContentProducer) entity;
					else
						contentProducer = new EntityAsyncContentProducer(entity);
				}
			}
			this.response = response;
			if (trigger != null)
				trigger.submitResponse(this);
			return true;
		}
		return false;
	}

	/**
	 * Called from the  <code>RequestHandler.handle</code> method this method keeps track of the supplied trigger.
	 * If our <code>setResponse</code> method has already been called to specify our response, then we invoke the trigger's <code>submitResponse</code> method to send our response back to the client
	 * Otherwise keep track of the trigger until our <code>setResponse</code> method is invoked (at which time it will invoke the trigger's <code>submitResponse</code> method to send our response back to the client).
	 */
	public synchronized boolean setTrigger(HttpAsyncExchange trigger) {
		assert trigger != null;
		this.trigger = trigger;
		if (response != null) {
			this.trigger.submitResponse(this);
			return true;
		}
		return false;
	}
}
