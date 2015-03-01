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
import java.nio.ByteBuffer;
import org.apache.commons.pool2.ObjectPool;
import org.apache.http.nio.IOControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides flow control of a shared read/write <code>ByteBuffer</code> which it lazily obtains from a <code>ObjectPool</code>.
 * One will be allocated for each request received from a client and another will be allocated to produce the response back to the client.
 * The flow control methods enable an <code>AbsClientRequestConsumer</code> to read in client request data, 
 * which hands off to a <code>TargetRequestProducer</code> who flips the buffer and writes it's content out to the target.
 */
public class BufferIOController {
	protected static final Logger Logger = LoggerFactory.getLogger(BufferIOController.class.getPackage().getName());

	/**
	 * Primary constructor
	 * @param bufferPool	A reusable pool of <code>ByteBuffer</code>s that this object can lazily borrow from (and return to).
	 */
	public BufferIOController(ObjectPool<ByteBuffer> bufferPool) {
		this.bufferPool = bufferPool;
	}

	/**
	 * This method MUST be called to clean up any borrowed <code>ByteBuffer</code>s.
	 */
	public void close() {
		if (borrowedBuffer) {
			assert writeCompleted;
			try {
				buffer.clear();
				bufferPool.returnObject(buffer);
				buffer = null;
				borrowedBuffer = false;
			}
			catch (Exception e) {
				Logger.error("Unable to return ByteBuffer to pool", e);
			}
		}
	}
	
	/**
	 * Returns the pool from which this IOEontroller allocates </code>ByteBuffer</code>s.
	 * Please use this method responsibly!
	 */
	protected ObjectPool<ByteBuffer> getPool() {
		return bufferPool;
	}
	private final ObjectPool<ByteBuffer> bufferPool;

	/**
	 * Returns the <code>ByteBuffer</code>, lazily obtaining one from the pool if need be.
	 */
	public ByteBuffer getByteBuffer() {
		if (buffer == null) {
			try {
				buffer = bufferPool.borrowObject();
				buffer.reset(); // Yes it's redundant with our return code, but it *is* a community pool :-)
				borrowedBuffer = true;
			}
			catch (Exception e) {
				buffer = ByteBuffer.allocateDirect(1024 * 1024);
				borrowedBuffer = false;
			}
		}
		return buffer;
	}
	private volatile ByteBuffer buffer;
	private volatile boolean borrowedBuffer;

	/**
	 * This flow control method notifies us of the <code>IOControl</code> that will be writing to our <code>ByteBuffer</code>.
	 */
	public void setWritingIOControl(IOControl writingControl) {
		this.writingControl = writingControl;
	}
	private volatile IOControl writingControl;
	
	/**
	 * This flow control method notifies us that data was written to our buffer (by the 'writing' <code>IOControl</code>).
	 * This gives us the opportunity to suspend the 'writing' <code>IOControl</code> if our buffer is full and to notify the 'reading' <code>IOControl</code> that data is available.
	 */
	public void dataWritten() {
		// If the buffer is full, suspend client input until there is free space in the buffer
		if (!buffer.hasRemaining())
			writingControl.suspendInput();
		// If there is some content in our buffer make sure anyone consuming it is notified that more is available.
		if (readingControl != null)
			if (buffer.position() > 0)
				readingControl.requestOutput();
	}
	/**
	 * This flow control method notifies us that the 'writing' <code>IOControl</code> has finished writing data to our buffer.
	 * This also gives us the opportunity to notify the 'reading' <code>IOControl</code> that data is available.
	 */
	public void writeCompleted() {
		if (readingControl != null)
			readingControl.requestOutput();
		writeCompleted = true;
	}
	private volatile boolean writeCompleted;

	/**
	 * This flow control method notifies us of the <code>IOControl</code> that will be reading from our <code>ByteBuffer</code>.
	 */
	public void setReadingIOControl(IOControl readingControl) {
		this.readingControl = readingControl;
	}
	private volatile IOControl readingControl;
	
	/**
	 * This flow control method notifies us that data was read from our buffer (by the 'reading' <code>IOControl</code>).
	 * This gives us the opportunity to suspend the 'reading' <code>IOControl</code> if our buffer has been fully drained,
	 * and to notify the 'writing' <code>IOControl</code> that it may write more data into the buffer.
	 * 
	 * @return	True if the 'writing' <code>IOControl</code> has notified us that it has completed writing, AND there is no more data in the buffer to be read.
	 */
	public boolean dataRead() {
		// If there is space in the buffer and the message has not been transferred, make sure the target is sending more data
		if (buffer.hasRemaining() && !writeCompleted) {
			if (writingControl != null)
				writingControl.requestInput();
		}
		if (buffer.position() == 0) {
			if (writeCompleted)
				return true;
			// Input buffer is empty. Wait until the client fills up the buffer
			readingControl.suspendOutput();
		}
		return false;
	}
}
