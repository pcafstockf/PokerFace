/*
 * If you have not read "JavaScript: The Good Parts" by Douglas Crockford, please consider browsing through its < 100 pages at: http://www.amazon.com/JavaScript-Good-Parts-Douglas-Crockford/dp/0596517742
 * All script endpoints are loaded into Nashorn and immediately invoked.  
 * They must return an object that exposes the methods: 'setup', 'inspectRequest', and 'teardown'.
 * They must also expose the generateResponse function IF the inspectRequest function would ever return the 'request' object that it receives.
 * NOTE about Nashorn: 
 * 	Because these will be JDK Nashorn specific scripts, you may use any feature / extension of Nashorn within these scripts.
 * NOTE about 'context' parameter:
 * 	Most of these methods have a parameter named 'context' which is of type (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/protocol/HttpContext.html)
 * 	This HttpContext is valid for the entire lifecycle of the request from the client through the response back to the client.
 * 	It will contain a number of important properties.
 * 		'pokerface.txId':	string containing the unique id of this transaction.
 * 		'pokerface.scriptHelper':	com.bytelightning.opensource.pokerface.ScriptHelper  Exposes useful PokerFace utility functions (you really should check out the documentation for this class).
 * 		'pokerface.scriptLogger':	org.slf4j.Logger that may be used by this endpoint for logging purposes.
 * 		'pokerface.scripts': java.util.NavigableMap<String,JSObject>	A map of all script endpoints known to PokerFace.
 */
(function() {
	// Note the fact that the function is inside a closure, and simply returns an new instance of an object with the required methods.
	return {
		/*
		 * This is a *required* method and provides us with a way to evolve the script API in the future without breaking older scripts.
		 * For now, you should always return 1 from this method.
		 */
		apiVersion: function() {
			return 1;
		},
		
		/*
		 * This method will be called immediately after this script is loaded and run.
		 * It may modify object state in any way it wishes as it is guaranteed *not* to be run concurrently.
		 * You could for example create a ConcurrentHashMap that could safely be modified by the immutable 'inspectRequest' / 'generateResponse' methods.
		 * 
		 * @param path	string path that this endpoint is being registered under.
		 * @param config	org.apache.commons.configuration.HierarchicalConfiguration will be the 'scriptConfig' child element of the PokerFace configuration file.
		 * @param callback	An object exposing a 'setupComplete()' method and a 'setupFailed(string)' method, ONE of which MUST be invoked.  
		 * 					'setupComplete' will register this endpoint.  Do NOT call 'setupComplete' until it is safe to invoke muttable methods such as 'inspectRequest' and 'generateResponse'.
		 * 					Invoking 'setupFailed(string)' WILL result in PokerFace process termination.
		 * @param logger	org.slf4j.Logger that may be used by this endpoint for logging purposes.
		 */
		setup: function(path, config, callback, logger) {
			// This should be the last line of this method.
			callback.setupComplete();
		},
		
		/*
		 * Returning null means this script has no interest in the request
		 * 		In this case no other script methods will be called.
		 * Returning 'this' means this script wishes to handle the request itself.
		 * 		In this case, the 'generateResponse' method called but the 'inspectResponse' method will *not*.
		 * Returning a String, URL, URI, or a new org.apache.http.HttpRequest, means the script wants to modify the request to (or response from) the remote target in some way.
		 * 		In this case, the inspectResponse method will be called but the generateResponse method will *not*.
		 * Returning an org.apache.http.nio.protocol.HttpAsyncRequestConsumer<com.bytelightning.opensource.pokerface.ResponseProducer> delegates all further request handling to that returned object.
		 * 		In this case no other script methods will be called.
		 * 
		 * This MUST be an immutable method (only variables local to this function closure may be modified) because it will be invoked concurrently. 
		 * To reiterate, global and object state must NOT be modified!
		 * BUT you can use the 'context' parameter for transactional scope.
		 * 
		 * @param request	org.apache.http.HttpRequest  (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpRequest.html)
		 * @param context	org.apache.http.protocol.HttpContext	@see notes about context at the top of this file.
		 * @returns See comments above.
		 */
		inspectRequest: function(request, context) {
			context.setAttribute("uripath", this.path);	// Just for demo purposes.  It's okay to read state in this immutable method.  We will save it in the context so we can pull it back out on completion and see that nothing changed
			return this;
		},
		
		/*
		 * Only invoked if the inspectRequest function returned it's own 'request' parameter.
		 * This function then becomes responsible to produce the request that will go back to the client.
		 * 
		 * This MUST be an immutable method (only variables local to this function closure may be modified) because it will be invoked concurrently. 
		 * To reiterate, global and object state must NOT be modified!
		 * BUT you can use the 'context' parameter for transactional scope.
		 * 
		 * @param request	org.apache.http.HttpRequest  (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpRequest.html)
		 * @param context	org.apache.http.protocol.HttpContext	Scope for the lifecycle of this request / response.  (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpRequest.html)
		 * @returns	A JavaScript response object as described in the body below.
		 */
		generateResponse: function(request, context) {
			// This function may return either a native Javascript object OR a properly initialized org.apache.http.HttpResponse object.
			var response = {
				statusCode: 200,		// If not specified, 200 is default
				reasonPhrase: "OK", 	// If not specified 'Ok' is the default
				headers: [
				    // Server controlled headers should *not* be specified ("Connection", "Keep-Alive", "Via", "Content-Length", "Content-Type", "Transfer-Encoding", "Date")
					{'Access-Control-Allow-Origin': '*'},
					// Content-Type may be specified but can also be controlled by the options below.
					// The 'Expires' header may ALSO be specified as a value of '@Number' where the number is an offset (+/-) in seconds to the Date header.
				],
				mimeType: 'text/html',	// If not specified AND Content-Type header is not specified, defaults to text/plain.  If both this and Content-Type are specified, this one takes precedence.
				charset: 'utf-8',		// If not specified AND Content-Type header is not specified, defaults to 'utf-8'.  If both this and Content-Type are specified, this one takes precedence.
				
				// NOW FOR THE FUN !  
				// This is a Nashorn specific script, so the 'content' field may be any one of:
				//		string (aka java.lang.String)
				//		java.lang.byte[]
				//		java.nio.ByteBuffer
				//		java.io.InputStream
				//		java.io.File
				//		org.apache.http.HttpEntity
				//	OR a native javascript function that will produce the content (this allows the response to begin streaming back to the client.
				//		The native javascript function will be invoked with the same parameters as this method, and must return one of the content elements described above.
				// Of course please keep in mind that the content MUST agree with the Content-Type / mimeType / charset (if any).
				content: function(request, context) {	// NOTE: This could also be an internal callback function similar to how the 'completion' callback is handled just below.
					// DANGER!!!  You may NOT access the parent 'generateResponse' closure from this function.  If you need that data, store it in the context in 'generateResponse' and read it back in this function.
					return '<html><head><title>Welcome</title></head><body>Welcome</body></html>';
				},
				// An *optional* completion function will be invoked after the response has been fully streamed back to the client.
				// If specified, this MUST *also* be an immutable method 
				// NOTE: This could also be an anonymous function instead of a method on this object (e.g. completion: function(response, context, error) { ..whatever.. };  BUT please see the DANGER note above as the same rules apply.
				completion: this.responseCompleted,
			};
			return response;
		},
		
		/*
		 * NOTE: As used in this template, this is a dual usage method (you would only use A *or* B (both would not make any sense).
		 * 	A.) Used as an internal callback method that the 'generateResponse' method passed back as the 'completion' attribute of the response (see the NOTE about that attribute above).
		 * 		It will be invoked after the response from 'generateResponse' has been fully streamed to the client, and is an opportunity to clean up any transaction scoped elements.
		 * 	B.) As a mandatory api method called after the optional 'inspectResponse' method.
		 * 
		 * This MUST be an immutable method (only variables local to this function closure may be modified) because it will be invoked concurrently. 
		 * To reiterate, global and object state must NOT be modified!
		 * Pay attention to the 'context' parameter you used transactional scope.
		 * 
		 * @param response	org.apache.http.HttpResponse	The (PokerFace transformed) result of the 'generateResponse' function. (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpResponse.html)
		 * @param context	org.apache.http.protocol.HttpContext	Scope for the lifecycle of this request / response.  (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/protocol/HttpContext.html)
		 * @param error	java.lang.Exception	If null the transaction completed successfully, otherwise an error occurred reading the response from the target or writing the response to the client.  
		 */
		responseCompleted: function(response, context, error) {
		},
		
		/*
		 * This method will *only* be called IF the 'inspectRequest' method indicated that it wished to modify the request to (or response from) the remote target.
		 * This method may modify the 'response' parameter (org.apache.http.HttpResponse) it receives, or return a new instance of org.apache.http.HttpResponse.
		 * 
		 * This MUST be an immutable method (only variables local to this function closure may be modified) because it will be invoked concurrently. 
		 * To reiterate, global and object state must NOT be modified!
		 * Pay attention to the 'context' parameter you used transactional scope.
		 * 
		 * @param response	org.apache.http.HttpResponse	The (PokerFace transformed) result of the 'generateResponse' function. (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpResponse.html)
		 * @param context	org.apache.http.protocol.HttpContext	Scope for the lifecycle of this request / response.  (http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/protocol/HttpContext.html)
		 * @returns See comments above.
		 */
		inspectResponse: function(response, context) {
			return response;	// Can return 'response', a new org.apache.http.HttpResponse, or omit the return statement entirely.
		},
		
		/*
		 * This method will be called when PokerFace is finished with the endpoint (shutting down, dynamically reloading a modified script, or dynamically removing a deleted script).
		 * It may modify object state in any way it wishes as it is guaranteed *not* to be run concurrently.
		 */
		teardown: function() {
		}
	};
})();	// Invoke the function so that when this script is run, it returns an 'endpoint' instance.
