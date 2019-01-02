**PokerFace** can be configured as a secure https web server, as well as an https reverse proxy to a plain http backend server (aka ssl bridging).

###Setup
Let's configure **PokerFace** as a static file server, and then enhance it with ssl.

Please follow the steps outlined [here](./fileserver.html) and ensure you are able to serve static files as explained in that document.

###Adding https

For production you will need a certificate signed by a well known CA, but for testing purposes it is common to quickly create a self signed certificate:

```
$JAVA_HOME/bin/keytool -genkey -alias selfserve -keyalg RSA -keypass changeit -storepass changeit -keystore serverkeys.jks
```

* Place the keystore containing your certificate in the directory `my-test-dir` which you created earlier during the static file walkthrough.

* Your local directory structure should now look like this:

```
my-test-dir  		(you can name this whatever you want)
 | my-static-files	(whatever name you want)
   | foo.html		(just some valid html file)
 | serverkeys.jks	(whatever name you want)
 | PokerFace-0.9.2.jar  (the latest version from the download link above)
```
* Change into 'my-test-dir' and launch PokerFace with the command:

```
java -jar PokerFace-0.9.2.jar -keystore "serverkeys.jks" -storepass "changeit" -keypass "changeit" -listen "selfserve,ssl=0.0.0.0:8080" -files "my-static-files"
```

* Browse to https://localhost:8080/foo.html .  
NOTE: If you used a self signed certificate, you will of course be prompted as to whether you trust the server.

By default, PokerFace will select the appropriate certificate from the keystore based on the hostname of the network interface on which the request was recieved.  You can override this by prefixing the listen address with the certificate alias.  

In the example above, we bound all network interfaces on the machine to use the certificate with the alias 'selfserve'.  Please see the [command line options](./cmdlineopts.html) for usage of `-listen`.

###Summary
As you can see configuring **PokerFace** as a secure https server is quick and easy.  Additional scenarios such as [overriding remote resources](./fileserver.html), configuring as a [reverse proxy](./reverseproxy.html), and [redirecting requests](./abscripting.html) are just as simple.

If you need more control over the configuration than what can be obtained from the [command line](./cmdlineopts.html), you can switch to using a [configuration file](./configfile.html).
