###Command Line Options

* `-h`:	Print a list of supported command line options and exit.

---

* `-listen <arg>`:	Multiple -listen options may be specified.  `<arg>` is formated as x=address:port.  
The 'x=' prefix is also optional and defaults to 'http='.  
The ':port' suffix is optional and defaults to port 80 for http and 443 for https.  
Recognized values of 'x' are a comma separated list of the following tokens:
	* `http`:	Non-secure
	* `https`:	The most secure algorithm available on the machine (typically TLS v1.2)
	* `ssl`:	Less secure compatibility mode using ssl instead of tls encryption.
	* `tls`:	Synonymous with https
	* A valid certificate alias	:	Binds a specific certificate to the address (implies https).

---

* `-servercpu <arg>` and `-targetcpu <arg>`:	Set the number of threads used for incomming requests from the client, and outgoing requests to remote targets, respectively.  With PokerFace's NIO model, you do not need a high thread count.  If you use these parameters it is recommended that you pass the number of cpu cores in your system.  
'arg' is a decimal number as follows:  
	- if < 0: Use the same number of threads as the number of physical cpu cores in the machine.
	- if >=  1:  Use this number of threads (up to twice the number of physical cores).
	- if > 0 and < 1: This fraction multiplied by the number of physical cores (e.g. 0.66 would be 2/3 the cores).  

---

* `-files <arg>`:	Filepath to a directory of static files.  
See [web server configuration](./fileserver.html) for more details.

---

* `-target <arg>`: Specify a remote target proxy.  Multiple target options may be specified.
'arg' is formated as requestPattern=targetUrl  
* `-trustany`:  Ignore certificate identity errors from target servers.  
See [Configuring a reverse proxy](./reverseproxy.html) for more information.

---

* `-keystore <arg>`:	Filepath to the certificate keystore.
* `-storepass <arg>`:	The store password of the keystore.
* `-keypass <arg>`:	The key password of the keystore.   
See [Https configuration](./httpsconfig.html) for more information.

---

* `-scripts <arg>`:	Filepath for root scripts directory.
* `-library <arg>`:	Filepath to JavaScript library to be loaded into global context.  Multiple library options may be specified.
* `-watch`:	Dynamically watch scripts directory for changes.
* `-dynamicTargetScripting`:	WARNING! This option allows scripts to redirect requests to *any* other remote server.  
See [Scripting](./servletscripting.html) for more information.

---

* `-config <arg>`:	Path to an XML configuration file allowing you to specify complex configurations for PokerFace.  
Command line options override xml configurtion values so you could for instance, specify everything except the certificate key password in an XML file and then pass the key password on the command line.  
There are some important caveats with using the 'config' option, so please see [XML configuration file](./configfile.html) documentation for more information.
