package acr.browser.lightning.search;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

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
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.utils.Utils;

/**
 * Copyright 9/10/2016 Anthony Restaino
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public final class DuckSuggestionsTask {

    private static final String TAG = RetrieveSuggestionsTask.class.getSimpleName();

    private static final Pattern SPACE_PATTERN = Pattern.compile(" ", Pattern.LITERAL);
    private static final String ENCODING = "UTF-8";
    private static final long INTERVAL_DAY = TimeUnit.DAYS.toMillis(1);
    private static final String DEFAULT_LANGUAGE = "en";
    @Nullable private static String sLanguage;
    @NonNull private final SuggestionsResult mResultCallback;
    @NonNull private final Application mApplication;
    @NonNull private final String mSearchSubtitle;
    @NonNull private String mQuery;

    DuckSuggestionsTask(@NonNull String query,
                        @NonNull Application application,
                        @NonNull SuggestionsResult callback) {
        mQuery = query;
        mResultCallback = callback;
        mApplication = application;
        mSearchSubtitle = mApplication.getString(R.string.suggestion);
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

    @NonNull
    private static String getQueryUrl(@NonNull String query) {
        return "https://duckduckgo.com/ac/?q=" + query;
    }

    void run() {
        List<HistoryItem> filter = new ArrayList<>(5);
        try {
            mQuery = SPACE_PATTERN.matcher(mQuery).replaceAll("+");
            mQuery = URLEncoder.encode(mQuery, ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        File cache = downloadSuggestionsForQuery(mQuery, getLanguage(), mApplication);
        if (!cache.exists()) {
            post(filter);
            return;
        }
        InputStream fileInput = null;
        try {
            fileInput = new FileInputStream(cache);
            String content = FileUtils.readStringFromFile(fileInput, ENCODING);
            JSONArray jsonArray = new JSONArray(content);
            int counter = 0;
            for (int n = 0, size = jsonArray.length(); n < size; n++) {
                JSONObject object = jsonArray.getJSONObject(n);
                String suggestion = object.getString("phrase");
                filter.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                    suggestion, R.drawable.ic_search));
                counter++;
                if (counter >= 5) {
                    break;
                }
            }
        } catch (Exception e) {
            post(filter);
            e.printStackTrace();
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
    private static File downloadSuggestionsForQuery(@NonNull String query, String language, @NonNull Application app) {
        String queryUrl = getQueryUrl(query);
        File cacheFile = new File(app.getCacheDir(), queryUrl.hashCode() + Suggestions.CACHE_FILE_TYPE);
        if (System.currentTimeMillis() - INTERVAL_DAY < cacheFile.lastModified()) {
            return cacheFile;
        }
        if (!isNetworkConnected(app)) {
            return cacheFile;
        }
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            URL url = new URL(queryUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.connect();
            if (connection.getResponseCode() >= HttpURLConnection.HTTP_MULT_CHOICE ||
                connection.getResponseCode() < HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Search API Responded with code: " + connection.getResponseCode());
                connection.disconnect();
                return cacheFile;
            }

            in = connection.getInputStream();

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
            connection.disconnect();
            cacheFile.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            Log.w(TAG, "Problem getting search suggestions", e);
        } finally {
            Utils.close(in);
            Utils.close(fos);
        }
        return cacheFile;
    }

    private static boolean isNetworkConnected(@NonNull Context context) {
        NetworkInfo networkInfo = getActiveNetworkInfo(context);
        return networkInfo != null && networkInfo.isConnected();
    }

    @Nullable
    private static NetworkInfo getActiveNetworkInfo(@NonNull Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
            .getApplicationContext()
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return null;
        }
        return connectivity.getActiveNetworkInfo();
    }

}
