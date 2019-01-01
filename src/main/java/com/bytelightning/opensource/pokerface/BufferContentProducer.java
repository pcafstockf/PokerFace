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

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Produces asynchronous content from an IOControlled byte buffer.
 * This class is primarily used to produce the content from a remote target back to a requesting client.
 */
@SuppressWarnings("WeakerAccess")
public class BufferContentProducer implements HttpAsyncContentProducer {
	protected static final Logger Logger = LoggerFactory.getLogger(BufferContentProducer.class.getPackage().getName());

	/**
	 * Primary constructor
	 *
	 * @param buffer The buffer that content will be produced from
	 * @param txId   The transaction id that this object is participating in
	 * @param role   The roll of this producer (e.g. server, proxy, etc.).
	 */
	public BufferContentProducer(BufferIOController buffer, String txId, String role) {
		this.buffer = buffer;
		this.txId = txId;
		this.role = role;
	}

	private final BufferIOController buffer;
	private final String txId;
	private final String role;

	@Override
	public boolean isRepeatable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * This specialization closes the content buffer.
	 */
	@SuppressWarnings("RedundantThrows")
	@Override
	public void close() throws IOException {
		buffer.close();
	}

	/**
	 * {@inheritDoc}
	 * This method is only called if the response has content (which is typically the case).
	 * It flips it's buffer (which some other object  has asynchronously written into), reads the response data and encodes it out to the client who requested it.
	 */
	@SuppressWarnings("Duplicates")
	@Override
	public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
		buffer.setReadingIOControl(ioctrl);
		// Send data to the client
		int n;
		boolean eof;
		final ByteBuffer bb = buffer.getByteBuffer();
		// Make sure nobody mucks with the buffer while we are reading from it and encoding its content.
		synchronized (bb) {
			bb.flip();
			n = encoder.write(bb);
			bb.compact();
			eof = buffer.dataRead();
		}
		Logger.trace("[client<-" + role + "] " + txId + " " + n + " bytes written");
		if (eof)
			encoder.complete();
	}
}
