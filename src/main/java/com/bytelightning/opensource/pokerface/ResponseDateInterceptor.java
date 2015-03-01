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
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * Similar to org.apache.http.ResponseDate, this class adds the Date header to the outgoing responses.
 * Additionally this intercepter may re-write the 'Expires' header.
 * Specifically, it examines the 'Expires' header, and if it is a number that begins with a '@' symbol, that number is interpreted as an offset (in seconds) to the 'Date' header.
 */
public class ResponseDateInterceptor implements HttpResponseInterceptor {

	/**
	 * Add a date header and possibly rewrite the 'Expires' header (see class documentation).
	 */
	public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
		final int status = response.getStatusLine().getStatusCode();
		if ((status >= HttpStatus.SC_OK) && !response.containsHeader(HTTP.DATE_HEADER)) {
			long now = System.currentTimeMillis();
			String nowStr = Utils.GetHTTPDateFormater().format(new Date(now));
			response.setHeader(HTTP.DATE_HEADER, nowStr);
			Header expires = response.getFirstHeader("Expires");
			if (expires != null) {
				String sval = expires.getValue();
				if (sval.startsWith("@")) {
					try {
						long lval = Long.parseLong(sval.substring(1)) * 1000;
						lval += now;
						sval = Utils.GetHTTPDateFormater().format(new Date(lval));
						response.setHeader("Expires", sval);
					}
					catch (NumberFormatException nfe) {
						// Do nothing
					}
				}
			}
		}
	}
}
