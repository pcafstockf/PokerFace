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

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Specialization of {@code AbsClientRequestConsumer} to consume a client request that will be processed by a JavaScript endpoint.
 */
public class RequestForScriptConsumer extends AbsClientRequestConsumer {

	/**
	 * Primary constructor simply enforces that the producer passed to our superclass is a {@code ScriptResponseProducer}
	 */
	RequestForScriptConsumer(HttpContext context, BufferIOController buffer, ScriptResponseProducer producer) {
		super(context, buffer, producer);
	}

	/**
	 * {@inheritDoc}
	 * Since the {@code RequestHandler} already asked the endpoint to evaluate the request, *and* since the scripts don't currently perform async loading of request content, this method does nothing.
	 */
	@SuppressWarnings("RedundantThrows")
	@Override
	public void requestReceived(HttpRequest request) throws HttpException, IOException {
		String id = (String) context.getAttribute("pokerface.txId");
		Logger.info("[client->endpoint] " + id + " " + request.getRequestLine());
	}

	/**
	 * {@inheritDoc}
	 * The producer does not need our buffer, so this method closes it.
	 */
	@Override
	public void close() throws IOException {
		super.close();
		buffer.close();
	}
}
