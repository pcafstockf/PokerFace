**PokerFace** is a free, lightweight, asynchronous, streaming, scriptable, http(s) **reverse proxy**.  

In addition to traditional reverse proxy features such as, web server, http(s) sanitizer, load balancer, and ssl bridge, PokerFace is **scriptable**.  

Scriptability means **PokerFace** can be used for sophisticated A/B testing, dynamic load balancing, analytics, mock data services, JavaScript "servlets", content caching, or any other task requiring more than a simple regex match.  

PokerFace is small (e.g. easy to audit), extremely fast, portable, and built on the popular Apache HttpCore NIO libraries to provide top performance, scalability, and standards compliance.

Here are some short guides focused on specific objectives to help you get started.

* [Reverse proxy to one or more remote servers](./reverseproxy.html).
* [Serve local files and/or cache remote resources](./fileserver.html).
* [Create web pages and services using JavaScript](./servletscripting.html).
* [A/B testing using JavaScript](./abscripting.html).
* [Using log4j or other logging frameworks](./loggingconfig.html).
* [Https configuration and SSL bridging](./httpsconfig.html).

###Command line options
Simple / common scenarios can be easily configured via [the command line](./cmdlineopts.html).  
More complex scenarios are configured via an [XML configuration file](./configfile.html).  
