**PokerFace** allows for great control over it's configuration via an XML file that is specified from the command line using the `-config <filepath>` option.

Generally command line options override any settings in the configuration file with the exception of the `-listen`, `-target`, `-scripts`, and `-library` options.  These will be ignored from the command line when a configuration file is used.
				 
###XSD
The formal XSD for the configuration file can be found [here](https://raw.githubusercontent.com/pcafstockf/PokerFace/master/src/main/resources/PokerFace_v1Config.xsd).

###Sample Configuration.
A sample configuration file can be found [here](https://raw.githubusercontent.com/pcafstockf/PokerFace/master/Samples/SampleConfig.xml).

###Documentation
Below is documenttion for each of the configuration elements.  
Click each element to expand/collapse the documentation for it and it's child elements.

<iframe id="configdoc" src="./ConfigurationDoc.html" frameborder="0" width="100%" scrolling="no"></iframe>
<script src="./js/iframeResizer.min.js"></script>
