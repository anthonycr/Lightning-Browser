package acr.browser.lightning.favicon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import acr.browser.lightning.utils.Utils;

/**
 * An image fetcher that creates image
 * loading requests on demand.
 */
class ImageFetcher {

    private static final String TAG = "ImageFetcher";

    @NonNull private final BitmapFactory.Options mLoaderOptions = new BitmapFactory.Options();

    ImageFetcher() {
    }

    @Nullable
    Bitmap retrieveFaviconFromCache(@NonNull File cacheFile) {
        return BitmapFactory.decodeFile(cacheFile.getPath(), mLoaderOptions);
    }

    @Nullable
    Bitmap retrieveBitmapFromDomain(@NonNull Uri uri) {
        FaviconUtils.assertUriSafe(uri);

        String faviconUrlGuess = uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";

        return retrieveBitmapFromUrl(faviconUrlGuess);
    }

    @Nullable
    Bitmap retrieveBitmapFromGoogle(@NonNull Uri uri) {
        FaviconUtils.assertUriSafe(uri);

        String googleFaviconUrl = "https://www.google.com/s2/favicons?domain_url=" + uri.toString();

        return retrieveBitmapFromUrl(googleFaviconUrl);
    }


    @Nullable
    private Bitmap retrieveBitmapFromUrl(@NonNull String url) {
        InputStream in = null;
        Bitmap icon = null;

        try {
            final URL urlDownload = new URL(url);
            final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.connect();
            in = connection.getInputStream();

            if (in != null) {
                icon = BitmapFactory.decodeStream(in, null, mLoaderOptions);
            }
        } catch (Exception ignored) {
            Log.d(TAG, "Could not download icon from: " + url);
        } finally {
            Utils.close(in);
        }

        return icon;
    }


}
