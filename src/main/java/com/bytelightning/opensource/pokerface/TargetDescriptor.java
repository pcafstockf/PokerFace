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

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a meta-data class to keep track of a remote Target for this proxy
 */
@SuppressWarnings("WeakerAccess")
public final class TargetDescriptor {
	protected static final Logger Logger = LoggerFactory.getLogger(TargetDescriptor.class.getPackage().getName());

	/**
	 * Primary constructor.
	 *
	 * @param target             The remote Target server FQDN
	 * @param targetPath         The path to a resource 'root' on the {@code target} (which would typically be an empty string "", but is not required to be so).
	 * @param prefixDiscardCount How many prefix characters of the incoming request uri *path* should be removed before appending the remaining to the {@code target + targetPath}.
	 */
	public TargetDescriptor(HttpHost target, String targetPath, int prefixDiscardCount) {
		this.target = target;
		this.targetPath = targetPath;
		this.prefixDiscardCount = prefixDiscardCount;
	}

	private final HttpHost target;
	private final String targetPath;
	private final int prefixDiscardCount;

	/**
	 * Returns the remote Target server FQDN
	 */
	public HttpHost getTarget() {
		return target;
	}

	/**
	 * Returns the path to a resource 'root' on the {@code target} (which would typically be an empty string "", but is not required to be so).
	 */
	public String getTargetPath() {
		return targetPath;
	}

	/**
	 * Returns the number of prefix characters of the incoming request uri *path* that should be removed before appending the remaining characters to end of {@code target + targetPath}.
	 */
	public int getPrefixDiscardCount() {
		return prefixDiscardCount;
	}
}
