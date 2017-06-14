package acr.browser.lightning.search;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.utils.Utils;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

abstract class BaseSuggestionsModel {

    private static final String TAG = "BaseSuggestionsModel";

    static final int MAX_RESULTS = 5;
    private static final long INTERVAL_DAY = TimeUnit.DAYS.toSeconds(1);
    @NonNull private static final String DEFAULT_LANGUAGE = "en";

    @NonNull private final OkHttpClient mHttpClient;
    @NonNull private final CacheControl mCacheControl;
    @NonNull private final String mEncoding;
    @NonNull private final String mLanguage;

    @NonNull
    protected abstract String createQueryUrl(@NonNull String query, @NonNull String language);

    protected abstract void parseResults(@NonNull InputStream inputStream, @NonNull List<HistoryItem> results) throws Exception;

    BaseSuggestionsModel(@NonNull Application application, @NonNull String encoding) {
        mEncoding = encoding;
        mLanguage = getLanguage();
        File suggestionsCache = new File(application.getCacheDir(), "suggestion_responses");
        mHttpClient = new OkHttpClient.Builder()
            .cache(new Cache(suggestionsCache, FileUtils.megabytesToBytes(1)))
            .addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
            .build();
        mCacheControl = new CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build();
    }

    @NonNull
    private static String getLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (TextUtils.isEmpty(language)) {
            language = DEFAULT_LANGUAGE;
        }
        return language;
    }

    @NonNull
    List<HistoryItem> getResults(@NonNull String query) {
        List<HistoryItem> filter = new ArrayList<>(5);
        try {
            query = URLEncoder.encode(query, mEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode the URL", e);
        }
        InputStream inputStream = downloadSuggestionsForQuery(query, mLanguage);
        if (inputStream == null) {
            // There are no suggestions for this query, return an empty list.
            return filter;
        }
        try {
            parseResults(inputStream, filter);
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse results", e);
            return filter;
        } finally {
            Utils.close(inputStream);
        }

        return filter;
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not getResults on the UI thread.
     *
     * @param query the query to get suggestions for
     * @return the cache file containing the suggestions
     */
    @Nullable
    private InputStream downloadSuggestionsForQuery(@NonNull String query, @NonNull String language) {
        String queryUrl = createQueryUrl(query, language);

        try {
            URL url = new URL(queryUrl);

            // OkHttp automatically gzips requests
            Request suggestionsRequest = new Request.Builder().url(url)
                .addHeader("Accept-Charset", mEncoding)
                .cacheControl(mCacheControl)
                .build();

            Response suggestionsResponse = mHttpClient.newCall(suggestionsRequest).execute();

            ResponseBody responseBody = suggestionsResponse.body();
            return responseBody != null ? responseBody.byteStream() : null;
        } catch (IOException exception) {
            Log.e(TAG, "Problem getting search suggestions", exception);
        }

        return null;
    }

    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse.newBuilder()
                .header("cache-control", "max-age=" + INTERVAL_DAY + ", max-stale=" + INTERVAL_DAY)
                .build();
        }
    };

}
