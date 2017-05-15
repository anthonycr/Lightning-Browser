package acr.browser.lightning.favicon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.InputStream;

import acr.browser.lightning.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An image fetcher that creates image
 * loading requests on demand.
 */
class ImageFetcher {

    private static final String TAG = "ImageFetcher";

    @NonNull private final BitmapFactory.Options mLoaderOptions = new BitmapFactory.Options();
    @NonNull private final OkHttpClient mHttpClient = new OkHttpClient();

    ImageFetcher() {}

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

        String googleFaviconUrl = "https://www.google.com/s2/favicons?domain_url=" + uri.getHost();

        return retrieveBitmapFromUrl(googleFaviconUrl);
    }

    @Nullable
    private Bitmap retrieveBitmapFromUrl(@NonNull String url) {
        Bitmap icon = null;

        InputStream boundsStream = null;
        InputStream iconStream = null;

        try {
            mLoaderOptions.inSampleSize = 1;
            mLoaderOptions.inJustDecodeBounds = true;

            Request imageRequest = new Request.Builder().url(url).build();

            Response boundsResponse = mHttpClient.newCall(imageRequest).execute();
            boundsStream = boundsResponse.body().byteStream();

            BitmapFactory.decodeStream(boundsStream, null, mLoaderOptions);

            boundsResponse.body().close();

            int size = Utils.dpToPx(24);

            mLoaderOptions.inSampleSize = Utils.calculateInSampleSize(mLoaderOptions, size, size);
            mLoaderOptions.inJustDecodeBounds = false;

            Response imageResponse = mHttpClient.newCall(imageRequest).execute();
            iconStream = imageResponse.body().byteStream();

            icon = BitmapFactory.decodeStream(iconStream, null, mLoaderOptions);

            imageResponse.body().close();
        } catch (Exception e) {
            Log.d(TAG, "Unable to download icon: " + url);
        } finally {
            Utils.close(boundsStream);
            Utils.close(iconStream);
        }

        return icon;
    }

}
