**PokerFace** scripts can be written to handle any incoming request and its response.  


###Setup
Let's implement a simple 'Hi' script so that we can configure and test PokerFace.  Then we will implement a more feature rich 'Hello World' script.

**PokerFace** scripts are incredibly simple:

```
(function() {
	var endpoint = {
		apiVersion: 1,
		
		inspectRequest: function(request, context) {
			return this;
		},

		generateResponse: function(request, context) {
			var response = {
				mimeType: 'text/html',
				content: '<html><head><title>Hi</title></head><body>Hi!</body></html>'
			};
			return response;
		}		
	};
	return endpoint;
})();
```

All scripts must be written as a function which returns an `endpoint` object.  
Endpoint's must define:

* A numeric attribute named `apiVersion` (currently always 1).  
* A method named `inspectRequest`, receiving [request](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/HttpRequest.html), and [context](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/protocol/HttpContext.html) parameters.

The `inspectRequest` method is allowed to return many differnt types of data.  Returning `this` means the script wishes to handle the request itself, and in this scenario the endpoint must also define a `generateResponse` method.

The `generateResponse` method must return an object describing the response to be sent back to the client.  We will discuss all the attributes of this 'response' object later, but as you can see, it's pretty simple.

###Testing the 'Hi' script
* You will need to have [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or greater installed on your machine.
* If you don't have it already, [download](./downloads.html) the latest PokerFace-0.9.2.jar file.
* Create a local directory structure that looks like this:

```
my-test-dir  		(you can name this whatever you want)
 | my-script-root	(whatever name you want)
   | hi.html.js		(the contents of the above 'hi' script)
 | PokerFace-0.9.2.jar  (the latest version from the download link above)
```

* Open a command prompt and type `java -version` to ensure you are running Java 8 or greater.
* Change into 'my-test-dir' and launch PokerFace with the command:

```
java -jar PokerFace-0.9.2.jar -listen 127.0.0.1:8080 -scripts "my-script-root"
```

* Browse to [http://localhost:8080/hi.html](http://localhost:8080/hi.html) to get an appropriate greeting :-)

That's it.  Congratulations on your first **PokerFace** script !

###Important (don't skip this) !
**PokerFace** scripts are simple and easy, but there is one key concept that you **must** wrap your head around.  Let's implement a Hello World script to illistrate.

For a script of any significance you need to be familiar with JavaScript and especially the concept of closures.  [Here](http://www.w3schools.com/js/js_function_closures.asp) is a simple explanation of closure's, and [here](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Closures) is a more complete explanation.
If you haven't read [JavaScript: The Good Parts](http://www.amazon.com/JavaScript-Good-Parts-Douglas-Crockford/dp/0596517742), it's a short quick read and highly recommended.

Java 8's [Nashorn](http://openjdk.java.net/projects/nashorn/) engine is amazing in how smoothly it integrates Java and JavaScript.  We have all the convience and flexibility of JavaScript, **and** the power of the entire Java universe.

**PokerFace** is built on the [Apache NIO HttpComponents](http://hc.apache.org/index.html) library.  This asynchrounous, streaming, http library allows PokerFace to rapidly handle large numbers of simultaneous connections with very low resource requirments.

The intersection of these three technologies (Java, JavaScript, NIO HttpComponents), is extremely powerful.  
But [with great power comes great responsibility](http://en.wikipedia.org/wiki/Uncle_Ben#.22With_great_power_comes_great_responsibility.22).  
By keeping in mind the responsibilities this environment brings, we can enjoy the power of all three of these technologies.  

#####The 'environment':
Your script `endpoint` object will be called simultaneously from multiple threads.  It is safe to *read* any local, global or 'this' variables.  However, because you are in a multi-threaded world, and because JavaScript is not multi-threaded, you may only *write* to local variables.  A workaround for this will be described in a moment.

#### Hello World
Now let's implement a multi-lingual 'Hello World' script that examines the 'Accept-Language' http header to respond in French, Spanish, or English (default).  We will also display the appropriately formated date and time for each browser's locale.  

We will use [moment.js](http://momentjs.com) to format the date and time.  It would be easier to use Java's [SimpleDateFormat](http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html), but using moment.js will give us a chance to illustrate the inclusion of external javascript libraries as well as the how to deal with JavaScript closures.

Here is the entire 'Hello World' script which we will discuss in detail below.

```
(function() {
	var langData = {
		en: { },
		fr: { },
		es: { }
	};
	return {
		apiVersion: 1,
		
		setup: function(path, config, logger, callback) {
			// configure moment.js and our greetings
			langData.en.formatter = moment();
			langData.en.formatter.locale('en');
			langData.en.message = config.getString('en');
			langData.fr.formatter = moment();
			langData.fr.message = config.getString('fr');
			langData.fr.formatter.locale('fr');
			langData.es.formatter = moment();
			langData.es.formatter.locale('es');
			langData.es.message = config.getString('es');

			// Illustration Java/JavaScript integration by defering the invocation of setupComplete().
			var Thread = Java.type('java.lang.Thread');
			new Thread(function() {
				// Legitimate to access 'logger' in the outer closure because the 'setup' method is not multi-threaded.
				logger.info(path + ' setup successfully.');
				callback.setupComplete();	// Must be called to register this endpoint
			}).start();
		},
		
		inspectRequest: function(request, context) {
			// store a value that we will retrieve in the 'generateResponse' method below.
			context.setAttribute('my.hello.charset', 'utf-8');
			// Return 'this' to tell PokerFace we will handle the request.
			return this;
		},

		generateResponse: function(request, context) {
			var theCharset = context.getAttribute('my.hello.charset');
			var response = {
				mimeType: 'text/html',
				charset: theCharset,
				content: function(request, context) {
						// WARNING: You may *not* access 'theCharset' variable above from within this callback !
						// It is in an outer closure that will be *undefined* when this callback is invoked.
						// See heading labeled 'Important' in the documentation above.
						// BUT, you could retrieve it using context.getAttribute('my.hello.charset');
					var helper = context.getAttribute('pokerface.scriptHelper');
					var locale = 'en';
					var acceptables = helper.getAcceptableLocales();
					for (var idx=0; idx<acceptables.length; idx++)
						if (acceptables[idx] in langData) {
							locale = acceptables[idx];
							break;
						}
					return '<html><head><title>Welcome</title></head><body>' + langData[locale].message + '<br/>' + langData[locale].formatter.format('MMMM Do YYYY, h:mm:ss a') + '</body></html>';
				}
			};
			return response;
		}
	};
})();
```

If an `endpoint` defines a `setup` method, it will be invoked just once, immediately after the script is run.  This method is guaranteed to only be called within a single threaded environment.  You can do pretty much anything in this method, including defered loading of things like a java based JDBC connection pool, initializing global and instance variables, etc.  However, if you do declare a 'setup' method, your script will **not** be registered with PokerFace until such time as you invoke 'callback.setupComplete()'.

Full documentation of all enpoint methods, parameters, and return values can be find in [EndpointTemplate.js](./EndpointTemplate.js.html).

###Configuring and Testing Hello World
Before we explain the script any further, let's see it in action.

* Create a new directory called `my-script-libs` inside `my-test-dir` (created earlier).
* Download the moment-with-locals-2.9.0.js file from the [downloads](./downloads.html) page and save it in `my-script-libs`.

Because we want to provide configurable greetings to our script (see setup method), we need to switch from passing all options on the command line to using a [configuration file](./configfile.html).

* Copy this xml config and save it as `my-test-config.xml` inside `my-test-dir`.

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration 
	xmlns="http://www.bytelightning.com/opensource.pokerface/xsd/v1"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.bytelightning.com/opensource.pokerface/xsd/v1 /PokerFace_v1Config.xsd"
>
	<server>
		<listen address="127.0.0.1" port="8080"/>
	</server>
	<scripts>
		<rootDirectory>my-script-root</rootDirectory>
		<library>my-script-libs/moment-with-locals-2.9.0.js</library>
		<scriptConfig>
			<en>Hello world</en>
			<fr>Bonjour monde</fr>
			<es>Hola mundo</es>
		</scriptConfig>
	</scripts>
</configuration>
```

* Copy the Hello World script above and save it as as `helloworld.html.js` in `my-script-root` (created earlier).

At this point your test directory should look like this:

```
my-test-dir\
 | my-script-root
   | hi.html.js
   | helloworld.html.js
 | my-script-libs
   | moment-with-locals-2.9.0.js
 | my-test-config.xml
 | PokerFace-0.9.2.jar  (download the latest version from the link above)
```

* Bring up a command prompt, change into 'my-test-dir' and launch **PokerFace** with the command:

```
java -jar PokerFace-0.9.2.jar -config "my-test-config.xml"
```

* Browse to [http://localhost:8080/helloworld.html](http://localhost:8080/helloworld.html) to recieve an appropriate greeting :-)
* If you want, change the preferred language of your browser to see greetings in another of the supported languages.

###Examining the script
We declare a private `langData` object to hold the [moment.js formatters](http://momentjs.com/docs/#/displaying/) and our greeting strings.  We initialize these formatters within the setup method because we don't want to risk creating one within the `generateResponse` method.  

*If* `moment()` were to alter internal closured variables within the multi-threaded environment of our `generateResponse` method, the results would be undefined.

The `config` parameter passed to the `setup` method will contain the xml child elements of the `scriptConfig` element of our `my-test-config.xml` file.  This is how we make the greeting "configurable".

The `generateResponse` method is similar to the one for `hi.html.js` above, except that instead of returning a string for the `content` field, it returns a JavaScript function.  
The advantage of this approach is that it allows PokerFace to immediately begin streaming the response headers back to the client while we finish building up the response body.  The disadvantage is that you must use care to avoid accessing outer variables in the `generateResponse` methods closure.

The `generateResponse` method receive the same two parameters that are initially passed to `inspectRequest`.  The client [request](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/HttpRequest.html), and the [context](http://hc.apache.org/httpcomponents-core-4.3.x/httpcore/apidocs/org/apache/http/protocol/HttpContext.html).  This context object is unique to each http transaction and may be used to store values you wish to share across methods and callbacks.

The `context` parameter is more fully described in [EndpointTemplate.js](./EndpointTemplate.js.html) but for now, the attribute key we are interested in is "pokerface.scriptHelper".  The value of this key is an object of type [ScriptHelper](http://pcafstockf.github.io/PokerFace/apidocs/com/bytelightning/opensource/pokerface/ScriptHelper.html) which exposes many useful PokerFace utility functions, **including one to determine the browsers prefered language**.

With all this in mind, please re-read the comments in the `helloworld.html.js` script and see if they make sense to you.

###Summary
That's it.  As you can see, once you have your head wrapped around how JavaScript closure's intract with multi-threaded NIO, the scripts are quite simple and powerful.

For more information on supported script methods and parameters, please see [EndpointTemplate.js](./EndpointTemplate.js.html).
