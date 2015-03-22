PokerFace scripting can be used for sophisticated [A/B testing](http://en.wikipedia.org/wiki/A/B_testing).  

###Setup
You will need to review the [scripting documentation](./servletscripting.html) to fully understand how to use scripts, but for now let's focus on the highlights.

###Approach
We will start with a trivial example of serving an existing html layout to half our visitors, and a new html test layout to the other half.

Every transaction has a unique id number, and for this example we will use it to serve the existing layout on every even numbered transaction id and the new layout on every odd numbered transaction id.

All scripts must implement the `inspectRequest` method, which recieves the client's [request](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/HttpRequest.html), and the [context](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/protocol/HttpContext.html) of the http transaction.  The transaction id is stored as a string in the `context` and can be accessed with `context.getAttribute('pokerface.txId')`.

The `inspectRequest` method is allowed to return many differnt types of values.  If it returns a string, PokerFace interprets this as a url to be proxied from a remote server.

The complete script for our A/B example is simply:

```
(function() {
	return {
		apiVersion: 1,
		
		inspectRequest: function(request, context) {
			var id = parseInt(context.getAttribute('pokerface.txId'));
			if (id & 1)
				return '/marketing/new-index.html';
			return '/marketing/index.html';	
		}
	};
})();
```

The [request](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/HttpRequest.html) object contains the request url as well as the HTTP request headers.  This would enable you to extract cookie, session, eTag, or other information to identify the user and determine which page to serve.  

###Summary
To gain a better understanding of PokerFace scripts you will need to read through the ['Hello World'](./servletscripting.html) example, but as you can see scripts can be both powerful and simple.
