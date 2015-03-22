**PokerFace** has simple but powerful reverse proxy capabilities.  Being built on top of the Apache HttpComponents NIO library, PokerFace can rapidly handle large numbers of simultaneous connections with very low resource requirements.

###Setup
To get everything setup, let's walk through a simple reverse proxy from your own machine to our good friends over at Stack Overflow.

* You will need to have [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or greater installed on your machine.
* If you don't have it already, [download](./downloads.html) the latest PokerFace-0.9.1.jar file.
* Create a local directory structure that looks like this:

```
my-test-dir  		(you can name this whatever you want)
 | PokerFace-0.9.1.jar  (the latest version from the download link above)
```

* Open a command prompt and type `java -version` to ensure you are running Java 8 or greater.
* Change into 'my-test-dir' and launch PokerFace with the command:

```
java -jar PokerFace-0.9.1.jar -listen 127.0.0.1:8080 -target "/*=http://stackoverflow.com"
```

This tells PokerFace to listen for http connections on localhost port 8080 and to forward all requests over to Stack Overflow.  You can test this by browsing to [http://localhost:8080/](http://localhost:8080/).  Note that it is the Stack Overflow website, but the url is your own machine.  This is a reverse proxy in it's simplest form.

###Basic Reverse Proxy
Let's look at a slightly more realistic reverse proxy configuration that will also illustrates the infamous **DocumentRoot** challenge (which you must be aware of when configuring any reverse proxy):

```
java -jar PokerFace-0.9.1.jar -listen http=0.0.0.0:8080 -target "/public/*=http://new-hotness.mydomain.com" -target "/private/*=http://old-and-busted.mydomain.com/corp#8"
```

This would tell PokerFace to listen for http connections to port 8080 on all interfaces. 
Requests beginning with `/public/` will be proxied to the server at `new-hotness.mydomain.com`. 
Requests beginning with `/private/` will be sent to the `old-and-busted.mydomain.com` server.
All other requests will return a `404` (NOT_FOUND).

####new-hotness
Assuming your machine is 'my-machine', then when a browser makes a request for `http://my-machine:8080/public/foo/bar.html`, PokerFace will make a proxy request to `http://new-hotness.mydomain.com/public/foo/bar.html` and return that content.  

####old-and-busted
You may notice the # (anchor) on the 'old-and-busted' remote target.  This tells PokerFace to strip the first #8 charachters off the beginning of each request **before** sending it on to that target.  So, when it recieves a request for `http://my-machine:8080/private/bar/foo.html`, it will re-write that and make a proxy request to `http://old-and-busted.mydomain.com/corp/bar/foo.html`.

###**The *DocumentRoot* challenge.**
**If** foo.html contains an absolute link to say /assets/my.css, then the **browser** is going to request `http://my-machine:8080/assets/my.css`.  PokerFace will then return a 404 response (since that does not start with either /public/* or /private/*).

Remember, this DocumentRoot challenge is faced by all reverse proxies. Fortunately, there are solutions:

* Ensure your html uses relative paths to child resources.  For example, http://old-and-busted.mydomain.com/corp/bar/foo.html's link would need to be to ./assets/my.css instead of /assets/my.css.  Be careful with this approach though.  ../assets/my.css will put you right back in the same 404 response boat.
* The simplest solution is to add an additional target to the configuration such as:
` -target "/assets/*=http://old-and-busted.mydomain.com/assets" `

###Summary
As you can see configuring **PokerFace** as a reverse proxy is quick and easy.  Additional scenarios such as [overriding remote resources](./fileserver.html), configuring [https connections](./httpsconfig.html), and [redirecting requests](./abscripting.html) are just as simple.

If you need more control over the configuration than what can be obtained from the [command line](./cmdlineopts.html), you can switch to using a [configuration file](./configfile.html).
