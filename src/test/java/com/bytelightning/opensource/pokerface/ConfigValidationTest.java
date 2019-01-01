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

import org.junit.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Sanity check to make sure our sample and test config's all stay in synch with our schema
 */
public class ConfigValidationTest {

	@SuppressWarnings("RedundantThrows")
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@SuppressWarnings("RedundantThrows")
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void validateSampleConfig() throws SAXException, IOException {
		URL xsdUri = PokerFaceApp.class.getResource("/PokerFace_v1Config.xsd");
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(xsdUri);
		Validator validator = schema.newValidator();
		File config = new File(new File("Samples"), "SampleConfig.xml");
		validator.validate(new StreamSource(config));
	}

	@Test
	public void validateProxySpecificTestConfig() throws SAXException, IOException {
		URL xsdUri = PokerFaceApp.class.getResource("/PokerFace_v1Config.xsd");
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(xsdUri);
		Validator validator = schema.newValidator();
		File config = new File(new File("src/test/resources"), "ProxySpecificTestConfig.xml");
		validator.validate(new StreamSource(config));
	}

	@Test
	public void validateHelloWorldTestConfig() throws SAXException, IOException {
		URL xsdUri = PokerFaceApp.class.getResource("/PokerFace_v1Config.xsd");
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(xsdUri);
		Validator validator = schema.newValidator();
		File config = new File(new File("src/test/resources"), "HelloWorldTestConfig.xml");
		validator.validate(new StreamSource(config));
	}

	@SuppressWarnings("RedundantThrows")
	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("RedundantThrows")
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
}
