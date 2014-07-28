/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.DownloadManager;
import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;

import java.io.IOException;

/**
 * This class is used to pull down the http headers of a given URL so that we can analyse the mimetype and make any
 * correction needed before we give the URL to the download manager. This operation is needed when the user long-clicks
 * on a link or image and we don't know the mimetype. If the user just clicks on the link, we will do the same steps of
 * correcting the mimetype down in android.os.webkit.LoadListener rather than handling it here.
 */
public class FetchUrlMimeType extends Thread {

	private Context mContext;

	private DownloadManager.Request mRequest;

	private String mUri;

	private String mCookies;

	private String mUserAgent;

	public FetchUrlMimeType(Context context, DownloadManager.Request request,
			String uri, String cookies, String userAgent) {
		mContext = context.getApplicationContext();
		mRequest = request;
		mUri = uri;
		mCookies = cookies;
		mUserAgent = userAgent;
	}

	@Override
	public void run() {
		// User agent is likely to be null, though the AndroidHttpClient
		// seems ok with that.
		AndroidHttpClient client = AndroidHttpClient.newInstance(mUserAgent);

		HttpHead request = new HttpHead(mUri);

		if (mCookies != null && mCookies.length() > 0) {
			request.addHeader("Cookie", mCookies);
		}

		HttpResponse response;
		String mimeType = null;
		String contentDisposition = null;
		try {
			response = client.execute(request);
			// We could get a redirect here, but if we do lets let
			// the download manager take care of it, and thus trust that
			// the server sends the right mimetype
			if (response.getStatusLine().getStatusCode() == 200) {
				Header header = response.getFirstHeader("Content-Type");
				if (header != null) {
					mimeType = header.getValue();
					final int semicolonIndex = mimeType.indexOf(';');
					if (semicolonIndex != -1) {
						mimeType = mimeType.substring(0, semicolonIndex);
					}
				}
				Header contentDispositionHeader = response.getFirstHeader("Content-Disposition");
				if (contentDispositionHeader != null) {
					contentDisposition = contentDispositionHeader.getValue();
				}
			}
		} catch (IllegalArgumentException ex) {
			request.abort();
		} catch (IOException ex) {
			request.abort();
		} finally {
			client.close();
		}

		if (mimeType != null) {
			if (mimeType.equalsIgnoreCase("text/plain") ||
					mimeType.equalsIgnoreCase("application/octet-stream")) {
				String newMimeType =
						MimeTypeMap.getSingleton().getMimeTypeFromExtension(
								MimeTypeMap.getFileExtensionFromUrl(mUri));
				if (newMimeType != null) {
					mRequest.setMimeType(newMimeType);
				}
			}
			String filename = URLUtil.guessFileName(mUri, contentDisposition,
					mimeType);
			mRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
		}

		// Start the download
		DownloadManager manager = (DownloadManager) mContext
				.getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(mRequest);
	}
}
