/*
 *  Copyright 2011 Peter Karich 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package acr.browser.lightning.Reading;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Locale;

import acr.browser.lightning.Constants;
import android.util.Log;

/**
 * This class is not thread safe. Use one new instance every time due to
 * encoding variable.
 * 
 * @author Peter Karich
 */
public class Converter {

	public final static String UTF8 = "UTF-8";
	public final static String ISO = "ISO-8859-1";
	public final static int K2 = 2048;
	private int maxBytes = 1000000 / 2;
	private String encoding;
	private String url;

	public Converter(String urlOnlyHint) {
		url = urlOnlyHint;
	}

	public Converter() {
	}

	public Converter setMaxBytes(int maxBytes) {
		this.maxBytes = maxBytes;
		return this;
	}

	public static String extractEncoding(String contentType) {
		String[] values;
		if (contentType != null)
			values = contentType.split(";");
		else
			values = new String[0];

		String charset = "";

		for (String value : values) {
			value = value.trim().toLowerCase(Locale.getDefault());

			if (value.startsWith("charset="))
				charset = value.substring("charset=".length());
		}

		// http1.1 says ISO-8859-1 is the default charset
		if (charset.length() == 0)
			charset = ISO;

		return charset;
	}

	public String getEncoding() {
		if (encoding == null)
			return "";
		return encoding.toLowerCase(Locale.getDefault());
	}

	public String streamToString(InputStream is) {
		return streamToString(is, maxBytes, encoding);
	}

	public String streamToString(InputStream is, String enc) {
		return streamToString(is, maxBytes, enc);
	}

	/**
	 * reads bytes off the string and returns a string
	 * 
	 * @param is
	 * @param maxBytes
	 *            The max bytes that we want to read from the input stream
	 * @return String
	 */
	public String streamToString(InputStream is, int maxBytes, String enc) {
		encoding = enc;
		// Http 1.1. standard is iso-8859-1 not utf8 :(
		// but we force utf-8 as youtube assumes it ;)
		if (encoding == null || encoding.isEmpty())
			encoding = UTF8;

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(is, K2);
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			// detect encoding with the help of meta tag
			try {
				in.mark(K2 * 2);
				String tmpEnc = detectCharset("charset=", output, in, encoding);
				if (tmpEnc != null)
					encoding = tmpEnc;
				else {
					Log.d(Constants.TAG, "no charset found in first stage");
					// detect with the help of xml beginning ala
					// encoding="charset"
					tmpEnc = detectCharset("encoding=", output, in, encoding);
					if (tmpEnc != null)
						encoding = tmpEnc;
					else
						Log.d(Constants.TAG, "no charset found in second stage");
				}

				if (!Charset.isSupported(encoding))
					throw new UnsupportedEncodingException(encoding);
			} catch (UnsupportedEncodingException e) {
				Log.d(Constants.TAG,
						"Using default encoding:" + UTF8 + " problem:" + e.getMessage()
								+ " encoding:" + encoding + " " + url);
				encoding = UTF8;
			}

			// SocketException: Connection reset
			// IOException: missing CR => problem on server (probably some xml
			// character thing?)
			// IOException: Premature EOF => socket unexpectly closed from
			// server
			int bytesRead = output.size();
			byte[] arr = new byte[K2];
			while (true) {
				if (bytesRead >= maxBytes) {
					Log.d(Constants.TAG, "Maxbyte of " + maxBytes
							+ " exceeded! Maybe html is now broken but try it nevertheless. Url: "
							+ url);
					break;
				}

				int n = in.read(arr);
				if (n < 0)
					break;
				bytesRead += n;
				output.write(arr, 0, n);
			}

			return output.toString(encoding);
		} catch (SocketTimeoutException e) {
			Log.e(Constants.TAG, e.toString() + " url:" + url);
		} catch (IOException e) {
			Log.e(Constants.TAG, e.toString() + " url:" + url);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
		return "";
	}

	/**
	 * This method detects the charset even if the first call only returns some
	 * bytes. It will read until 4K bytes are reached and then try to determine
	 * the encoding
	 * 
	 * @throws IOException
	 */
	protected String detectCharset(String key, ByteArrayOutputStream bos, BufferedInputStream in,
			String enc) throws IOException {

		// Grab better encoding from stream
		byte[] arr = new byte[K2];
		int nSum = 0;
		while (nSum < K2) {
			int n = in.read(arr);
			if (n < 0)
				break;

			nSum += n;
			bos.write(arr, 0, n);
		}

		String str = bos.toString(enc);
		int encIndex = str.indexOf(key);
		int clength = key.length();
		if (encIndex > 0) {
			char startChar = str.charAt(encIndex + clength);
			int lastEncIndex;
			if (startChar == '\'')
				// if we have charset='something'
				lastEncIndex = str.indexOf("'", ++encIndex + clength);
			else if (startChar == '\"')
				// if we have charset="something"
				lastEncIndex = str.indexOf("\"", ++encIndex + clength);
			else {
				// if we have "text/html; charset=utf-8"
				int first = str.indexOf("\"", encIndex + clength);
				if (first < 0)
					first = Integer.MAX_VALUE;

				// or "text/html; charset=utf-8 "
				int sec = str.indexOf(" ", encIndex + clength);
				if (sec < 0)
					sec = Integer.MAX_VALUE;
				lastEncIndex = Math.min(first, sec);

				// or "text/html; charset=utf-8 '
				int third = str.indexOf("'", encIndex + clength);
				if (third > 0)
					lastEncIndex = Math.min(lastEncIndex, third);
			}

			// re-read byte array with different encoding
			// assume that the encoding string cannot be greater than 40 chars
			if (lastEncIndex > encIndex + clength && lastEncIndex < encIndex + clength + 40) {
				String tmpEnc = SHelper.encodingCleanup(str.substring(encIndex + clength,
						lastEncIndex));
				try {
					in.reset();
					bos.reset();
					return tmpEnc;
				} catch (IOException ex) {
					Log.e(Constants.TAG, "Couldn't reset stream to re-read with new encoding "
							+ tmpEnc + " " + ex.toString());
				}
			}
		}
		return null;
	}
}
