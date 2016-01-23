package acr.browser.lightning.async;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;

public class ImageDownloadTask extends AsyncTask<Void, Void, Bitmap> {

    private static final String TAG = ImageDownloadTask.class.getSimpleName();
    private final File mCacheDir;
    private final WeakReference<ImageView> bmImage;
    private final HistoryItem mWeb;
    private final String mUrl;
    private final Bitmap mDefaultBitmap;

    public ImageDownloadTask(@NonNull ImageView bmImage,
                             @NonNull HistoryItem web,
                             @NonNull Bitmap defaultBitmap,
                             @NonNull Context context) {
        // Set a tag on the ImageView so we know if the view
        // has gone out of scope and should not be used
        bmImage.setTag(web.getUrl().hashCode());
        this.bmImage = new WeakReference<>(bmImage);
        this.mWeb = web;
        this.mUrl = web.getUrl();
        this.mDefaultBitmap = defaultBitmap;
        this.mCacheDir = BrowserApp.get(context).getCacheDir();
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Bitmap mIcon = null;
        // unique path for each url that is bookmarked.
        if (mUrl == null) {
            return mDefaultBitmap;
        }
        final Uri uri = Uri.parse(mUrl);
        if (uri.getHost() == null || uri.getScheme() == null) {
            return mDefaultBitmap;
        }
        final String hash = String.valueOf(uri.getHost().hashCode());
        final File image = new File(mCacheDir, hash + ".png");
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
                    mIcon = BitmapFactory.decodeStream(in);
                }
                // ...and cache it
                if (mIcon != null) {
                    fos = new FileOutputStream(image);
                    mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
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
            mIcon = BitmapFactory.decodeFile(image.getPath());
        }
        if (mIcon == null) {
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
                    mIcon = BitmapFactory.decodeStream(in);
                }
                // ...and cache it
                if (mIcon != null) {
                    fos = new FileOutputStream(image);
                    mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                }

            } catch (Exception e) {
                Log.d(TAG, "Could not download Google favicon");
            } finally {
                Utils.close(in);
                Utils.close(fos);
            }
        }
        if (mIcon == null) {
            return mDefaultBitmap;
        } else {
            return mIcon;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        AsyncExecutor.getInstance().notifyThreadFinish();
        final Bitmap fav = Utils.padFavicon(bitmap);
        ImageView view = bmImage.get();
        if (view != null && view.getTag().equals(mWeb.getUrl().hashCode())) {
            view.setImageBitmap(fav);
        }
        mWeb.setBitmap(fav);
    }

}
