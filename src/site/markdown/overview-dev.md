This document is just a quick, high level overview of the design, classes, and code for PokerFace.  If you are looking for specifics of a paticular class or method, please consult it's Javadoc.

PokerFace only deals with http/https protocols, and handles everything asynchrounously.  Because of that this document will drop the HttpAsync prefixes and suffixes commonly used in `org.apache.http.*`.

With all the request / response / consumer / producer combinations, it is important to keep these specific meanings straight in your head:

<a name="RequestConsumer"></a>**Request-Consumer**: Consumes an HttpRequest from a client / browser.

* [RequestForScriptConsumer](apidocs/com/bytelightning/opensource/pokerface/RequestForScriptConsumer.html), [RequestForTargetConsumer](apidocs/com/bytelightning/opensource/pokerface/RequestForTargetConsumer.html), [RequestForFileConsumer](apidocs/com/bytelightning/opensource/pokerface/RequestForFileConsumer.html)

**Request-Producer**: Issues an HttpRequest to a remote system.

* [TargetRequestProducer](apidocs/com/bytelightning/opensource/pokerface/TargetRequestProducer.html)

**Response-Consumer**: Reads the response back from a remote system.

* [TargetResponseConsumer](apidocs/com/bytelightning/opensource/pokerface/TargetResponseConsumer.html)

**Response-Producer**: Sends an HttpResponse back to a client / browser.

* [ResponseProducer](apidocs/com/bytelightning/opensource/pokerface/ResponseProducer.html), [ScriptResponseProducer](apidocs/com/bytelightning/opensource/pokerface/ScriptResponseProducer.html)

###<a name="TargetDescriptor"></a>TargetDescriptor (aka Target)
A [TargetDescriptor](apidocs/com/bytelightning/opensource/pokerface/TargetDescriptor.html) is basically an [org.apache.http.HttpHost](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/HttpHost.html) descrbing a remote system which PokerFace proxys to, and which keeps track of some meta-info that allows us to optionally transform the request url from the client into a suitable request to the remote Target.

###<a name="RequestHandler"></a>RequestHandler
The [RequestHandler](apidocs/com/bytelightning/opensource/pokerface/RequestHandler.html) is a singleton dispatcher that analyzes all new request from the browser (which may not yet be fully received yet).  Because this object is the brains behind PokerFace, we will discuss it in more detail:

The [RequestHandler.processRequest](apidocs/com/bytelightning/opensource/pokerface/RequestHandler.html#processRequest-org.apache.http.HttpRequest-org.apache.http.protocol.HttpContext-) method is called by the framework, and always returns a subclass instance of [AbsClientRequestConsumer](apidocs/com/bytelightning/opensource/pokerface/AbsClientRequestConsumer.html) (see below).  

The flow of the `processRequest` method is as follows:

1. Checks to see if there is a static file matching the request uri.
2. If there is a matching static file, a [RequestForFileConsumer](apidocs/com/bytelightning/opensource/pokerface/RequestForFileConsumer.html) is returned.
3. Checks to see if any script endpoint matches the browser request.
	* Please see the 'inspectRequest()' method in the [EndpointTemplate.js](http://pcafstockf.github.io/PokerFace/EndpointTemplate.js.html) file for further documentation.
4. If the script wished to handle the request, a [RequestForScriptConsumer](apidocs/com/bytelightning/opensource/pokerface/RequestForScriptConsumer.html) is returned.
5. The request (possibly modified by the script) is matched against configured proxy [TargetDescriptor](#TargetDescriptor)s
	* If a configured `TargetDescriptor` is **not** found **and** the redirect request came from the script endpoint, then **IF** the `dynamicHostMap` option is set to true, a proxy `TargetDescriptor` will be created on the fly.
6. The `processRequest` method then returns a [RequestForTargetConsumer](apidocs/com/bytelightning/opensource/pokerface/RequestForTargetConsumer.html) to consume the request.
	* NOTE: The Target itself may be null at this point, and if so the `RequestForTargetConsumer.getResult` method will return a [ResponseProducer](apidocs/com/bytelightning/opensource/pokerface/ResponseProducer.html) instance that will send a 404 response back to the client.

NOTE: All [Request-Consumer](#RequestConsumer)'s have a [getResult](apidocs/com/bytelightning/opensource/pokerface/AbsClientRequestConsumer.html#getResult--) method that returns a [ResponseProducer](apidocs/com/bytelightning/opensource/pokerface/ResponseProducer.html) instance which will in turn produces the response back to the client.  The framework will pass this `ResponseProducer` to the [RequestHandler.handle](apidocs/com/bytelightning/opensource/pokerface/RequestHandler.html#handle-com.bytelightning.opensource.pokerface.ResponseProducer-org.apache.http.nio.protocol.HttpAsyncExchange-org.apache.http.protocol.HttpContext-) method.

###Building
On a Mac:

```
export JAVA_HOME="`/usr/libexec/java_home -v '1.8*'`"
mvn clean package site
```