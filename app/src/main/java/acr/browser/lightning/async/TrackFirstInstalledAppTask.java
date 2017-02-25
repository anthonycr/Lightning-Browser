package acr.browser.lightning.async;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.utils.UrlUtils;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;

/**
 * Created by Viacheslav Titov on 08.02.2017.
 */

public class TrackFirstInstalledAppTask extends AsyncTask<String, Boolean, Boolean> {

    private static final String TAG = TrackFirstInstalledAppTask.class.getSimpleName();

    private static final String HTTP_PARAM_DATE = "date";
    private static final String HTTP_PARAM_APP_ID = "app_id";
    private static final String HTTP_PARAM_REFERRER_ID = "referrer_id";
    private static final String HTTP_PARAM_PUBLIC_KEY = "p_key";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd"); //from server side @DateTimeFormat(pattern = "YYYY-MM-dd") //for example 2017-02-14

    @Override
    protected Boolean doInBackground(String... params) {
        if (params == null || params.length < 2) return false;
        final String referrer = params[0];
        final String referrerId = UrlUtils.getReferrerIdForInstalledApp(referrer, "utm_content");
        final String appId = params[1];
        final String publicKey = params[2];
        HashMap<String, String> httpParameters = new HashMap<>();
        httpParameters.put(HTTP_PARAM_DATE, DATE_FORMAT.format(new Date()));
        httpParameters.put(HTTP_PARAM_APP_ID, appId);
        httpParameters.put(HTTP_PARAM_REFERRER_ID, referrerId);
        httpParameters.put(HTTP_PARAM_PUBLIC_KEY, publicKey);
        try {
            final URL urlDownload = new URL(Constants.TRECKING_MOBITECH_INSTALLED_APP);
            final HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
            connection.setRequestMethod(HttpPost.METHOD_NAME);
            UrlUtils.putHttpParameters(httpParameters, connection);
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
