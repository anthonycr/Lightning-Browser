package acr.browser.lightning.async;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import acr.browser.lightning.utils.UrlUtils;

/**
 * Created by Viacheslav Titov on 08.02.2017.
 */

public class TrackFirstInstalledAppTask extends AsyncTask<String, Boolean, Boolean> {

    private static final String TAG = TrackFirstInstalledAppTask.class.getSimpleName();

    @Override
    protected Boolean doInBackground(String... params) {
        final String referrer = params[0];
        final String url = UrlUtils.makeTrackInstalledAppUrl(referrer, "utm_content");
        try {
            final URL urlDownload = new URL(url);
            final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
            connection.connect();
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
                Log.d(TAG, "app was installed");
                return true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "tracking didn't passed");
        return false;
    }
}
