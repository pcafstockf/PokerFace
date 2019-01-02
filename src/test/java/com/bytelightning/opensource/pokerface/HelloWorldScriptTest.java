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

import org.apache.commons.configuration.XMLConfiguration;
import org.junit.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.SecureRandom;

public class HelloWorldScriptTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		PrevSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
		PrevHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

		proxy = new PokerFace();
		XMLConfiguration conf = new XMLConfiguration();
		conf.load(ProxySpecificTest.class.getResource("/HelloWorldTestConfig.xml"));
		proxy.config(conf);
		boolean started = proxy.start();
		Assert.assertTrue("Successful proxy start", started);

		SSLContext sc = SSLContext.getInstance("TLS");
		TrustManager[] trustAllCertificates = {new X509TrustAllManager()};
		sc.init(null, trustAllCertificates, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> {
			return true; // Just allow them all.
		});

	}

	private static PokerFace proxy;
	private static SSLSocketFactory PrevSocketFactory;
	private static HostnameVerifier PrevHostnameVerifier;

	@Before
	public void setUp() {
	}

	@Test
	public void testHelloWorld() throws IOException {
		URL obj = new URL("https://localhost:8443/helloWorlD.html");    // Intentional case mismatch
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept-Language", "es, fr;q=0.8, en;q=0.7");
		int responseCode = con.getResponseCode();
		Assert.assertEquals("Valid reponse code", 200, responseCode);

		String contentType = con.getHeaderField("Content-Type");
		String charset = ScriptHelperImpl.GetCharsetFromContentType(contentType);
		Assert.assertTrue("Correct charset", charset.equalsIgnoreCase("utf-8"));

		try (Reader reader = new InputStreamReader(con.getInputStream(), charset)) {
			int aChar;
			StringBuilder sb = new StringBuilder();
			while ((aChar = reader.read()) != -1)
				sb.append((char) aChar);
			Assert.assertTrue("Acceptable language detected", sb.toString().contains("Hola mundo"));
		}
	}

	@After
	public void tearDown() {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		HttpsURLConnection.setDefaultHostnameVerifier(PrevHostnameVerifier);
		HttpsURLConnection.setDefaultSSLSocketFactory(PrevSocketFactory);

		if (proxy != null)
			proxy.stop();
	}
}
