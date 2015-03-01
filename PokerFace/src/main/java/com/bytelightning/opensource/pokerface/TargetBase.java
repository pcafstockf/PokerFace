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
import java.util.HashSet;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHeader;

/**
 * Simple base class to assist with common tasks for a proxy (such as header manipulation)
 */
public abstract class TargetBase {
	protected static final Header Via = new BasicHeader("Via", "PokerFace/" + Utils.Version);

	/**
	 * Build a *lowercase* list of headers that should be removed.
	 * Primarily the list comes from the 'Connection' header (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.10)
	 * @param connectionHeader	The value of the 'Connection' header (may be null).
	 * @param names	An additional list of header names that should be added to the returned list.
	 */
	protected static Set<String> BuildHeaderRemovalList(Header connectionHeader, String ... names) {
		HashSet<String> retVal = new HashSet<String>();
		// Add any headers specified by the Connection header 
		if (connectionHeader != null) {
			retVal.add("connection");
			String[] hdrs = connectionHeader.getValue().split("[ \t,]");
			for (String h : hdrs) {
				String t = h.trim();
				if (t.length() > 0)
					retVal.add(t.toLowerCase());
			}
		}
		// Add any caller supplied names to the list.
		if (names != null)
			for (String name : names)
				retVal.add(name.toLowerCase().trim());
		return retVal;
	}
	
	/**
	 * Create an appropriate http 'Via' header as specified in (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.45)
	 * @param curViaHeader	The existing 'Via' header (may be null).
	 * @param protocolVersion	The version of this specific http transaction.
	 */
	protected static Header CreateHttpViaHeader(Header curViaHeader, ProtocolVersion protocolVersion) {
		if (curViaHeader == null)
			return new BasicHeader(Via.getName(), protocolVersion.toString() + " " + Via.getValue());
		else
			return new BasicHeader("Via", curViaHeader.getValue() + ", " + protocolVersion.toString() + " " + Via.getValue());
	}
}
