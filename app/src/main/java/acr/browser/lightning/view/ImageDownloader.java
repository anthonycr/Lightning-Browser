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

    @NonNull private Bitmap mDefaultBitmap;

    public ImageDownloader(@NonNull Bitmap defaultBitmap) {
        mDefaultBitmap = defaultBitmap;
        BrowserApp.getAppComponent().inject(this);
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
                Bitmap favicon = retrieveBitmap(mApp, mDefaultBitmap, url);

                Bitmap paddedFavicon = Utils.padFavicon(favicon);

                subscriber.onItem(paddedFavicon);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    private static Bitmap retrieveBitmap(@NonNull Application app,
                                         @NonNull Bitmap defaultBitmap,
                                         @Nullable String url) {

        // unique path for each url that is bookmarked.
        if (url == null) {
            return defaultBitmap;
        }

        Bitmap icon = null;
        File cache = app.getCacheDir();
        final Uri uri = Uri.parse(url);

        if (uri.getHost() == null || uri.getScheme() == null || Constants.FILE.startsWith(uri.getScheme())) {
            return defaultBitmap;
        }

        final String hash = String.valueOf(uri.getHost().hashCode());
        final File image = new File(cache, hash + ".png");
        final String urlDisplay = uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";

        // checks to see if the image exists
        if (!image.exists()) {
            FileOutputStream fos = null;
            InputStream in = null;
            try {
                // if not, download it...
                final URL urlDownload = new URL(urlDisplay);
                final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                in = connection.getInputStream();

                if (in != null) {
                    icon = BitmapFactory.decodeStream(in);
                }
                // ...and cache it
                if (icon != null) {
                    fos = new FileOutputStream(image);
                    icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    Log.d(Constants.TAG, "Downloaded: " + urlDisplay);
                }

            } catch (Exception ignored) {
                Log.d(TAG, "Could not download: " + urlDisplay);
            } finally {
                Utils.close(in);
                Utils.close(fos);
            }
        } else {
            // if it exists, retrieve it from the cache
            icon = BitmapFactory.decodeFile(image.getPath());
        }

        if (icon == null) {
            InputStream in = null;
            FileOutputStream fos = null;
            try {
                // if not, download it...
                final URL urlDownload = new URL("https://www.google.com/s2/favicons?domain_url=" + uri.toString());
                final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                in = connection.getInputStream();

                if (in != null) {
                    icon = BitmapFactory.decodeStream(in);
                }
                // ...and cache it
                if (icon != null) {
                    fos = new FileOutputStream(image);
                    icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                }

            } catch (Exception e) {
                Log.d(TAG, "Could not download Google favicon");
            } finally {
                Utils.close(in);
                Utils.close(fos);
            }
        }

        if (icon == null) {
            return defaultBitmap;
        } else {
            return icon;
        }
    }
}
