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
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.SSLNHttpServerConnectionFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig.Builder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.reactor.ssl.SSLMode;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous, fully streaming, HTTP/1.1, dynamic, scriptable reverse proxy.
 * This class wires together all needed pieces (consulting the supplied configuration data).
 */
@SuppressWarnings("restriction")
public class PokerFace {
	protected static final Logger Logger = LoggerFactory.getLogger(PokerFace.class.getPackage().getName());

	/**
	 * Primary / Default constructor.
	 */
	public PokerFace() {
	}
	
	/**
	 * Configures all the needed components, but does not actually start the server.
	 * @param config	Contains all information needed to fully wire up the http, https, and httpclient components of this reverse proxy.
	 * @throws Exception	Yeah, a lot can go wrong here, but at least it will be caught immediately :-)
	 */
	public void config(HierarchicalConfiguration config) throws Exception {
		List<HierarchicalConfiguration>  lconf;
		HttpAsyncRequester executor = null;
		BasicNIOConnPool connPool = null;
		ObjectPool<ByteBuffer> byteBufferPool = null;
		LinkedHashMap<String, TargetDescriptor> mappings = null;
		ConcurrentMap<String, HttpHost> hosts = null;

		handlerRegistry = new UriHttpAsyncRequestHandlerMapper();
		
		// Initialize the keystore (if one was specified)
		KeyStore keystore = null;
		char[] keypass = null;
		String keystoreUri = config.getString("keystore");
		if ((keystoreUri != null) && (keystoreUri.trim().length() > 0)) {
			Path keystorePath = Utils.MakePath(keystoreUri);
			if (!Files.exists(keystorePath))
				throw new ConfigurationException("Keystore does not exist.");
			if (Files.isDirectory(keystorePath))
				throw new ConfigurationException("Keystore is not a file");
			String storepass = config.getString("storepass");
			if ((storepass != null) && "null".equals(storepass))
				storepass = null;
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (InputStream keyStoreStream = Files.newInputStream(keystorePath)) {
				keystore.load(keyStoreStream, storepass == null ? null : storepass.trim().toCharArray());
			}
			catch (IOException ex) {
				Logger.error("Unable to load https server keystore from " + keystoreUri);
				return;
			}
			keypass = config.getString("keypass").trim().toCharArray();
		}
		
		// Wire up the listening reactor
		lconf = config.configurationsAt("server");
		if ((lconf == null) || (lconf.size() != 1))
			throw new ConfigurationException("One (and only one) server configuration element is allowed.");
		else {
			Builder builder = IOReactorConfig.custom();
			builder.setIoThreadCount(ComputeReactorProcessors(config.getDouble("server[@cpu]", 0.667)));
			builder.setSoTimeout(config.getInt("server[@soTimeout]", 0));
			builder.setSoLinger(config.getInt("server[@soLinger]", -1));
			builder.setSoReuseAddress(true);
			builder.setTcpNoDelay(false);
			builder.setSelectInterval(100);
			
			IOReactorConfig rconfig = builder.build();
			Logger.info("Configuring server with options: " + rconfig.toString());
			listeningReactor = new DefaultListeningIOReactor(rconfig);
			
			lconf = config.configurationsAt("server.listen");
			InetSocketAddress addr;
			boolean hasNonWildcardSecure = false;
			LinkedHashMap<SocketAddress, SSLContext> addrSSLContext = new LinkedHashMap<SocketAddress, SSLContext>();
			if ((lconf == null) || (lconf.size() == 0)) {
				addr = new InetSocketAddress("127.0.0.1", 8080);
				ListenerEndpoint ep = listeningReactor.listen(addr);
				Logger.warn("Configured " + ep.getAddress());
			}
			else {
				TrustManager[] trustManagers = null;
				KeyManagerFactory kmf = null;
				// Create all the specified listeners.
				for (HierarchicalConfiguration hc : lconf) {
					String addrStr = hc.getString("[@address]");
					if ((addrStr == null) || (addrStr.length() == 0))
						addrStr = "0.0.0.0";
					String alias = hc.getString("[@alias]");
					int port = hc.getInt("[@port]", alias != null ? 443 : 80);
					addr = new InetSocketAddress(addrStr, port);
					ListenerEndpoint ep = listeningReactor.listen(addr);
					String protocol = hc.containsKey("[@protocol]") ? hc.getString("[@protocol]") : null;
					Boolean secure = hc.containsKey("[@secure]") ? hc.getBoolean("[@secure]") : null;
					if ((alias != null) && (secure == null))
						secure = true;
					if ((protocol != null) && (secure == null))
						secure = true;
					if ((secure != null) && secure) {
						if (protocol == null)
							protocol = "TLS";
						if (keystore == null)
							throw new ConfigurationException("An https listening socket was requested, but no keystore was specified.");
						if (kmf == null) {
							kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
							kmf.init(keystore, keypass);
						}
						// Are we going to trust all clients or just specific ones?
						if (hc.getBoolean("[@trustAny]", true))
							trustManagers = new TrustManager[] { new X509TrustAllManager() };
						else {
							TrustManagerFactory instance = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
							instance.init(keystore);
							trustManagers = instance.getTrustManagers();
						}
						KeyManager[] keyManagers = kmf.getKeyManagers();
						if (alias != null)
							for (int i=0; i<keyManagers.length; i++) {
								if (keyManagers[i] instanceof X509ExtendedKeyManager)
									keyManagers[i] = new PokerFaceKeyManager(alias, (X509ExtendedKeyManager)keyManagers[i]);
							}
						SSLContext sslCtx = SSLContext.getInstance(protocol);
						sslCtx.init(keyManagers, trustManagers, new SecureRandom());
						if (addr.getAddress().isAnyLocalAddress()) {
							// This little optimization helps us respond faster for every connection as we don't have to extrapolate a local connection address to wild card.
							for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
								NetworkInterface intf = en.nextElement();
								for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
									addr = new InetSocketAddress(enumIpAddr.nextElement(), port);
									addrSSLContext.put(addr, sslCtx);
								}
							}
						}
						else {
							addrSSLContext.put(addr, sslCtx);
							hasNonWildcardSecure = true;
						}
					}
					Logger.warn("Configured " + (alias == null ? "" : (protocol + " on")) + ep.getAddress());
				}
			}
			// We will need an HTTP protocol processor for the incoming connections
			String serverAgent = config.getString("server.serverAgent", "PokerFace/" + Utils.Version);
			HttpProcessor inhttpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] { new ResponseDateInterceptor(), new ResponseServer(serverAgent), new ResponseContent(), new ResponseConnControl() });
			HttpAsyncService serviceHandler = new HttpAsyncService(inhttpproc, new DefaultConnectionReuseStrategy(), null, handlerRegistry, null) {
			    public void exception( final NHttpServerConnection conn, final Exception cause) {
			    	Logger.warn(cause.getMessage());
			    	super.exception(conn, cause);
			    }
			};
			if (addrSSLContext.size() > 0) {
				final SSLContext defaultCtx = addrSSLContext.values().iterator().next();
				final Map<SocketAddress, SSLContext> sslMap;
				if ((! hasNonWildcardSecure) || (addrSSLContext.size() == 1))
					sslMap = null;
				else
					sslMap = addrSSLContext;
				listeningDispatcher = new DefaultHttpServerIODispatch(serviceHandler, new SSLNHttpServerConnectionFactory(defaultCtx, null, ConnectionConfig.DEFAULT) {
					protected SSLIOSession createSSLIOSession(IOSession iosession, SSLContext sslcontext, SSLSetupHandler sslHandler) {
						SSLIOSession retVal;
						SSLContext sktCtx = sslcontext;
						if (sslMap != null) {
							SocketAddress la = iosession.getLocalAddress();
							if (la != null) {
								sktCtx = sslMap.get(la);
								if (sktCtx == null)
									sktCtx = sslcontext;
							}
							retVal = new SSLIOSession(iosession, SSLMode.SERVER, sktCtx, sslHandler);
						}
						else
							retVal = super.createSSLIOSession(iosession, sktCtx, sslHandler);
						if (sktCtx != null)
							retVal.setAttribute("com.bytelightning.opensource.pokerface.secure", true);
						return retVal;
					}
				});
			}
			else
				listeningDispatcher = new DefaultHttpServerIODispatch(serviceHandler, ConnectionConfig.DEFAULT);
		}
	
		// Configure the httpclient reactor that will be used to do reverse proxing to the specified targets.
		lconf = config.configurationsAt("targets");
		if ((lconf != null) && (lconf.size() > 0)) {
			HierarchicalConfiguration conf = lconf.get(0);
			Builder builder = IOReactorConfig.custom();
			builder.setIoThreadCount(ComputeReactorProcessors(config.getDouble("targets[@cpu]", 0.667)));
			builder.setSoTimeout(conf.getInt("targets[@soTimeout]", 0));
			builder.setSoLinger(config.getInt("targets[@soLinger]", -1));
			builder.setConnectTimeout(conf.getInt("targets[@connectTimeout]", 0));
			builder.setSoReuseAddress(true);
			builder.setTcpNoDelay(false);
			connectingReactor = new DefaultConnectingIOReactor(builder.build());

			final int bufferSize = conf.getInt("targets[@bufferSize]", 1024) * 1024;
			byteBufferPool = new SoftReferenceObjectPool<ByteBuffer>(new BasePooledObjectFactory<ByteBuffer>() {
				@Override
				public ByteBuffer create() throws Exception {
					return ByteBuffer.allocateDirect(bufferSize);
				}
				@Override
				public PooledObject<ByteBuffer> wrap(ByteBuffer buffer) {
					return new DefaultPooledObject<ByteBuffer>(buffer);
				}
			});
			
			KeyManager[] keyManagers = null;
			TrustManager[] trustManagers = null;
			
			if (keystore != null) {
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keystore, keypass);
				keyManagers = kmf.getKeyManagers();
			}
			// Will the httpclient's trust any remote target, or only specific ones.
			if (conf.getBoolean("targets[@trustAny]", false))
				trustManagers = new TrustManager[] {new X509TrustAllManager()};
			else if (keystore != null) {
		        TrustManagerFactory instance = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		        instance.init(keystore);
		        trustManagers = instance.getTrustManagers();
			}
			SSLContext clientSSLContext = SSLContext.getInstance(conf.getString("targets[@protocol]", "TLS"));
			clientSSLContext.init(keyManagers, trustManagers, new SecureRandom());
			
			// Setup an SSL capable connection pool for the httpclients.
			connPool = new BasicNIOConnPool(connectingReactor, new BasicNIOConnFactory(clientSSLContext, null, ConnectionConfig.DEFAULT), conf.getInt("targets[@connectTimeout]", 0));
			connPool.setMaxTotal(conf.getInt("targets[@connMaxTotal]", 1023));
			connPool.setDefaultMaxPerRoute(conf.getInt("targets[@connMaxPerRoute]", 1023));

			// Set up HTTP protocol processor for outgoing connections
			String userAgent = conf.getString("targets.userAgent", "PokerFace/" + Utils.Version);
			HttpProcessor outhttpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] { new RequestContent(), new RequestTargetHost(), new RequestConnControl(), new RequestUserAgent(userAgent), new RequestExpectContinue(true) });
			executor = new HttpAsyncRequester(outhttpproc, new DefaultConnectionReuseStrategy());

			// Now set up all the configured targets.
			mappings = new LinkedHashMap<String, TargetDescriptor>();
			hosts = new ConcurrentHashMap<String, HttpHost>();
			String[] scheme = { null };
			String[] host = { null };
			int[] port = { 0 };
			String[] path = { null };
			int[] stripPrefixCount = { 0 };
			for (HierarchicalConfiguration targetConfig : conf.configurationsAt("target")) {
				String match = targetConfig.getString("[@pattern]");
				if ((match == null) || (match.trim().length() < 1)) {
					Logger.error("Unable to configure target;  Invalid url match pattern");
					continue;
				}
				String key = RequestForTargetConsumer.UriToTargetKey(targetConfig.getString("[@url]"), scheme, host, port, path, stripPrefixCount);
				if (key == null) {
					Logger.error("Unable to configure target");
					continue;
				}
				HttpHost targetHost = hosts.get(key);
				if (targetHost == null) {
					targetHost = new HttpHost(host[0], port[0], scheme[0]);
					hosts.put(key, targetHost);
				}
				TargetDescriptor desc = new TargetDescriptor(targetHost, path[0], stripPrefixCount[0]);
				mappings.put(match, desc);
			}
			connectionDispatcher = new DefaultHttpClientIODispatch(new HttpAsyncRequestExecutor(), ConnectionConfig.DEFAULT);
		}
		// Allocate the script map which will be populated by it's own executor thread.
		if (config.containsKey("scripts.rootDirectory")) {
			Path tmp = Utils.MakePath(config.getProperty("scripts.rootDirectory"));
			if (!Files.exists(tmp))
				throw new FileNotFoundException("Scripts directory does not exist.");
			if (!Files.isDirectory(tmp))
				throw new FileNotFoundException("'scripts' path is not a directory.");
			scripts = new ConcurrentSkipListMap<String, ScriptObjectMirror>();
			boolean watch = config.getBoolean("scripts.dynamicWatch", false);
			List<Path> jsLibs;
			Object prop = config.getProperty("scripts.library");
			if (prop != null) {
				jsLibs = new ArrayList<Path>();
				if (prop instanceof Collection<?>) {
					 @SuppressWarnings("unchecked")
					Collection<Object> oprop = (Collection<Object>)prop;
					for (Object obj :oprop)
						jsLibs.add(Utils.MakePath(obj));
				}
				else {
					jsLibs.add(Utils.MakePath(prop));
				}
			}
			else
				jsLibs = null;
			
			lconf = config.configurationsAt("scripts.scriptConfig");
			if (lconf != null) {
				if (lconf.size() > 1)
					throw new ConfigurationException("Only one scriptConfig element is allowed.");
				if (lconf.size() == 0)
					lconf = null;
			}
			
			HierarchicalConfiguration scriptConfig;
			if (lconf == null)
				scriptConfig = new HierarchicalConfiguration();
			else
				scriptConfig = lconf.get(0);
			scriptConfig.setProperty("pokerface.scripts.rootDirectory", tmp.toString());
			
			configureScripts(jsLibs, scriptConfig, tmp, watch);
			if (watch)
				ScriptDirectoryWatcher = new DirectoryWatchService();
		}

		// Configure the static file directory (if any)
		Path staticFilesPath = null;
		if (config.containsKey("files.rootDirectory")) {
			Path tmp = Utils.MakePath(config.getProperty("files.rootDirectory"));
			if (!Files.exists(tmp))
				throw new FileNotFoundException("Files directory does not exist.");
			if (!Files.isDirectory(tmp))
				throw new FileNotFoundException("'files' path is not a directory.");
			staticFilesPath = tmp;
			List<HierarchicalConfiguration> mimeEntries =  config.configurationsAt("files.mime-entry");
			if (mimeEntries != null) {
				for (HierarchicalConfiguration entry : mimeEntries) {
					entry.setDelimiterParsingDisabled(true);
					String type = entry.getString("[@type]", "").trim();
					if (type.length() == 0)
						throw new ConfigurationException("Invalid mime type entry");
					String extensions = entry.getString("[@extensions]", "").trim();
					if (extensions.length() == 0)
						throw new ConfigurationException("Invalid mime extensions for: " + type);
					ScriptHelperImpl.AddMimeEntry(type, extensions);
				}
			}
		}
		
		handlerRegistry.register("/*", new RequestHandler(executor, connPool, byteBufferPool, staticFilesPath, mappings, scripts != null ? Collections.unmodifiableNavigableMap(scripts) : null, config.getBoolean("scripts.allowScriptsToSpecifyDynamicHosts", false) ? hosts : null));
	}
	protected ConnectingIOReactor connectingReactor;
	protected IOEventDispatch connectionDispatcher;
	protected ListeningIOReactor listeningReactor;
	protected IOEventDispatch listeningDispatcher;
	protected UriHttpAsyncRequestHandlerMapper handlerRegistry;
	protected NavigableMap<String, ScriptObjectMirror> scripts;

	/**
	 * Utility method that allows us to be very flexible in how many reactor processors are allocated to a reactor.
	 * @param num	If not a finite number, returns 1.
	 * 				If <= 0, returns the actual number of Runtime.getRuntime().availableProcessors().
	 * 				If >= 1, returns the minimum of <code>num</code> or (Runtime.getRuntime().availableProcessors(). * 2)
	 * 				If > 0 && < 1, returns (num * Runtime.getRuntime().availableProcessors()).
	 * @return	The translated number of IOProcessors for a reactor.
	 */
	private static int ComputeReactorProcessors(double num) {
		if (!Double.isFinite(num))
			num = 1d;
		if (num <= 0)
			return Runtime.getRuntime().availableProcessors();
		if (num >= (1d - Double.MIN_VALUE))
			return Math.min((int) Math.rint(num), (Runtime.getRuntime().availableProcessors() * 2));
		return Math.max(1, (int) Math.rint(Runtime.getRuntime().availableProcessors() * num));
	}

	/**
	 * If requested by the user, this method walks the script directory discovering, loading, compiling, and initialing an .js javascript files it finds in the specified directory or it's children.
	 * @param baseScriptDirectory	The contents of this directory should be structured in the same layout as the url's we wish to interfere with.
	 * @param watchScriptDirectory	If true, a watch will be placed on <code>baseScriptDirectory</code> and any javascript file modifications (cud) will be dynamically rebuilt and reflected in the running server. 
	 * @return	True if all scripts were successfully loaded.
	 */
	protected boolean configureScripts(final List<Path> jsLibs, final HierarchicalConfiguration scriptConfig, final Path baseScriptDirectory, boolean watchScriptDirectory) {
		// Our unit test has verified that CompiledScripts can produce objects (endpoints) that can be executed from ANY thread (and even concurrently execute immutable methods).
		// However we have not validated that Nashorn can compile *and* recompile scripts from multiple threads.
//TODO: Write unit test to see if we can use all available processors to compile discovered javascript files.
		ScriptCompilationExecutor = Executors.newSingleThreadScheduledExecutor();
		// This is done to make sure the engine is allocated in the same thread that will be doing the compiling.
		Callable<Boolean> compileScriptsTask = new Callable<Boolean>() {
			@Override
			public Boolean call() {
				Nashorn = new ScriptEngineManager().getEngineByName("nashorn");
				
				if (jsLibs != null)
					for (Path lib : jsLibs)
						if (! loadScriptLibrary(lib))
							return false;

				// Recursively discover javascript files, compile, load, and setup any that are found.
				EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
				try {
					Files.walkFileTree(baseScriptDirectory, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
					    @Override
					    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							if (Files.isDirectory(dir) && dir.getFileName().toString().startsWith("#"))
								return FileVisitResult.SKIP_SUBTREE;
							return super.preVisitDirectory(dir, attrs);
					    }
					    @Override
						public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
							if (Files.isRegularFile(path)) {
								if (path.toString().toLowerCase().endsWith(".js")) {
									MakeJavaScriptEndPointDescriptor(baseScriptDirectory, path, scriptConfig, new NewEndpointSetupCallback());
								}
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
				catch (IOException e) {
					Logger.error("Unable recursively load scripts", e);
					return false;
				}
				return true;
			}
		};
		// Walk the root directory recursively compiling all discovered javascript files (does not return until all endpoint files have been setup).
		try {
			if (! ScriptCompilationExecutor.submit(compileScriptsTask).get())
				return false;
		}
		catch (Throwable e) {
			Logger.error("Unable to compile scripts", e);
			return false;
		}
		if (watchScriptDirectory) {
			try {
				// Establish a watch on the root
				ScriptDirectoryWatcher.establishWatch(baseScriptDirectory, new DirectoryWatchEventListener() {
					// Internal Callable task to load, compile, and initialize a javascript file endpoint.
					final class CreateEndpointTask implements Callable<Void> {
						public CreateEndpointTask(Path file, EndpointSetupCompleteCallback callback) {
							this.file = file;
							this.callback = callback;
						}
						private final Path file;
						private final EndpointSetupCompleteCallback callback;

						@Override
						public Void call() {
							MakeJavaScriptEndPointDescriptor(baseScriptDirectory, file, scriptConfig, callback);
							return null;
						}
					}
					// Internal Callable task that gives us the ability to schedule a delayed unload of a deleted or obsoleted endpoint.
					// By delaying for a period of time longer than twice the socket timeout, we can safely call the endpoint's teardown method.
					final class DecommisionEndpointTask implements Callable<Void> {
						private DecommisionEndpointTask(ScriptObjectMirror endpoint) {
							this.endpoint = endpoint;
						}
						private final ScriptObjectMirror endpoint;

						@Override
						public Void call() {
							if (endpoint.hasMember("teardown"))
								endpoint.callMember("teardown");
							return null;
						}
					}

					/**
					 * Called by the WatchService when the contents of the script directory have changed.
					 */
					@Override
					public void onWatchEvent(Path watchDir, final Path oldFile, final Path newFile, FileChangeType change) {
						if (change == FileChangeType.eRenamed) {
							// If it was changed to something that does *not* end .js then it should no longer be considered an endpoint.
							if (oldFile.toString().toLowerCase().endsWith(".js"))
								if (!newFile.toString().toLowerCase().endsWith(".js"))
									change = FileChangeType.eDeleted;
						}
						if (change == FileChangeType.eModified || change == FileChangeType.eRenamed) {
							// Decommission the obsolete and load the update.
							try {
								assert newFile.toString().toLowerCase().endsWith(".js");	// Will be true because of the 'rename' check at the top of this method.
								ScriptCompilationExecutor.submit(new CreateEndpointTask(newFile, new NewEndpointSetupCallback() {
									@Override
									public ScriptObjectMirror setupComplete(JavaScriptEndPoint endpoint) {
										ScriptObjectMirror old = super.setupComplete(endpoint);
										assert old != null;
										// Yeah, it's hincky, but it won't be in use this long after we remove it from the Map.
										ScriptCompilationExecutor.schedule(new DecommisionEndpointTask(old), 6, TimeUnit.MINUTES);
										return null;
									}
								}));
							}
							catch (Throwable e) {
								Logger.error("Unable to compile modified script found at " + newFile.toAbsolutePath().toString(), e);
							}
						}
						else if (change == FileChangeType.eCreated) {
							// This is the easy one.  If a javascript file was created, load it.
							if (newFile.toString().toLowerCase().endsWith(".js")) {
								try {
									ScriptCompilationExecutor.submit(new CreateEndpointTask(newFile, new NewEndpointSetupCallback()));
								}
								catch (Throwable e) {
									Logger.error("Unable to compile new script found at " + newFile.toAbsolutePath().toString(), e);
								}
							}
						}
						else if (change == FileChangeType.eDeleted) {
							// Endpoint should be decommisioned.
							if (oldFile.toString().toLowerCase().endsWith(".js")) {
								String uriKey = FileToUriKey(baseScriptDirectory, oldFile);
								ScriptObjectMirror desc = scripts.remove(uriKey);
								if (desc != null) {
									// Yeah, it's hincky, but it won't be in use this long after we remove it from the Map.
									ScriptCompilationExecutor.schedule(new DecommisionEndpointTask(desc), 6, TimeUnit.MINUTES); 
								}
							}
						}
					}
				});
			}
			catch (IOException e) {
				Logger.error("Unable to establish a real time watch on the script directory.", e);
			}
		}
		else	// Not watching for changes, so we are done with the Executor.
			ScriptCompilationExecutor.shutdown();
		return true;
	}
	
	/**
	 * Load the specified JavaScript library into the global scope of the Nashorn engine
	 * @param lib	Path to the JavaScript library.
	 */
	protected boolean loadScriptLibrary(Path lib) {
		try (Reader r = Files.newBufferedReader(lib, Charset.forName("utf-8"))) {
			Nashorn.eval(r, Nashorn.getBindings(ScriptContext.GLOBAL_SCOPE));
		}
		catch (Exception e) {
			Logger.error("Unable to load JavaScript library " + lib.toAbsolutePath().toString(), e);
			return false;
		}
		return true;
	}
	
	private static ScriptEngine Nashorn;	// The key to our javasciptability.
	private static ScheduledExecutorService ScriptCompilationExecutor;
	private DirectoryWatchService ScriptDirectoryWatcher;

	// Internal class to keep track of the uriKey associated with a loaded ScriptObjectMirror.
	private static final class JavaScriptEndPoint {
		public JavaScriptEndPoint(String uriKey, ScriptObjectMirror som) {
			this.uriKey = uriKey;
			this.som = som;
		}
		private final String uriKey;
		private final ScriptObjectMirror som;
	}
	// Wrapped callback from each script endpoint's 'setup' method.
	private static interface EndpointSetupCompleteCallback {
		public ScriptObjectMirror setupComplete(JavaScriptEndPoint endpoint);
		public void setupFailed(Path path, String msg, Throwable err);
	}
	// Internal boilerplate to handle script endpoint setup
	private class NewEndpointSetupCallback implements EndpointSetupCompleteCallback {
		@Override
		public ScriptObjectMirror setupComplete(JavaScriptEndPoint endpoint) {
			assert (endpoint != null);
			return scripts.put(endpoint.uriKey, endpoint.som);
		}
		@Override
		@edu.umd.cs.findbugs.annotations.SuppressWarnings(value="DM_EXIT", justification="A script that cannot complete it's setup means the proxy can't be used")
		public void setupFailed(Path path, String msg, Throwable err) {
			String desc = "Script was unable to complete setup [" + path.toString() + "]: " + msg;
			if (err != null)
				Logger.error(desc, err);
			else
				Logger.error(desc);
			System.exit(-100);
		}
	}

	/**
	 * This is where Nashorn compiles the script, evals it into global scope to get an endpoint, and invokes the setup method of the endpoint.
	 * @param rootPath	The root script directory path to assist in building a relative uri type path to discovered scripts.
	 * @param f	The javascript file.
	 * @param uriKey	A "pass-back-by-reference" construct to w
	 * @return
	 */
	private static void MakeJavaScriptEndPointDescriptor(Path rootPath, Path f, HierarchicalConfiguration scriptConfig, EndpointSetupCompleteCallback cb) {
		CompiledScript compiledScript;
		try (Reader r = Files.newBufferedReader(f, Charset.forName("utf-8"))) {
			compiledScript = ((Compilable) Nashorn).compile(r);
		}
		catch (Throwable e) {
			cb.setupFailed(f, "Unable to load and compile script at " + f.toAbsolutePath().toString(), e);
			return;
		}
		ScriptObjectMirror obj;
		try {
			obj = (ScriptObjectMirror) compiledScript.eval(Nashorn.getBindings(ScriptContext.GLOBAL_SCOPE));
		}
		catch (Throwable e) {
			cb.setupFailed(f, "Unable to eval the script at " + f.toAbsolutePath().toString(), e);
			return;
		}
		assert f.startsWith(rootPath);
		String uriKey = FileToUriKey(rootPath, f);
		final JavaScriptEndPoint retVal = new JavaScriptEndPoint(uriKey, obj);
		
		try {
			if (obj.hasMember("setup")) {
				obj.callMember("setup", uriKey, scriptConfig, ScriptHelper.ScriptLogger, new SetupCompleteCallback() {
					@Override
					public void setupComplete() {
						cb.setupComplete(retVal);
					}
					@Override
					public void setupFailed(String msg) {
						cb.setupFailed(f, msg, null);
					}
				});
			}
			else {
				cb.setupComplete(retVal);
			}
		}
		catch (Throwable e) {
			cb.setupFailed(f, "The script at " + f.toAbsolutePath().toString() + " did not expose the expected 'setup' method", e);
			return;
		}
	}

	/**
	 * Utility method to covert a javascript <code>File</code> reference into a lowercased uri type path relative to the script directory root.
	 */
	private static String FileToUriKey(Path rootPath, Path f) {
		assert f.toAbsolutePath().startsWith(rootPath);
		String uriKey = rootPath.toAbsolutePath().relativize(f.toAbsolutePath()).toString().toLowerCase();
		assert uriKey.endsWith(".js");
		boolean isForDirectory = uriKey.endsWith("?.js");
		uriKey = "/" + uriKey.substring(0, uriKey.length() - (isForDirectory ? 4 : 3));
		if (isForDirectory && (! uriKey.endsWith("/")))
			uriKey = uriKey + "/";
		return uriKey;
	}

	/**
	 * All user definable configuration has occurred, fire up the server.
	 */
	public boolean start() {
		boolean retVal = false;
		
		// No need to fire up a connecting reactor if there was no listening reactor.
		if ((connectingReactor != null) && (listeningReactor != null)) {
			// NIO based Reactors run in their own threads.
			Thread connectReactorThread = new Thread("connecting-reactor") {
				public void run() {
					try {
						connectingReactor.execute(connectionDispatcher);
					}
					catch (InterruptedIOException ex) {
						Logger.error("Interrupted", ex);
					}
					catch (IOException ex) {
						Logger.error("Incoming connection error", ex);
					}
					finally {
						connectingReactor = null;
						connectionDispatcher = null;
						// If this reactor shuts down for some reason, ensure the other reactors shut down as well.
						try {
							if (listeningReactor != null)
								listeningReactor.shutdown();
						}
						catch (IOException ex2) {
							Logger.error("Unable to shutdown", ex2);
						}
					}
				}
			};
			connectReactorThread.start();
		}

		// Fire up listening reactor.
		if (listeningReactor != null) {
			// NIO based Reactors run in their own threads.
			Thread secureListenReactorThread = new Thread("listening-reactor") {
				public void run() {
					try {
						listeningReactor.execute(listeningDispatcher);
					}
					catch (InterruptedIOException ex) {
						Logger.error("Interrupted", ex);
					}
					catch (IOException ex) {
						Logger.error("Incoming connection error", ex);
					}
					finally {
						listeningReactor = null;
						listeningDispatcher = null;
						// If this reactor shuts down for some reason, ensure the other reactors shut down as well.
						try {
							if (connectingReactor != null)
								connectingReactor.shutdown();
						}
						catch (IOException ex2) {
							Logger.error("Unable to shutdown", ex2);
						}
					}
				}
			};
			secureListenReactorThread.start();
			retVal = true;
		}
		return retVal;
	}

	/**
	 * Shut it all down.
	 * @throws IOException 
	 */
	public void stop() throws IOException {
		if (listeningReactor != null)
			listeningReactor.shutdown();
	}
}
