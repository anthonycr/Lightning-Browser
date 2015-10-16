/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download;

import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BrowserEvents;

/**
 * This class is used to pull down the http headers of a given URL so that we
 * can analyse the mimetype and make any correction needed before we give the
 * URL to the download manager. This operation is needed when the user
 * long-clicks on a link or image and we don't know the mimetype. If the user
 * just clicks on the link, we will do the same steps of correcting the mimetype
 * down in android.os.webkit.LoadListener rather than handling it here.
 */
class FetchUrlMimeType extends Thread {

    private final Context mContext;

    private final DownloadManager.Request mRequest;

    private final String mUri;

    private final String mCookies;

    private final String mUserAgent;

    public FetchUrlMimeType(Context context, DownloadManager.Request request, String uri,
                            String cookies, String userAgent) {
        mContext = context;
        mRequest = request;
        mUri = uri;
        mCookies = cookies;
        mUserAgent = userAgent;
    }

    @Override
    public void run() {
        // User agent is likely to be null, though the AndroidHttpClient
        // seems ok with that.
        final Bus eventBus = BrowserApp.getAppComponent().getBus();
        String mimeType = null;
        String contentDisposition = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(mUri);
            connection = (HttpURLConnection) url.openConnection();
            if (mCookies != null && !mCookies.isEmpty()) {
                connection.addRequestProperty("Cookie", mCookies);
                connection.setRequestProperty("User-Agent", mUserAgent);
            }
            connection.connect();
            // We could get a redirect here, but if we do lets let
            // the download manager take care of it, and thus trust that
            // the server sends the right mimetype
            if (connection.getResponseCode() == 200) {
                String header = connection.getHeaderField("Content-Type");
                if (header != null) {
                    mimeType = header;
                    final int semicolonIndex = mimeType.indexOf(';');
                    if (semicolonIndex != -1) {
                        mimeType = mimeType.substring(0, semicolonIndex);
                    }
                }
                String contentDispositionHeader = connection.getHeaderField("Content-Disposition");
                if (contentDispositionHeader != null) {
                    contentDisposition = contentDispositionHeader;
                }
            }
        } catch (IllegalArgumentException | IOException ex) {
            if (connection != null)
                connection.disconnect();
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        String filename = "";
        if (mimeType != null) {
            if (mimeType.equalsIgnoreCase("text/plain")
                    || mimeType.equalsIgnoreCase("application/octet-stream")) {
                String newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(mUri));
                if (newMimeType != null) {
                    mRequest.setMimeType(newMimeType);
                }
            }
            filename = URLUtil.guessFileName(mUri, contentDisposition, mimeType);
            mRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        }

        // Start the download
        DownloadManager manager = (DownloadManager) mContext
                .getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(mRequest);
        Handler handler = new Handler(Looper.getMainLooper());
        final String file = filename;
        handler.post(new Runnable() {
            @Override
            public void run() {
                eventBus.post(new BrowserEvents.ShowSnackBarMessage(mContext.getString(R.string.download_pending) + ' ' + file));
            }
        });
    }
}
