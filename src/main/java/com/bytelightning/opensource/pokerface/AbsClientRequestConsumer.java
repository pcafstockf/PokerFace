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

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Abstract base class to consume a request from the client / browser.
 * Every request from a client will cause an instance of this class to be created.
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbsClientRequestConsumer implements HttpAsyncRequestConsumer<ResponseProducer> {
	protected static final Logger Logger = LoggerFactory.getLogger(AbsClientRequestConsumer.class.getPackage().getName());

	/**
	 * Primary constructor
	 *
	 * @param context  The context of this http transaction.
	 * @param buffer   An asynchronous friendly buffer to hold any incoming request content
	 * @param producer The ResponseProducer that is responsible for handling our response back to the requesting client.
	 */
	protected AbsClientRequestConsumer(HttpContext context, BufferIOController buffer, ResponseProducer producer) {
		assert context != null;
		this.context = context;
		this.buffer = buffer;
		this.producer = producer;
	}

	protected final HttpContext context;
	protected final BufferIOController buffer;
	protected volatile ResponseProducer producer;
	private volatile boolean completed;
	private volatile Exception exception;

	/**
	 * {@inheritDoc}
	 * This method is only called if the request had content (e.g. POST).
	 * It reads the incoming request data and stores it in it's buffer to be read asynchronously by some other object (typically a RequestProducer).
	 */
	@SuppressWarnings("Duplicates")
	@Override
	public void consumeContent(ContentDecoder decoder, IOControl ioctrl) throws IOException {
		buffer.setWritingIOControl(ioctrl);
		int n;
		final ByteBuffer bb = buffer.getByteBuffer();
		// Make sure the buffer isn't mucked with while we are actually filling it.
		synchronized (bb) {
			n = decoder.read(bb); // Decode the data from the client / browser into the buffer
			buffer.dataWritten();
		}
		String id = (String) context.getAttribute("pokerface.txId");
		Logger.trace("[client->proxy] " + id + " " + n + " bytes read");
		if (decoder.isCompleted())
			Logger.trace("[client->proxy] " + id + " content fully read");
	}

	/**
	 * {@inheritDoc}
	 * Invoked once the request has been fully received by this server.
	 * This method also sets the writeCompleted flag on it's buffer (if it has one).
	 */
	@Override
	public void requestCompleted(HttpContext context) {
		completed = true;
		if (buffer != null)
			buffer.writeCompleted();
		String id = (String) context.getAttribute("pokerface.txId");
		Logger.debug("[client->proxy] " + id + " request completed");
	}

	/**
	 * Returns the {@code ResponseProducer} that will ultimately send the result of this request/response transaction back to the client.
	 */
	@Override
	public ResponseProducer getResult() {
		return producer;
	}

	@Override
	public boolean isDone() {
		return completed;
	}

	@Override
	public Exception getException() {
		return exception;
	}

	@Override
	public void failed(Exception exception) {
		Logger.warn("[client->proxy] ", exception);
		this.exception = exception;
	}

	/**
	 * {@inheritDoc}
	 * This specialization currently does nothing.
	 */
	@Override
	public void close() throws IOException {
	}
}
