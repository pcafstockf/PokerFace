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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * This class is a GOF Bridge pattern to decouple file watching from the underlying technology used to 'watch' a given directory for changes.
 * Could use something like JNotify, but currently uses JDK 7.
 */
@SuppressWarnings("WeakerAccess")
public class DirectoryWatchService {

	/**
	 * Initialize the watching technology.
	 */
	public DirectoryWatchService() throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<>();
		this.listeners = new ConcurrentHashMap<>();
		Thread t = new Thread("DirectoryWatcher") {
			public void run() {
				processEvents();
			}
		};
		t.setDaemon(true);
		t.start();
	}

	protected final WatchService watcher;
	protected final Map<WatchKey, Path> keys;
	protected final Map<Path, DirectoryWatchEventListener> listeners;

	/**
	 * Register a watch on the specified directory/file
	 *
	 * @return An opaque token needed to cancel this 'watch'
	 */
	@SuppressWarnings("UnusedReturnValue")
	public Object establishWatch(Path watch, DirectoryWatchEventListener listener) throws IOException {
		Path p = watch.toAbsolutePath();
		listeners.put(p, listener);
		Object key;
		if (Files.isDirectory(p))
			key = registerAll(p);
		else {
			key = p;
			Path par = p.getParent().getParent();
			boolean found = false;
			for (Path path : keys.values())
				if (path.compareTo(par) == 0) {
					found = true;
					break;
				}
			if (!found)
				register(par);
		}
		return key;
	}

	/**
	 * Cancel a previously established watch
	 *
	 * @param token Value returned from {@code establishWatch}
	 * @return True if the watch existed and was successfully canceled.
	 */
	@SuppressWarnings("unused")
	public boolean cancelWatch(Object token) {
		if (token instanceof Path) {
			listeners.remove(token);
			return true;
		}
		else if (token instanceof File) {
			Path p = Paths.get(((File) token).getAbsolutePath());
			listeners.remove(p);
			return true;
		}
		else if (token instanceof WatchKey) {
			((WatchKey) token).cancel();
			listeners.remove(keys.remove(token));
			return true;
		}
		return false;
	}

	// Useful utitlity
	@SuppressWarnings("unchecked")
	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private WatchKey register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		keys.put(key, dir);
		return key;
	}

	/**
	 * Register the given directory, and all its sub-directories, with this service.
	 */
	private WatchKey registerAll(final Path start) throws IOException {
		final WatchKey[] retVal = {null};
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				WatchKey key = register(dir);
				if (retVal[0] == null)
					retVal[0] = key;
				return FileVisitResult.CONTINUE;
			}
		});
		return retVal[0];
	}

	/**
	 * Process all events for keys we have queued to the watcher.
	 * I copies this code from a sample somewhere, but I honestly don't remember.  Maybe Oracle ?
	 */
	private void processEvents() {
		while (true) {
			WatchKey key;
			try {
				key = watcher.take(); // wait for key to be signaled
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null)
				continue;

			for (WatchEvent<?> event : key.pollEvents()) {
				@SuppressWarnings("rawtypes") WatchEvent.Kind kind = event.kind();
				if (kind == OVERFLOW) // ? How should OVERFLOW event be handled
					continue;
				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);

				DirectoryWatchEventListener listener = null;
				Path root = null;
				for (Entry<Path, DirectoryWatchEventListener> e : listeners.entrySet()) {
					Path p = e.getKey();
					if (child.startsWith(p) || (child.compareTo(p) == 0)) {
						listener = e.getValue();
						root = p;
						break;
					}
				}
				if (listener != null) {
					if (kind == ENTRY_MODIFY) {
						if (child.getFileName().toString().equals(".DS_Store"))
							return;
						listener.onWatchEvent(root, null, child, DirectoryWatchEventListener.FileChangeType.eModified);
					}
					else if (kind == ENTRY_CREATE) {
						listener.onWatchEvent(root, null, child, DirectoryWatchEventListener.FileChangeType.eCreated);
					}
					else if (kind == ENTRY_DELETE) {
						listener.onWatchEvent(root, null, child, DirectoryWatchEventListener.FileChangeType.eDeleted);
					}
				}

				// if directory is created, and watching recursively, then register it and its sub-directories
				if (kind == ENTRY_CREATE) {
					try {
						if (Files.isDirectory(child, NOFOLLOW_LINKS))
							registerAll(child);
					} catch (IOException x) {
						// ignore to keep sample readable
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				Path p = keys.remove(key);
				listeners.remove(p);
				// all directories are inaccessible
				if (keys.isEmpty())
					break;
			}
		}
	}
}
