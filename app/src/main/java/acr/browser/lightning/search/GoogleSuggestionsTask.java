package acr.browser.lightning.search;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
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
import acr.browser.lightning.utils.Utils;

public class GoogleSuggestionsTask {

    private static final String TAG = RetrieveSuggestionsTask.class.getSimpleName();

    private static final Pattern SPACE_PATTERN = Pattern.compile(" ", Pattern.LITERAL);
    private static final String ENCODING = "ISO-8859-1";
    private static final long INTERVAL_DAY = TimeUnit.DAYS.toMillis(1);
    private static final String DEFAULT_LANGUAGE = "en";
    @Nullable private static XmlPullParser sXpp;
    @Nullable private static String sLanguage;
    @NonNull private final SuggestionsResult mResultCallback;
    @NonNull private final Application mApplication;
    @NonNull private final String mSearchSubtitle;
    @NonNull private String mQuery;

    GoogleSuggestionsTask(@NonNull String query,
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
    private static String getQueryUrl(@NonNull String query, @NonNull String language) {
        return "https://suggestqueries.google.com/complete/search?output=toolbar&hl="
            + language + "&q=" + query;
    }

    @NonNull
    private static synchronized XmlPullParser getParser() throws XmlPullParserException {
        if (sXpp == null) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            sXpp = factory.newPullParser();
        }
        return sXpp;
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
            fileInput = new BufferedInputStream(new FileInputStream(cache));
            XmlPullParser parser = getParser();
            parser.setInput(fileInput, ENCODING);
            int eventType = parser.getEventType();
            int counter = 0;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "suggestion".equals(parser.getName())) {
                    String suggestion = parser.getAttributeValue(null, "data");
                    filter.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                        suggestion, R.drawable.ic_search));
                    counter++;
                    if (counter >= 5) {
                        break;
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            post(filter);
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
        String queryUrl = getQueryUrl(query, language);
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
            // Old API that doesn't support HTTPS
            // http://google.com/complete/search?q= + query + &output=toolbar&hl= + language
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
                //noinspection IOResourceOpenedButNotSafelyClosed
                in = new GZIPInputStream(in);
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
