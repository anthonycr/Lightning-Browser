package acr.browser.lightning.utils;

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
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.HistoryItem;

/**
 * Created by Stefano Pacifici on 25/08/15.
 */
public class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

    final ImageView bmImage;
    final HistoryItem mWeb;
    final File mCacheDir;
    final String mUrl;
    final Bitmap mDefaultBitmap;

    public DownloadImageTask(@NonNull ImageView bmImage, @NonNull HistoryItem web,
                             @NonNull Bitmap defaultBitmap) {
        assert bmImage != null;
        assert web != null;
        assert defaultBitmap != null;
        this.bmImage = bmImage;
        this.mWeb = web;
        this.mCacheDir = bmImage.getContext().getCacheDir();
        this.mUrl = web.getUrl();
        this.mDefaultBitmap = defaultBitmap;
    }

    protected Bitmap doInBackground(Void... params) {
        Bitmap mIcon = null;
        // unique path for each url that is bookmarked.
        final Uri uri = Uri.parse(mUrl);

        final String hash = "" + Utils.hash(uri.getHost());
        final File image = new File(mCacheDir, hash + ".png");
        final Uri urldisplay = Uri.fromParts(uri.getScheme(), uri.getHost(), "favicon.ico");
        // checks to see if the image exists
        if (!image.exists()) {
            FileOutputStream fos = null;
            InputStream in = null;
            try {
                // if not, download it...
                final URL urlDownload = new URL(urldisplay.toString());
                final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                connection.setDoInput(true);
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
                    Log.d(Constants.TAG, "Downloaded: " + urldisplay);
                }

            } catch (Exception e) {
                e.printStackTrace();
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
                final URL urlDownload = new URL("https://www.google.com/s2/favicons?domain_url="
                        + uri.toString());
                final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                connection.setDoInput(true);
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
                e.printStackTrace();
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

    protected void onPostExecute(Bitmap result) {
        final Bitmap fav = Utils.padFavicon(result);
        bmImage.setImageBitmap(fav);
        mWeb.setBitmap(fav);
        // notifyBookmarkDataSetChanged();
    }
}
