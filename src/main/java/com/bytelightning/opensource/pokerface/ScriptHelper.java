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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.MimeEntry;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.DateFormat;

/**
 * Exposes a number of useful java based utilities to JavaScript endpoints.
 */
@SuppressWarnings({"restriction", "unused"})
public interface ScriptHelper {
	Logger ScriptLogger = LoggerFactory.getLogger(PokerFace.class.getPackage().getName() + ".scripts");

	/**
	 * Returns a {@code ByteBuffer} that will be automatically cleaned up once the transaction completes.
	 */
	ByteBuffer createBuffer();

	/**
	 * Returns http or https scheme of this transaction
	 */
	String getScheme();

	/**
	 * Convert (if necessary) and return the absolute URL that represents the resource referenced by this possibly relative URL.
	 * If this URL is already absolute, return it unchanged. This method would typically be used to compute a 302 redirect url.
	 *
	 * @param location URL to be (possibly) converted and then returned
	 * @throws IllegalArgumentException if a MalformedURLException is thrown when converting the relative URL to an absolute one
	 */
	String makeAbsoluteUrl(String location);

	/**
	 * Return the language Locales accepted by the requesting client in order of preference (best to least).
	 */
	String[] getAcceptableLocales();

	/**
	 * Locate a {@code sun.net.www.MimeEntry} by it's MIMEType
	 */
	MimeEntry findMimeEntryByType(String mimeType);

	/**
	 * Locate a {@code sun.net.www.MimeEntry} by the file extension that has been associated with it.
	 */
	MimeEntry findMimeEntryByExt(String ext);

	/**
	 * Return the underlying local {@code InetAddress} for this request / response transaction
	 */
	InetAddress getLocalAddress();

	/**
	 * Return the underlying local socket port for this request / response transaction
	 */
	int getLocalPort();

	/**
	 * Return the underlying remote {@code InetAddress} for this request / response transaction
	 */
	InetAddress getRemoteAddress();

	/**
	 * Return the underlying remote socket port for this request / response transaction
	 */
	int getRemotePort();

	/**
	 * Returns the host name as seen by the calling client.
	 * This method examines the HOST header (which is an HTTP required header).
	 * If not available for some reason, this method will do a name lookup on the local address of this http connection.
	 */
	String getHOSTName();

	/**
	 * Returns the host port as seen by the calling client.
	 * This method examines the HOST header (which is an HTTP required header).
	 * Per the standard, if the port is not specified in the header (or the header does not exist), the default 443 or 80 is
	 * returned.
	 */
	int getHOSTPort();

	/**
	 * Returns the charset of the Content-Type header
	 */
	String getCharacterEncoding();

	/**
	 * Get a new date formatter compatible with HTTP headers protocol.
	 * SPECIFICALLY... This means: The parser will parse GMT and return local time. The formatter will take a local time and output a GMT string.
	 */
	DateFormat getHTTPDateFormater();

	/**
	 * Format millisecond based date into a GMT string compatible with HTTP headers protocol.
	 *
	 * @param millisecondsSinceEpoch URL to be (possibly) converted and then returned
	 */
	String formatDate(long millisecondsSinceEpoch);
}
