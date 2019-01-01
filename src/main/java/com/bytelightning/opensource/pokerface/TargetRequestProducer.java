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

import org.apache.http.*;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 * This class forwards the request we received from a client on to a remote Target.
 * In the streaming spirit, this class is created as soon as a remote Target is identified, and may actually make the HttpRequest
 * to the remote Target before we have fully received the content of the request from the client.
 * This class uses the {@code BufferIOController} to coordinate reading flow control from the {@code RequestForTargetConsumer}'s incoming request content buffer.
 * For this reason, this class has the responsibility for closing the buffer that was given to the {@code RequestForTargetConsumer} by the {@code RequestHandler}
 */
public class TargetRequestProducer extends TargetBase implements HttpAsyncRequestProducer {
	protected static final Logger Logger = LoggerFactory.getLogger(TargetRequestProducer.class.getPackage().getName());

	/**
	 * Primary constructor
	 *
	 * @param targetDesc    The remote Target
	 * @param clientRequest The request we received from the client.
	 * @param context       The context of this request / response transaction.
	 * @param buffer        The buffer containing the content (if any) of the original request from the client.
	 */
	public TargetRequestProducer(TargetDescriptor targetDesc, HttpRequest clientRequest, HttpContext context, BufferIOController buffer) {
		this.targetDesc = targetDesc;
		this.clientRequest = clientRequest;
		this.context = context;
		this.buffer = buffer;
	}

	private final TargetDescriptor targetDesc;
	private final HttpRequest clientRequest;
	private final HttpContext context;
	private final BufferIOController buffer;

	/**
	 * {@inheritDoc}
	 * Close the content buffer given to us by the {@code RequestForTargetConsumer}.
	 */
	@SuppressWarnings("RedundantThrows")
	@Override
	public void close() throws IOException {
		buffer.close();
	}

	@Override
	public HttpHost getTarget() {
		return targetDesc.getTarget();
	}

	@SuppressWarnings("Duplicates")
	@Override
	public HttpRequest generateRequest() {
		// Munge the uri as needed to match the remote Target root.
		RequestLine reqLine = clientRequest.getRequestLine();
		int discardCount = targetDesc.getPrefixDiscardCount();
		String pathSuffix;
		if (discardCount > 0) {
			String uri = reqLine.getUri();
			pathSuffix = uri.substring(discardCount);
		}
		else
			pathSuffix = reqLine.getUri();
		// Build the fully qualified remote Target URI.
		String newUri = targetDesc.getTarget().toURI() + targetDesc.getTargetPath() + pathSuffix;
		// Rewrite request!!!!
		HttpRequest retVal;
		if (clientRequest instanceof HttpEntityEnclosingRequest) {
			retVal = new BasicHttpEntityEnclosingRequest(reqLine.getMethod(), newUri, reqLine.getProtocolVersion());
			((BasicHttpEntityEnclosingRequest) retVal).setEntity(((HttpEntityEnclosingRequest) clientRequest).getEntity());
		}
		else
			retVal = new BasicHttpRequest(reqLine.getMethod(), newUri, clientRequest.getProtocolVersion());
		// Host header is going to be different (obviously)
		Set<String> removals = BuildHeaderRemovalList(clientRequest.getFirstHeader("Connection"), "Via", "Host", "Keep-Alive", "Content-Length");
		// Copy across all other headers
		for (Header hdr : clientRequest.getAllHeaders()) {
			if (removals.contains(hdr.getName().toLowerCase()))
				continue;
			retVal.addHeader(hdr);
		}
		retVal.setHeader(CreateHttpViaHeader(clientRequest.getFirstHeader("Via"), clientRequest.getProtocolVersion()));

		String id = (String) context.getAttribute("pokerface.txId");
		Logger.info("[proxy->target] " + id + " " + retVal.getRequestLine());
		return retVal;
	}

	/**
	 * {@inheritDoc}
	 * This method is only called if the request has content (e.g. POST).
	 * It flips it's buffer (which the {@code RequestForTargetConsumer} has asynchronously written into), reads the request data and encodes it out to the remote Target.
	 */
	@SuppressWarnings("Duplicates")
	@Override
	public void produceContent(final ContentEncoder encoder, final IOControl ioctrl) throws IOException {
		buffer.setReadingIOControl(ioctrl);
		int n;
		boolean eof;
		final ByteBuffer bb = buffer.getByteBuffer();
		// Make sure nobody mucks with the buffer while we are reading from it and encoding its content.
		synchronized (bb) {
			bb.flip();
			n = encoder.write(bb); // Encode the data going out to the remote target
			bb.compact();
			eof = buffer.dataRead();
		}
		String id = (String) context.getAttribute("pokerface.txId");
		Logger.trace("[proxy->target] " + id + " " + n + " bytes written");
		if (eof) {
			encoder.complete();
			Logger.trace("[proxy->target] " + id + " content fully written");
		}
	}

	@Override
	public void requestCompleted(final HttpContext context) {
		String id = (String) context.getAttribute("pokerface.txId");
		Logger.debug("[proxy->target] " + id + " request completed");
	}

	/**
	 * {@inheritDoc}
	 * This class is not repeatable as it is created solely for this transaction.
	 */
	@Override
	public boolean isRepeatable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * Does nothing as this class is not repeatable because it is created solely for this transaction.
	 */
	@Override
	public void resetRequest() {
	}

	@Override
	public void failed(final Exception ex) {
		//TODO: Shouldn't TargetRequestProducer.failed be notifying somebody that it failed it's task?
		Logger.warn("[proxy->target] ", ex);
	}
}
