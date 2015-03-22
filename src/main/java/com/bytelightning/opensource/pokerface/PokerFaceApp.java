package com.bytelightning.opensource.pokerface;
/*
The MIT License (MIT)

PokerFace: Asynchronous, streaming, HTTP/1.1, scriptable, reverse proxy.

Copyright (c) 2015 Frank Stock

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.resolver.DefaultEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Main entry point for PokerFace, this class analyzes various command line switches and builds up a configuration that it delegates to an instance of the <code>PokerFace<code> class to actually perform the configuration.
 */
public class PokerFaceApp {
	public static void main(String[] args) {
	    if (JavaVersionAsFloat() < (1.8f - Float.MIN_VALUE)) {
	    	System.err.println("PokerFace requires at least Java v8 to run.");
	    	return;
	    }
		// Configure the command line options parser
		Options options = new Options();
		options.addOption("h", false, "help");
		options.addOption("listen", true, "(http,https,secure,tls,ssl,CertAlias)=Address:Port for https.");
		options.addOption("keystore", true, "Filepath for PokerFace certificate keystore.");
		options.addOption("storepass", true, "The store password of the keystore.");
		options.addOption("keypass", true, "The key password of the keystore.");
		options.addOption("target", true, "Remote Target requestPattern=targetUri");	// NOTE: targetUri may contain user-info and if so will be interpreted as the alias of a cert to be presented to the remote target
		options.addOption("servercpu", true, "Number of cores the server should use.");
		options.addOption("targetcpu", true, "Number of cores the http targets should use.");
		options.addOption("trustany", false, "Ignore certificate identity errors from target servers.");
		options.addOption("files", true, "Filepath to a directory of static files.");
		options.addOption("config", true, "Path for XML Configuration file.");
		options.addOption("scripts", true, "Filepath for root scripts directory.");
		options.addOption("library", true, "JavaScript library to load into global context.");
		options.addOption("watch", false, "Dynamically watch scripts directory for changes.");
		options.addOption("dynamicTargetScripting", false, "WARNING! This option allows scripts to redirect requests to *any* other remote server.");
		
		CommandLine cmdLine = null;
		// parse the command line.
		try {
			CommandLineParser parser = new PosixParser();
			cmdLine = parser.parse(options, args);
			if (args.length == 0 || cmdLine.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.setWidth(120);
				formatter.printHelp(PokerFaceApp.class.getSimpleName(), options);
				return;
			}
		}
		catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		catch (Exception ex) {
			ex.printStackTrace(System.err);
			return;
		}
		
		XMLConfiguration config = new XMLConfiguration();
		try {			
			if (cmdLine.hasOption("config")) {
				Path tmp = Utils.MakePath(cmdLine.getOptionValue("config"));
				if (!Files.exists(tmp))
					throw new FileNotFoundException("Configuration file does not exist.");
				if (Files.isDirectory(tmp))
					throw new FileNotFoundException("'config' path is not a file.");
				// This is a bit of a pain, but but let's make sure we have a valid configuration file before we actually try to use it.
				config.setEntityResolver(new DefaultEntityResolver() {
					@Override
					public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
						InputSource retVal = super.resolveEntity(publicId, systemId);
						if ((retVal == null) && (systemId != null)) {
							try {
								URL entityURL;
								if (systemId.endsWith("/PokerFace_v1Config.xsd"))
									entityURL = PokerFaceApp.class.getResource("/PokerFace_v1Config.xsd");
								else
									entityURL = new URL(systemId);
				                URLConnection connection = entityURL.openConnection();
				                connection.setUseCaches(false);
				                InputStream stream = connection.getInputStream();
				                retVal = new InputSource(stream);
				                retVal.setSystemId(entityURL.toExternalForm());
							}
							catch (Throwable e) {
								return retVal;
							}
						}
		                return retVal;
					}
					
				});
				config.setSchemaValidation(true);
				config.setURL(tmp.toUri().toURL());
				config.load();
				if (cmdLine.hasOption("listen"))
					System.out.println("IGNORING 'listen' option because a configuration file was supplied.");
				if (cmdLine.hasOption("target"))
					System.out.println("IGNORING 'target' option(s) because a configuration file was supplied.");
				if (cmdLine.hasOption("scripts"))
					System.out.println("IGNORING 'scripts' option because a configuration file was supplied.");
				if (cmdLine.hasOption("library"))
					System.out.println("IGNORING 'library' option(s) because a configuration file was supplied.");
			}
			else {
				String[] serverStrs;
				String[] addr = {null};
				String[] port = {null};
				serverStrs = cmdLine.getOptionValues("listen");
				if (serverStrs == null)
					throw new MissingOptionException("No listening addresses specified specified");
				for (int i=0; i<serverStrs.length; i++) {
					String addrStr;
					String alias = null;
					String protocol = null;
					Boolean https = null;
					int addrPos = serverStrs[i].indexOf('=');
					if (addrPos >= 0) {
						if (addrPos < 2)
							throw new IllegalArgumentException("Invalid http argument.");
						else if (addrPos + 1 >= serverStrs[i].length())
							throw new IllegalArgumentException("Invalid http argument.");
						addrStr = serverStrs[i].substring(addrPos+1, serverStrs[i].length());
						String[] types = serverStrs[i].substring(0, addrPos).split(",");
						for (String type : types) {
							if (type.equalsIgnoreCase("http"))
								break;
							else if (type.equalsIgnoreCase("https") || type.equalsIgnoreCase("secure"))
								https = true;
							else if (type.equalsIgnoreCase("tls") || type.equalsIgnoreCase("ssl"))
								protocol = type.toUpperCase();
							else
								alias = type;
						}
					}
					else
						addrStr = serverStrs[i];
					ParseAddressString(addrStr, addr, port, alias != null ? 443 : 80);
					config.addProperty("server.listen(" + i + ")[@address]", addr[0]);
					config.addProperty("server.listen(" + i + ")[@port]", port[0]);
					if (alias != null)
						config.addProperty("server.listen(" + i + ")[@alias]", alias);
					if (protocol != null)
						config.addProperty("server.listen(" + i + ")[@protocol]", protocol);
					if (https != null)
						config.addProperty("server.listen(" + i + ")[@secure]", https);
				}
				String servercpu = cmdLine.getOptionValue("servercpu");
				if (servercpu != null)
					config.setProperty("server[@cpu]", servercpu);
				String clientcpu = cmdLine.getOptionValue("targetcpu");
				if (clientcpu != null)
					config.setProperty("targets[@cpu]", clientcpu);

				// Configure static files
				if (cmdLine.hasOption("files")) {
					Path tmp = Utils.MakePath(cmdLine.getOptionValue("files"));
					if (!Files.exists(tmp))
						throw new FileNotFoundException("Files directory does not exist.");
					if (!Files.isDirectory(tmp))
						throw new FileNotFoundException("'files' path is not a directory.");
					config.setProperty("files.rootDirectory", tmp.toAbsolutePath().toUri());
				}
				
				// Configure scripting
				if (cmdLine.hasOption("scripts")) {
					Path tmp = Utils.MakePath(cmdLine.getOptionValue("scripts"));
					if (!Files.exists(tmp))
						throw new FileNotFoundException("Scripts directory does not exist.");
					if (!Files.isDirectory(tmp))
						throw new FileNotFoundException("'scripts' path is not a directory.");
					config.setProperty("scripts.rootDirectory", tmp.toAbsolutePath().toUri());
					config.setProperty("scripts.dynamicWatch", cmdLine.hasOption("watch"));
					String[] libraries = cmdLine.getOptionValues("library");
					if (libraries != null) {
						for (int i=0; i<libraries.length; i++) {
							Path lib = Utils.MakePath(libraries[i]);
							if (!Files.exists(lib))
								throw new FileNotFoundException("Script library does not exist [" + libraries[i] + "].");
							if (Files.isDirectory(lib))
								throw new FileNotFoundException("Script library is not a file [" + libraries[i] + "].");
							config.setProperty("scripts.library(" + i + ")", lib.toAbsolutePath().toUri());
						}
					}
				}
				else if (cmdLine.hasOption("watch"))
					System.out.println("IGNORING 'watch' option as no 'scripts' directory was specified.");
				else if (cmdLine.hasOption("library"))
					System.out.println("IGNORING 'library' option as no 'scripts' directory was specified.");
			}
			String keyStorePath = cmdLine.getOptionValue("keystore");			
			if (keyStorePath != null)
				config.setProperty("keystore", keyStorePath);
			String keypass = cmdLine.getOptionValue("keypass");
			if (keypass != null)
				config.setProperty("keypass", keypass);
			String storepass = cmdLine.getOptionValue("storepass");
			if (storepass != null)
				config.setProperty("storepass", keypass);
			if (cmdLine.hasOption("trustany"))
				config.setProperty("targets[@trustAny]", true);
			
			config.setProperty("scripts.dynamicTargetScripting", cmdLine.hasOption("dynamicTargetScripting"));

			String[] targetStrs = cmdLine.getOptionValues("target");
			if (targetStrs != null) {
				for (int i=0; i<targetStrs.length; i++) {
					int uriPos = targetStrs[i].indexOf('=');
					if (uriPos < 2)
						throw new IllegalArgumentException("Invalid target argument.");
					else if (uriPos + 1 >= targetStrs[i].length())
						throw new IllegalArgumentException("Invalid target argument.");
					String patternStr = targetStrs[i].substring(0, uriPos);
					String urlStr = targetStrs[i].substring(uriPos+1, targetStrs[i].length());
					String alias;
					try {
						URL url = new URL(urlStr);
						alias = url.getUserInfo();
						String scheme = url.getProtocol();
						if ((! "http".equals(scheme)) && (! "https".equals(scheme)))
							throw new IllegalArgumentException("Invalid target uri scheme.");
						int port = url.getPort();
						if (port < 0)
							port = url.getDefaultPort();
						urlStr = scheme + "://" + url.getHost() + ":" + port + url.getPath();
						String ref = url.getRef();
						if (ref != null)
							urlStr += "#" + ref;
					}
					catch(MalformedURLException ex) {
						throw new IllegalArgumentException("Malformed target uri");
					}
					config.addProperty("targets.target(" + i + ")[@pattern]", patternStr);
					config.addProperty("targets.target(" + i + ")[@url]", urlStr);
					if (alias != null)
						config.addProperty("targets.target(" + i + ")[@alias]", alias);
				}
			}
//			config.save(System.out);
		}
		catch (Throwable e) {
			e.printStackTrace(System.err);
			return;
		}
		// If we get here, we have a possibly valid configuration.
		try {
			final PokerFace p = new PokerFace();
			p.config(config);
			if (p.start()) {
				PokerFace.Logger.warn("Started!");
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							PokerFace.Logger.warn("Initiating shutdown...");
							p.stop();
							PokerFace.Logger.warn("Shutdown completed!");
						}
						catch (Throwable e) {
							PokerFace.Logger.error("Failed to shutdown cleanly!");
							e.printStackTrace(System.err);
						}
					}
				});
			}
			else {
				PokerFace.Logger.error("Failed to start!");
				System.exit(-1);
			}
		}
		catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Break apart a string into a well known address form passing back the pieces.
	 */
	private static void ParseAddressString(String addrStr, String[] hostName, String[] port, int defaultPort) {
		try {
			if ((addrStr == null) || addrStr.length() == 0)
				addrStr = "127.0.0.1:8080";
			addrStr = addrStr.replace('/', ':');
			URL url = new URL(("http://") + addrStr);
			if ((url.getHost() == null) && (url.getPort() == -1))
				throw new IllegalArgumentException("Invalid http(s) address specified.");
			if ((url.getHost() == null) || (url.getHost().length() == 0))
				hostName[0] = "0.0.0.0";
			else
				hostName[0] = url.getHost();
			if (url.getPort() == -1)
				port[0] = "" + defaultPort;
			else
				port[0] = "" + url.getPort();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Invalid http(s) address specified.");
		}
	}

	private static float JavaVersionAsFloat() {
		final String[] toParse = System.getProperty("java.version")
				.split("\\.");
		if (toParse.length >= 2) {
			try {
				return Float.parseFloat(toParse[0] + '.' + toParse[1]);
			} catch (final NumberFormatException nfe) {
				// just fall through
			}
		}
		return -1;
	}
}
