PokerFace can serve static files from a local directory just like any other web-server.

###Setup
* You will need to have [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or greater installed on your machine.
* If you don't have it already, [download](./downloads.html) the latest PokerFace-0.9.1.jar file.
* Create a local directory structure that looks like this:

```
my-test-dir  		(you can name this whatever you want)
 | my-static-files	(whatever name you want)
   | foo.html		(just some valid html file)
 | PokerFace-0.9.1.jar  (the latest version from the download link above)
```

* Open a command prompt and type `java -version` to ensure you are running Java 8 or greater.
* Change into 'my-test-dir' and launch PokerFace with the command:

```
java -jar PokerFace-0.9.1.jar -listen 127.0.0.1:8080 -files "my-static-files"
```

* Browse to http://localhost:8080/foo.html

Please note that **if** the static files feature is turned on, and a request for an **existing** static file is received, PokerFace will immediately return that file without invoking any scripts or proxying to a remote Target.  This gives you the ability to override and/or cache static assets of remote servers.

PokerFace already understands common 'mime-type' to file 'extension' mappings.  You can add additional mappings by using a [configuration file](./configfile.html).

###Summary
As you can see configuring **PokerFace** as a static file server is quick and easy.  Additional scenarios such as configuring [https connections](./httpsconfig.html), and [redirecting requests](./abscripting.html) are just as simple.
