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
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Set;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reads in the response we receive back from the remote Target, and then signals the <code>ResponseProducer</code> that it can begin responding back to the requesting client.
 * Note that the <code>ResponseProducer</code> may actually start responding back to the client even before we have fully read the response content from the remote Target.
 */
@SuppressWarnings("restriction")
public class TargetResponseConsumer extends TargetBase implements HttpAsyncResponseConsumer<HttpResponse> {
	protected static final Logger Logger = LoggerFactory.getLogger(TargetResponseConsumer.class.getPackage().getName());

	/**
	 * Primary constructor
	 * @param producer	The object that will produce the final response back to the client.
	 * @param producersBuffer	The buffer which this object will write to, and which the producer will read from.
	 * @param context	The context of this transaction.
	 * @param endpoint	If non-null, this is a script endpoint that may wish to alter the response from the target before we send it back to the client.
	 */
	public TargetResponseConsumer(ResponseProducer producer, BufferIOController producersBuffer, HttpContext context, ScriptObjectMirror endpoint) {
		this.producer = producer;
		this.producersBuffer = producersBuffer;
		this.context = context;
		this.endpoint = endpoint;
	}
	private final ResponseProducer producer;
	private final BufferIOController producersBuffer;
	private final HttpContext context;
	private final ScriptObjectMirror endpoint;
	private volatile HttpResponse response;
	private volatile boolean completed;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void responseReceived(HttpResponse response) {
		String id = (String)context.getAttribute("pokerface.txId");	
		Logger.info("[proxy<-target] " + id + " " + response.getStatusLine());
		// Rewrite response!!!!
		HttpResponse r = new BasicHttpResponse(response.getStatusLine());
		r.setEntity(response.getEntity());

		Set<String> removals = BuildHeaderRemovalList(response.getFirstHeader("Connection"), "Via", "Content-Length", "Content-Type", "Transfer-Encoding", "Keep-Alive", "Date");
		for (Header hdr : response.getAllHeaders()) {
			if (removals.contains(hdr.getName().toLowerCase()))
				continue;
			r.addHeader(hdr);
		}
		r.setHeader(CreateHttpViaHeader(response.getFirstHeader("Via"), response.getProtocolVersion()));
		
		// If a script endpoint was specified, give it a chance to alter the response.
		if ((endpoint != null) && (endpoint.hasMember("inspectResponse"))) {
			Object result = endpoint.callMember("inspectResponse", r, context);
			if (result instanceof HttpResponse)
				r = (HttpResponse)result;
		}
		
		if (producer.setResponse(r))
			Logger.debug("[proxy<-target] " + id + " response received");
	}

	/**
	 * {@inheritDoc}
	 * This method is only called if the response had content (which it typically does).
	 * It reads the incoming response data and stores it in it's buffer to be read asynchronously by the <code>ResponseProducer</code> that is part of this request -> request -> response -> response transaction.
	 */
	@Override
	public void consumeContent(ContentDecoder decoder, IOControl ioctrl) throws IOException {
		producersBuffer.setWritingIOControl(ioctrl);
		int n;
		final ByteBuffer bb = producersBuffer.getByteBuffer();
		// Make sure the buffer isn't mucked with while we are actually filling it.
		synchronized (bb) {
			n = decoder.read(bb); // decode the target's response into the ResponseProducers content buffer.
			producersBuffer.dataWritten();
		}
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.trace("[proxy<-target] " + id + " " + n + " bytes read");
		if (decoder.isCompleted())
			Logger.trace("[proxy<-target] " + id + " content fully read");
	}

	/**
	 * {@inheritDoc}
	 * This method also lets the <code>ResponseProducer</code> know that we have received all the content from the remote Target.
	 */
	@Override
	public void responseCompleted(HttpContext context) {
		if (completed)
			return;
		completed = true;
		producersBuffer.writeCompleted();
		if ((endpoint != null) && endpoint.hasMember("responseCompleted"))
			endpoint.callMember("responseCompleted", context, null);
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.debug("[proxy<-target] " + id + " response completed");
	}

	/**
	 * {@inheritDoc}
	 * This method also notifies the <code>ResponseProducer</code> about this exception.
	 */
	@Override
	public void failed(Exception ex) {
		if (completed)
			return;
		completed = true;
		producersBuffer.writeCompleted();
		if ((endpoint != null) && endpoint.hasMember("responseCompleted"))
			endpoint.callMember("responseCompleted", context, ex);
		producer.setException(ex);
	}

	/**
	 * {@inheritDoc}
	 * This method also notifies the <code>ResponseProducer</code> about the cancellation by invoking our <code>failed</code> method with an <code>InterruptedIOException</code>.
	 */
	@Override
	public boolean cancel() {
		if (completed)
			return false;
		failed(new InterruptedIOException("Cancelled"));
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse getResult() {
		return response;
	}

	/**
	 * {@inheritDoc}
	 * This method returns null as any failure will have already been propagated to the <code>ResponseProducer</code>
	 */
	@Override
	public Exception getException() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDone() {
		return completed;
	}
}
