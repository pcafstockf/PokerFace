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

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.*;

import javax.script.*;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * This is not really a unit test and does not need to be run prior to checking.  It's primary purpose is as a POC WRT how the Nashorn engine can safely be used.
 */
@SuppressWarnings("restriction")
public class NashornCompiledScriptTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		engineFactory = new NashornScriptEngineFactory();
		engine = engineFactory.getScriptEngine();
		@SuppressWarnings("StringBufferReplaceableByString") String script = new StringBuilder().append("(function() {").append("	var i = 0;").append("	return {").append("		setI: function(val) {").append("			i = val;").append("		},").append("		addTwice: function(amount) {").append("			var x = amount;").append("			i += amount;").append("			var shortly_later = new Date()/1000 + Math.random;").append("			while( (new Date()/1000) < shortly_later) { Math.random() };").append("			i += x;").append("		},").append("		getI: function() {").append("			return i;").append("		},").append("		square: function(val) {").append("			var x = val;").append("			var shortly_later = new Date()/1000 + Math.random;").append("			while( (new Date()/1000) < shortly_later) { Math.random() };").append("			return (val * x);").append("		},").append("	};").append("})();").toString();
		compiledScript = ((Compilable) engine).compile(script);
	}

	private static NashornScriptEngineFactory engineFactory;
	private static ScriptEngine engine;
	private static CompiledScript compiledScript;

	@Before
	public void setUp() {
	}

	@Test
	public void testSingleThread() throws ScriptException {
		ScriptObjectMirror obj = (ScriptObjectMirror) compiledScript.eval();
		obj.callMember("setI", 3);
		Number result = (Number) obj.callMember("getI");
		Assert.assertEquals("Getter/Setter works", 3, result.intValue());
		obj.callMember("addTwice", 2);
		result = (Number) obj.callMember("getI");
		Assert.assertEquals("addTwice works", 7, result.intValue());
	}

	/**
	 * This test answers the question of whether we can:
	 * 1.)  Compile a script using an engine created on the main thread.
	 * 2.)  Eval that script (also on the main thread) to produce an object with immutable methods *and* mutable methods which *modify* internal object state/properties encapsulated/protected within a closure.
	 * 3.)  Safely invoke the objects immutable methods from *ANY* thread (often simultaneously).
	 * 4.)  Safely invoke the objects mutable methods from *ANY* thread as long as the object is synchronized (e.g. not invoked *simultaneously* from multiple threads).
	 * 5.)  Safely invoke immutable methods from multiple threads while a synchronized mutable method is being simultaneously executed in another thread.
	 * 6.)  Variables declared within the scope of the objects methods (not object properties), are not affected by executing that same method in other threads simultaneously.
	 */
	@Test
	public void testMultiThread() throws ScriptException, InterruptedException, ExecutionException {
		final ScriptObjectMirror obj = (ScriptObjectMirror) compiledScript.eval();
		obj.callMember("setI", 2);
		final SquareInterface sq = ((Invocable) engine).getInterface(obj, SquareInterface.class);

		Callable<Boolean> testMutableTask = () -> {
			int i = (int) (Math.random() * 10);
			int j = (int) (Math.random() * 10);
			// Validate that if the object is synchronized, it's state may be altered from different threads.
			synchronized (obj) {
				obj.callMember("setI", i);
				Number result = (Number) obj.callMember("getI");
				if (result.intValue() != i)
					return false;
				obj.callMember("addTwice", j);
				result = (Number) obj.callMember("getI");
				if (result.intValue() != (i + (j * 2)))
					return false;
			}
			return true;
		};
		Callable<Boolean> testImutableTask = () -> {
			int i = (int) (Math.random() * 10);
			// Validate that methods which do not alter the objects state may be invoked from multiple threads simultaneously.
			Number result = (Number) obj.callMember("square", i);
			if (result.intValue() != (i * i))
				return false;
			int secondResult = sq.square(i);
			//noinspection RedundantIfStatement
			if (secondResult != (i * i))
				return false;
			return true;
		};

		ExecutorService executor = Executors.newCachedThreadPool();
		ArrayList<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < 500; i++) {
			if (Math.random() > 0.5) {
				results.add(executor.submit(testMutableTask));
				results.add(executor.submit(testImutableTask));
			}
			else {
				results.add(executor.submit(testImutableTask));
				results.add(executor.submit(testMutableTask));
			}
		}
		for (Future<Boolean> result : results) {
			boolean jsResult = result.get();
			Assert.assertTrue("Threads did not interfere", jsResult);
		}
		executor.awaitTermination(1, TimeUnit.SECONDS);
		executor.shutdownNow();
	}

	@After
	public void tearDown() {
	}

	@AfterClass
	public static void tearDownAfterClass() {
		compiledScript = null;
		engine = null;
		engineFactory = null;
	}
}
