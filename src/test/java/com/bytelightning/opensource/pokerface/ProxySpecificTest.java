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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Setup a remote target (SunHttpServer), configure PokerFace to proxy to it, and use an HttpUrlConnection to validate that PokerFace injects the proper headers
 */
@SuppressWarnings({"restriction", "WeakerAccess"})
public class ProxySpecificTest {

	/**
	 * Configure both servers
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		RemoteTarget = new SunHttpServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8088), null);
		RemoteTarget.start(ProxySpecificTest::OnRemoteTargetRequest);
		proxy = new PokerFace();
		XMLConfiguration conf = new XMLConfiguration();
		conf.load(ProxySpecificTest.class.getResource("/ProxySpecificTestConfig.xml"));
		proxy.config(conf);
		boolean started = proxy.start();
		Assert.assertTrue("Successful proxy start", started);

	}

	private static SunHttpServer RemoteTarget;
	private static PokerFace proxy;

	/**
	 * Context handler for the SunHttpServer
	 */
	protected static void OnRemoteTargetRequest(HttpExchange exchange) {
		Headers rspHdrs = exchange.getResponseHeaders();
		rspHdrs.set("Date", Utils.GetHTTPDateFormater().format(new Date()));
		rspHdrs.set("Content-Type", "text/html");
		try {
			exchange.sendResponseHeaders(200, 0);
			OutputStream out = exchange.getResponseBody();
			out.write("<htm><head><title>TEST</title></head><body>TEST</body></html>".getBytes(StandardCharsets.UTF_8));
			out.close();
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@SuppressWarnings("RedundantThrows")
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Create a client request to PokerFace and verify that it proxied to the SunHttpServer correctly.
	 */
	@Test
	public void testViaHeader() throws IOException {
		URL obj = new URL("http://localhost:8080/index.html");
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		// optional default is GET
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		Assert.assertEquals("Valid reponse code", 200, responseCode);
		String hdrField = con.getHeaderField("Via");
		Assert.assertNotNull("Received a 'via' header", hdrField);
		Assert.assertEquals("Received a valid 'via' header", "HTTP/1.1 PokerFace/" + Utils.Version, hdrField);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		//noinspection StatementWithEmptyBody
		while (in.readLine() != null)
			;
		in.close();
	}

	@SuppressWarnings("RedundantThrows")
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Shut down the servers.
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (proxy != null)
			proxy.stop();
		if (RemoteTarget != null)
			RemoteTarget.stop();
	}
}
