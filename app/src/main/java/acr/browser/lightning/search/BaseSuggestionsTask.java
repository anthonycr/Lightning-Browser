package acr.browser.lightning.search;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

abstract class BaseSuggestionsTask {

    private static final String TAG = BaseSuggestionsTask.class.getSimpleName();

    static final int MAX_RESULTS = 5;
    private static final long INTERVAL_DAY = TimeUnit.DAYS.toMillis(1);
    private static final String DEFAULT_LANGUAGE = "en";
    @Nullable private static String sLanguage;
    @NonNull private final SuggestionsResult mResultCallback;
    @NonNull private final Application mApplication;
    @NonNull private final OkHttpClient mHttpClient = new OkHttpClient();
    @NonNull private final CacheControl mCacheControl;
    @NonNull private final ConnectivityManager mConnectivityManager;
    @NonNull private String mQuery;

    @NonNull
    protected abstract String getQueryUrl(@NonNull String query, @NonNull String language);

    protected abstract void parseResults(@NonNull FileInputStream inputStream, @NonNull List<HistoryItem> results) throws Exception;

    @NonNull
    protected abstract String getEncoding();

    BaseSuggestionsTask(@NonNull String query,
                        @NonNull Application application,
                        @NonNull SuggestionsResult callback) {
        mQuery = query;
        mResultCallback = callback;
        mApplication = application;
        mCacheControl = new CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build();
        mConnectivityManager = getConnectivityManager(mApplication);
    }

    @NonNull
    private static synchronized String getLanguage() {
        if (sLanguage == null) {
            sLanguage = Locale.getDefault().getLanguage();
        }
        if (TextUtils.isEmpty(sLanguage)) {
            sLanguage = DEFAULT_LANGUAGE;
        }
        return sLanguage;
    }

    void run() {
        List<HistoryItem> filter = new ArrayList<>(5);
        try {
            mQuery = URLEncoder.encode(mQuery, getEncoding());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode the URL", e);
        }
        File cache = downloadSuggestionsForQuery(mQuery, getLanguage(), mApplication);
        if (!cache.exists()) {
            post(filter);
            return;
        }
        FileInputStream fileInput = null;
        try {
            fileInput = new FileInputStream(cache);
            parseResults(fileInput, filter);
        } catch (Exception e) {
            post(filter);
            Log.e(TAG, "Unable to parse results", e);
            return;
        } finally {
            Utils.close(fileInput);
        }
        post(filter);
    }

    private void post(@NonNull List<HistoryItem> result) {
        mResultCallback.resultReceived(result);
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not run on the UI thread.
     *
     * @param query the query to get suggestions for
     * @return the cache file containing the suggestions
     */
    @NonNull
    private File downloadSuggestionsForQuery(@NonNull String query, String language, @NonNull Application app) {
        String queryUrl = getQueryUrl(query, language);
        File cacheFile = new File(app.getCacheDir(), queryUrl.hashCode() + SuggestionsAdapter.CACHE_FILE_TYPE);
        if (System.currentTimeMillis() - INTERVAL_DAY < cacheFile.lastModified()) {
            return cacheFile;
        }
        if (!isNetworkConnected()) {
            return cacheFile;
        }
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            URL url = new URL(queryUrl);
            Request suggestionsRequest = new Request.Builder().url(url)
                    .addHeader("Accept-Encoding", "gzip")
                    .addHeader("Accept-Charset", getEncoding())
                    .cacheControl(mCacheControl)
                    .build();

            Response suggestionsResponse = mHttpClient.newCall(suggestionsRequest).execute();

            if (suggestionsResponse.code() >= HttpURLConnection.HTTP_MULT_CHOICE ||
                    suggestionsResponse.code() < HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Search API Responded with code: " + suggestionsResponse.code());
                suggestionsResponse.body().close();
                return cacheFile;
            }

            in = suggestionsResponse.body().byteStream();

            if (in != null) {
                in = new GZIPInputStream(in);
                //noinspection IOResourceOpenedButNotSafelyClosed
                fos = new FileOutputStream(cacheFile);
                int buffer;
                while ((buffer = in.read()) != -1) {
                    fos.write(buffer);
                }
                fos.flush();
            }
            suggestionsResponse.body().close();
            cacheFile.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            Log.w(TAG, "Problem getting search suggestions", e);
        } finally {
            Utils.close(in);
            Utils.close(fos);
        }
        return cacheFile;
    }

    private boolean isNetworkConnected() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @NonNull
    private static ConnectivityManager getConnectivityManager(@NonNull Context context) {
        return (ConnectivityManager) context
                .getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

}
