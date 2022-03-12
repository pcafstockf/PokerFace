###Logging
By default **PokerFace** uses the standard java.util.logging framework built into the JVM.

Thanks to [SLF4J](http://www.slf4j.org), it is possible to configure PokerFace to use other logging frameworks.  This is done by specifying a logging framework and its SLF4J implementation module on the classpath ahead of the PokerFace jar.  For example to use log4j, you could launch PokerFace with:

```
java -Dlog4j.configuration="path/to/log4j.properties" -classpath "path/to/log4j.jar:path/to/slf4j-log4j12.jar:path/to/PokerFace-0.9.4.jar" com.bytelightning.opensource.pokerface.PokerFaceApp -config "path/to/pokerface-config.xml"

NOTE: On Windows you will need to change the class path separator character above from ':' to ';'.
```

You will recieve a warning from SLF4J that the "Class path contains multiple SLF4J bindings".  This is expected, as the PokerFace jar file already contains a binding to the standard java logging framework, and we have just added another one to the classpath.  
You need to look at the warning to be sure the right logging framework was selected, but otherwise the warning is harmless.  You can read more about the warning [here](http://www.slf4j.org/codes.html#multiple_bindings) (see the 'Note' tag at the bottom of that section).

Please see [SLF4J](http://www.slf4j.org) for a list of supported logging frameworks and where to obtain them.
