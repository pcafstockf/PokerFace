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
import java.io.File;
import java.io.IOException;

import org.apache.http.Consts;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.protocol.HttpContext;

import sun.net.www.MimeEntry;

/**
 * Specialization of <code>AbsClientRequestConsumer</code> to consume a client request for a local static file and which generates the <code>ResponseProducer</code>.
 */
@SuppressWarnings("restriction")
public class RequestForFileConsumer extends AbsClientRequestConsumer {
	/**
	 * Primary constructor
	 * @param context	The context of this http transaction
	 * @param file	The file which will be sent (asynchronously) back to the client
	 */
	RequestForFileConsumer(HttpContext context, File file) {
		super(context, null, new ResponseProducer("server", context));
		this.file = file;
	}
	private final File file;
	private final static String OkReasonPhrase = (new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null)).getStatusLine().getReasonPhrase();

	/**
	 * Once we have fully received the request from the client, generate a response and tell the <code>ResponseProducer</code> about it.
	 */
	public void requestReceived(HttpRequest request) throws HttpException, IOException {
		String id = (String)context.getAttribute("pokerface.txId");
		Logger.info("[client->server] " + id + " " + request.getRequestLine());
		
		String uriStr = (String)context.getAttribute("pokerface.uripath");
		ContentType ct = ContentType.DEFAULT_BINARY;
		int dotPos = uriStr.lastIndexOf('.');
		if (dotPos > 0) {
			String ext = uriStr.substring(dotPos);
			MimeEntry entry = ScriptHelperImpl.MimeExtensionsMap.get(ext);
			if (entry != null)
				ct = ContentType.create(entry.getType(), Consts.UTF_8);
		}
		
		BasicHttpResponse response = new BasicHttpResponse(request.getProtocolVersion(), HttpStatus.SC_OK, OkReasonPhrase);
		response.setEntity(new NFileEntity(file, ct));
		if (producer.setResponse(response))
			Logger.trace("[client<-server] " + id + " response triggered");
	}
}