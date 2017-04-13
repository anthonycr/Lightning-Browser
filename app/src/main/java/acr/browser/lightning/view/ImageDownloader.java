package acr.browser.lightning.view;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.utils.Utils;

/**
 * An ImageDownloader that creates image
 * loading requests on demand.
 */
public class ImageDownloader {

    private static final String TAG = "ImageDownloader";

    @Inject Application mApp;

    @NonNull private final Bitmap mDefaultBitmap;
    @NonNull private final BitmapFactory.Options mLoaderOptions = new BitmapFactory.Options();

    public ImageDownloader(@NonNull Bitmap defaultBitmap) {
        BrowserApp.getAppComponent().inject(this);
        mDefaultBitmap = defaultBitmap;
    }

    /**
     * Creates a new image request for the given url.
     * Emits the bitmap associated with that url, or
     * the default bitmap if none was found.
     *
     * @param url the url for which to retrieve the bitmap.
     * @return a single that emits the bitmap that was found.
     */
    @NonNull
    public Single<Bitmap> newImageRequest(@Nullable final String url) {
        return Single.create(new SingleAction<Bitmap>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Bitmap> subscriber) {
                Bitmap favicon = retrieveFaviconForUrl(url);

                Bitmap paddedFavicon = Utils.padFavicon(favicon);

                subscriber.onItem(paddedFavicon);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    private Bitmap retrieveFaviconForUrl(@Nullable String url) {

        // unique path for each url that is bookmarked.
        if (url == null) {
            return mDefaultBitmap;
        }

        Bitmap icon;
        File cache = mApp.getCacheDir();
        Uri uri = Uri.parse(url);

        if (uri.getHost() == null || uri.getScheme() == null || Constants.FILE.startsWith(uri.getScheme())) {
            return mDefaultBitmap;
        }

        String hash = String.valueOf(uri.getHost().hashCode());
        File image = new File(cache, hash + ".png");
        String urlDisplay = uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";

        if (image.exists()) {
            // If image exists, pull it from the cache
            icon = BitmapFactory.decodeFile(image.getPath());
        } else {
            // Otherwise, load it from network
            icon = retrieveBitmapFromUrl(urlDisplay);
        }

        if (icon == null) {
            String googleFaviconUrl = "https://www.google.com/s2/favicons?domain_url=" + uri.toString();
            icon = retrieveBitmapFromUrl(googleFaviconUrl);
        }

        if (icon == null) {
            return mDefaultBitmap;
        } else {
            cacheBitmap(image, icon);

            return icon;
        }
    }

    private void cacheBitmap(@NonNull File cacheFile, @NonNull Bitmap imageToCache) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(cacheFile);
            imageToCache.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Could not cache icon");
        } finally {
            Utils.close(fos);
        }
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
