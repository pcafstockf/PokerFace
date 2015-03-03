PokerFace provides the ability to inspect, delegate, modify, or respond, to any request from a client / browser by using Java's Nashorn JavaScript engine.  This combines the flexibility of JavaScript with the full power of Java.  It can be used for sophisticated A/B testing, dynamic load balancing, analytics, or any other task requiring more logic than a simple regex match.  The possibilities are endless.

Scripting is enabled by specifiying a root scripts directory which contains JavaScript files overlaying any url endpoints you may wish to override.  The layout of the root directory will mirror the paths of the url's that PokerFace will recieve.  

###Example
To illistrate PokerFace's scripting capabilities let's use a simple A/B test of new layouts for your web application dashboard, found at `http://www.mydomain.com/myapp/dashboard.html`.  For this example, we will define 4 JavaScript endpoints within a *script-root-dir* directory.

```
script-root-dir
 | myapp
   | ?.js
   | dashboard.html.js
 | mylib
   | favlib.js.js
 | #ignore-me
   | is-not-an-endpoint.js
 | ?.js
```

Passing `'-scripts script-root-dir'` to PokerFace will activate it's scriptability.  The script with the longest path match (if any) will then be selected and run for each client request.

IMPORTANT: Endpoint matching proceeds from most specific to least specific until an interested endpoint is found (if any).  If no interested script endpoint is found, or if a script alters the request, PokerFace will look for a matching remote "Target" and proxy the request out to it.

Requests for `http://www.mydomain.com/mylib/favlib.js` will result in the `favlib.js.js` endpoint being run (we would expect the script to be written to actually return javascript code of some sort to the client).

Endpoint matching proceeds from most specific to least specific until an interested endpoint is found (if any).

Requests for `http://www.mydomain.com/myapp/dashboard.html` will cause the `inspectRequest` method of the `dashboard.html.js` script to be called.  If that endpoint chooses *not* to handle the request, PokerFace will call the `inspectRequest` method of the `script-root-dir/myapp/?.js` script .  If that script also does not choose to handle the request, `script-root-dir/?.js` will be called.

Requests for all *other* pages in the site would cause the `script-root-dir/?.js` endpoint to be called.

Finally please note that any directory beginning with the `#` character will be (recusively) excluded from script discovery.

For more information, please see *Implementing JavaScript Endpoints* below.

###Scripting Command Line Options
* `-scripts`: As describes above, this identifies a root directory containing script endpoints, and turns on PokerFace's scripting capabilities.
* `-library`: Specifies a JavaScript file to be preloaded into the global context of the Nashorn engine and available to all scripts.
* `-watch`: Instructs PokerFace to watch the contents of the scripts directory for changes, and dynamically load, reload, or unload script endpoints as the JavaScript files are added, modified, or deleted.
* `-dynamicTargetScripting`: *Use this option with care!* It allows any JavaScript endpoint to redirect requests to *any* remote target.  Without this option, your scripts are limited to the `-target`s that you predefine.  If you use this feature, make sure your carefully audit your scripts.

###Implementing JavaScript Endpoints
You will need to be familiar with the JavaScript language and especially the concept of closures.  If you haven't read [JavaScript: The Good Parts](http://www.amazon.com/JavaScript-Good-Parts-Douglas-Crockford/dp/0596517742), it's a short quick read and I highly recommend it.

Documentation and comments should be as close as possible to their subject, so please refer to the heavily commented [Endpoint Template](https://raw.githubusercontent.com/pcafstockf/PokerFace/master/Samples/ScriptRoot/EndpointTemplate.js) for remaining script documentation and implementation details.  

You may also want to take a look at the [Hello World](https://raw.githubusercontent.com/pcafstockf/PokerFace/master/Samples/ScriptRoot/HelloWorld.html.js) script.