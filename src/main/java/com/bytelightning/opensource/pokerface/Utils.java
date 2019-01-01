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

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Useful collection of Http related utility methods.
 */
@SuppressWarnings("WeakerAccess")
public class Utils {
	public static final String Version;

	static {
		Properties pomProps = new Properties();
		try (Reader r = new InputStreamReader(Utils.class.getResourceAsStream("/META-INF/maven/com.bytelightning.opensource.pokerface/PokerFace/pom.properties"), Charset.forName("utf-8"))) {
			pomProps.load(r);
		} catch (Throwable e) {
			pomProps.put("version", "?");
		}
		Version = pomProps.getProperty("version");
	}

	/**
	 * Converts a string to a {@code Path} by checking to see if it is a URI for any of the known providers, or just a plain {@code File} path.
	 * WARNING! This is not an efficient routine and should NOT be called in performance critical code.
	 */
	public static Path MakePath(Object path) {
		if (path == null)
			return null;
		if (path instanceof Path)
			return (Path) path;
		String s = path.toString();
		if (s.length() == 0)
			return null;
		URI uri;
		Path retVal;
		for (FileSystemProvider p : FileSystemProvider.installedProviders()) {
			if (s.toLowerCase().startsWith(p.getScheme().toLowerCase() + ":/")) {
				try {
					uri = new URI(s);
					retVal = p.getPath(uri);
					if (retVal != null)
						return retVal;
				} catch (URISyntaxException e) {
					// continue on.
				}
			}
		}

		// Didn't match any of the providers, see if it is a regular File path.
		File f = (new File(s)).getAbsoluteFile();
		try {
			uri = new URI("file:///");
			FileSystem fs = FileSystems.getFileSystem(uri);
			if (fs == null)
				return null;
			retVal = fs.getPath(f.getAbsolutePath());
		} catch (URISyntaxException e) {
			retVal = null;
		}
		return retVal;
	}

	/**
	 * Format used for HTTP date headers.
	 */
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

	/**
	 * Convenient lookup for the GMT time zone.
	 */
	private static final TimeZone GMT_TZ = TimeZone.getTimeZone("GMT");

	/**
	 * Get a new date formatter compatible with HTTP headers protocol.
	 * SPECIFICALLY... This means:
	 * The parser will parse GMT and return local time.
	 * The formatter will take a local time and output a GMT string.
	 */
	public static DateFormat GetHTTPDateFormater() {
		SimpleDateFormat f = new SimpleDateFormat(HTTP_DATE_FORMAT);
		f.setTimeZone(GMT_TZ);
		return f;
	}

	/**
	 * Wrap a few classes of objects into an appropriate HttpEntity.
	 *
	 * @param obj Currently may be one of:
	 *            org.apache.http.HttpEntity
	 *            java.lang.String
	 *            java.lang.byte[]
	 *            java.nio.ByteBuffer
	 *            java.io.InputStream
	 *            java.io.File
	 * @param ct  The content type (if any) of 'obj'
	 */
	public static HttpEntity WrapObjWithHttpEntity(Object obj, ContentType ct) {
		HttpEntity retVal;

		if (obj instanceof HttpEntity)
			retVal = (HttpEntity) obj;
		else if (obj instanceof String)
			retVal = new NStringEntity((String) obj, ct);
		else if (obj instanceof byte[])
			retVal = new NByteArrayEntity((byte[]) obj, ct);
		else if (obj instanceof InputStream)
			retVal = new InputStreamEntity((InputStream) obj, ct);
		else if (obj instanceof ByteBuffer)
			retVal = new NByteArrayEntity(((ByteBuffer) obj).array(), ct);
		else if (obj instanceof File)
			retVal = new NFileEntity((File) obj, ct);
		else
			retVal = null;
		return retVal;
	}
}
