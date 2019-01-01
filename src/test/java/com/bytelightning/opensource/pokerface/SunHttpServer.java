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

import com.sun.net.httpserver.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic wrapper around the sun http server that one can always count on being present in a jdk.
 *
 * @see <a href="http://docs.oracle.com/javase/6/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html">com/sun/net/httpserver/HttpServer.html</a>
 */
@SuppressWarnings({"restriction", "WeakerAccess"})
public class SunHttpServer {
	/**
	 * Create a com.sun.net.httpserver (HttpServer or HttpsServer) at the indicated local address.
	 *
	 * @param addr   The address the server should listen on.
	 * @param sslCtx If non-null, create an https server (@see CreateDefaultSSLContext). Otherwise a plain http server.
	 */
	public SunHttpServer(InetSocketAddress addr, SSLContext sslCtx) throws IOException {
		if (sslCtx != null) {
			HttpsServer retVal = HttpsServer.create(addr, 100);
			retVal.setHttpsConfigurator(new HttpsConfigurator(sslCtx) {
				public void configure(HttpsParameters params) {
					SSLParameters sslparams = getSSLContext().getDefaultSSLParameters();
					params.setSSLParameters(sslparams);
				}
			});
			httpServer = retVal;
		}
		else
			httpServer = HttpServer.create(addr, 100);
	}

	private HttpServer httpServer;

	/**
	 * Serve multiple requests at once
	 */
	private ExecutorService httpThreadPool = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "Remote Target Thread");
		t.setDaemon(true);
		return t;
	});

	/**
	 * Register a handler for all (root) requests, and start the server.
	 *
	 * @param rootHandler Will receive all incoming requests.
	 */
	public void start(HttpHandler rootHandler) {
		// Hang everything off the root context.
		httpServer.createContext("/", rootHandler);
		// Set the ability to handle multiple requests at once
		httpServer.setExecutor(httpThreadPool);
		// Make it so.
		httpServer.start();
	}

	/**
	 * Register handlers for a set of uri path's, and start the server.
	 *
	 * @param handlers A {@code Map} of uri path strings to HttpHandler's.
	 */
	@SuppressWarnings("unused")
	public void start(Map<String, HttpHandler> handlers) {
		for (Entry<String, HttpHandler> e : handlers.entrySet())
			httpServer.createContext(e.getKey(), e.getValue());
		// Set the ability to handle multiple requests at once
		httpServer.setExecutor(httpThreadPool);
		// Make it so.
		httpServer.start();
	}

	/**
	 * Shut it all down cleanly.
	 */
	public void stop() {
		if (httpServer != null) {
			httpServer.stop(2);
			httpThreadPool.shutdownNow();
			httpServer = null;
		}
	}
}
