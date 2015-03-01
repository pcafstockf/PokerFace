This guide will give you a basic introduction to PokerFace and describes some simple tasks that can be accomplished with it.  
This guide describes some simple tasks that can be accomplished with Pokerface.
###Basic Command Line Syntax
You could 'fire up' a simple local reverse proxy with:

```
java -jar PokerFace-{version}.jar -listen 127.0.0.1:8080 -target "/*=http://stackoverflow.com"
```

This tells PokerFace to listen for http connections on localhost port 8080 and to forward all requests over to our good friends at Stack Overflow: [http://localhost:8080/](http://localhost:8080/)

###Help
Simple / common scenarios can be easily described via the command line, while more complex scenarios will require a configuration file.  You can get a complete list of command line options with:

```
java -jar PokerFace-{version}.jar -h
```

###Basic Reverse Proxy
Here is a possible real world setup that also illustrates the infamous **DocumentRoot** challenge (which you must be aware of when configuring any reverse proxy):

```
java -jar PokerFace-{version}.jar -listen https=0.0.0.0:443 -target "/public/*=https://new-hotness.mydomain.com" -target "/private/*=http://old-and-busted.mydomain.com/corp#8"  -keystore serverkey.jks -keypass changeit
```

This would tell PokerFace to listen for https connections to port 443 on all interfaces (multiple *-listen* arguments are allowed). 
Requests beginning with `/public/` will be proxied to the server at `new-hotness.mydomain.com`. 
Requests beginning with `/private/` will be sent to the `old-and-busted.mydomain.com` server.
All other requests will return a `404` (NOT_FOUND).

Assuming PokerFace is running on a machine named `www.mydomain.com`, lets walk through the *DocumentRoot* challenge.

####new-hotness
When PokerFace recieves a request for `https://www.mydomain.com/public/foo/bar.html`, it will make a proxy request to `https://new-hotness.mydomain.com/public/foo/bar.html`.  
However, **if** bar.html contains an absolute link to /assets/my.css, then the browser will request https://www.mydomain.com/assets/my.css, and then PokerFace will return a 404 response (since that does not start with either /public/* or /private/*).

Remember, this DocumentRoot challenge is faced by all reverse proxies. Fortunately, there are work-arounds to solve this problem:

* One way is to ensure your html uses relative paths (e.g. /public/foo/bar.html 's link would be to ../assets/my.css).
* Another way is to add an additional target to the configuration such as:
` -target "/assets/*=https://new-hotness.mydomain.com" `

####old-and-busted
You may notice the # (anchor) on the old-and-busted remote target.  This tells PokerFace to strip the first #8 charachters off each request before sending it on to the target.  Therefore, when it recieves a request for `https://www.mydomain.com/private/bar/foo.html`, it will make a proxy request to `https://old-and-busted.mydomain.com/corp/bar/foo.html`.

####Securing https
For a production server you will need a certificate signed by a well known CA.  But for testing purposes, you can quickly create a self signed certificate with:

```
$JAVA_HOME/bin/keytool -genkey -alias www.mydomain.com -keyalg RSA -keypass changeit -storepass changeit -keystore serverkey.jks
```

####Multi-port / alternate certificates
By default, PokerFace will select the appropriate certificate from the keystore based on the hostname on which the request was recieved.  You can override this by prefixing the listen address with the certificate alias.  So, `-listen alt.mydomain.com=0.0.0.0:443`  will find the certificate with an alias of alt.mydomain.com in the keystore and present that as the https certificate.

###Logging
PokerFace supports many logging frameworks thanks to [SLF4J](http://www.slf4j.org).  By default it uses the standard java.util.logging framework built into the JDK.  You can override this by specifying one of the SLF4J implementation modules first on the classpath.  For example to use log4j, you would launch PokerFace with:

```
java -classpath "path/to/slf4j-log4j12.jar;path/to/PokerFace-{version}.jar" com.bytelightning.opensource.pokerface.PokerFaceApp -config path/to/config.xml
```


###Additional Command Line Options

* `-servercpu` / `-targetcpu`: Controls the number of threads for incomming requests from the client, and outgoing requests to remote targets, respectively.  Since PokerFace uses the Java NIO model, you do not need a high thread count.  If you use these parameters it is recommended that you pass 2/3 the number of cpu cores in your system.  Both of these options provide flexible specification modes:

	* if < 0: Use the actual number of physical cpu cores in the machine.
	* if >=  1:  Use this number (up to twice the number of physical cores).
	* if > 0 and < 1: This fraction multiplied by the number of physical cores (e.g. 0.66 would be 2/3 the cores).
* `-trustany`: Tells PokerFace to ignore any certificate errors that the target servers might generate (e.g. invalid hostname, etc.).
* `-config`: allows you to specify complex configurations for PokerFace using XML. For instance, you could specify everything except the certificate key password in an XML file and then pass the password on the command line.  Documentation and comments should be as close as possible to their subject, so please see the heavily commented [sample configuration file](Samples/SampleConfig.xml) which discusses all configurable options for PokerFace.
* `-scripts`, `-watch`, and `-dynamicTargetScripting` options are discussed in [Scripting PokerFace](ScriptingPokerFace.html).
