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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Concrete implementation of org.apache.http.HttpEntity which obtains it's content from a JavaScript function.
 * The primary purpose of this class is to allow a script to start sending the headers of a response back to a client while it is busy building the actual content.
 */
@SuppressWarnings({"deprecation", "restriction"})
public class NJavascriptFunctionEntity extends AbstractHttpEntity implements HttpAsyncContentProducer, ProducingNHttpEntity {
	protected static final Logger Logger = LoggerFactory.getLogger(NJavascriptFunctionEntity.class.getPackage().getName());

	/**
	 * Primary constructor
	 */
	public NJavascriptFunctionEntity(String endpointUriStr, ScriptObjectMirror endPoint, ScriptObjectMirror fn, ContentType contentType, HttpResponse response, HttpContext context) {
        setContentType(contentType.toString());
        this.endpointUriStr = endpointUriStr;
        this.endPoint = endPoint;
        this.fn = fn;
        this.contentType = contentType;
        this.response = response;
        this.context = context;
	}
	private final String endpointUriStr;
	private final ScriptObjectMirror endPoint;
	private final ScriptObjectMirror fn;
	private final ContentType contentType;
	private final HttpResponse response;
	private final HttpContext context;
	private HttpEntity delegate;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRepeatable() {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		if (delegate instanceof Closeable)
			((Closeable)delegate).close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finish() throws IOException {
        close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isStreaming() {
		ensureDelegate();
		return delegate.isStreaming();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentLength() {
		ensureDelegate();
		return delegate.getContentLength();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		ensureDelegate();
		return delegate.getContent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		ensureDelegate();
		delegate.writeTo(outstream);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
		HttpAsyncContentProducer cp = getContentPoducer();
		cp.produceContent(encoder, ioctrl);
		if (encoder.isCompleted())
			cp.close();
	}

	/**
	 * Ensure that we can get a org.apache.http.HttpEntity from the JavaScript function (directly or indirectly).
	 */
	private void ensureDelegate() {
		if (delegate == null) {
			Object result = fn.call(endPoint, response, context);
			delegate = Utils.WrapObjWithHttpEntity(result, contentType);
			if (delegate == null) {
				Logger.error("The endpoint content callback function at " + endpointUriStr + " returned an unknown content type.");
				delegate = new NStringEntity("Invalid endpoint content response!", ContentType.DEFAULT_TEXT);
			}
		}
	}
	/**
	 * Wrap our delegate in an <code>HttpAsyncContentProducer</code>
	 */
	private HttpAsyncContentProducer getContentPoducer() throws IOException {
		ensureDelegate();
		if (delegate instanceof HttpAsyncContentProducer)
			return (HttpAsyncContentProducer)delegate;
		if (contentProducer == null)
			contentProducer = new EntityAsyncContentProducer(delegate);
		return contentProducer;
	}
	private HttpAsyncContentProducer contentProducer;
}
