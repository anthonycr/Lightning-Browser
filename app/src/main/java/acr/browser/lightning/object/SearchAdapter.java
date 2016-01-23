package acr.browser.lightning.object;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

public class SearchAdapter extends BaseAdapter implements Filterable {

    private static final String TAG = SearchAdapter.class.getSimpleName();
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ", Pattern.LITERAL);
    private final List<HistoryItem> mHistory = new ArrayList<>(5);
    private final List<HistoryItem> mBookmarks = new ArrayList<>(5);
    private final List<HistoryItem> mSuggestions = new ArrayList<>(5);
    private final List<HistoryItem> mFilteredList = new ArrayList<>(5);
    private final List<HistoryItem> mAllBookmarks = new ArrayList<>(5);
    private final Object mLock = new Object();
    private final Context mContext;
    private boolean mUseGoogle = true;
    private boolean mIsExecuting = false;
    private final boolean mDarkTheme;
    private final boolean mIncognito;
    private static final String CACHE_FILE_TYPE = ".sgg";
    private static final String ENCODING = "ISO-8859-1";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final long INTERVAL_DAY = 86400000;
    private static final int MAX_SUGGESTIONS = 5;
    private static final SuggestionsComparator mComparator = new SuggestionsComparator();
    private final String mSearchSubtitle;
    private SearchFilter mFilter;
    private final Drawable mSearchDrawable;
    private final Drawable mHistoryDrawable;
    private final Drawable mBookmarkDrawable;
    private String mLanguage;

    @Inject
    HistoryDatabase mDatabaseHandler;

    @Inject
    BookmarkManager mBookmarkManager;

    @Inject
    PreferenceManager mPreferenceManager;

    public SearchAdapter(Context context, boolean dark, boolean incognito) {
        BrowserApp.getAppComponent().inject(this);
        mAllBookmarks.addAll(mBookmarkManager.getAllBookmarks(true));
        mUseGoogle = mPreferenceManager.getGoogleSearchSuggestionsEnabled();
        mContext = context;
        mSearchSubtitle = mContext.getString(R.string.suggestion);
        mDarkTheme = dark || incognito;
        mIncognito = incognito;
        Thread delete = new Thread(new ClearCacheRunnable(context));
        mSearchDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_search, mDarkTheme);
        mBookmarkDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_bookmark, mDarkTheme);
        mHistoryDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_history, mDarkTheme);
        delete.setPriority(Thread.MIN_PRIORITY);
        delete.start();
        mLanguage = Locale.getDefault().getLanguage();
        if (mLanguage.isEmpty()) {
            mLanguage = DEFAULT_LANGUAGE;
        }
    }

    private static class NameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String filename) {
            return filename.endsWith(CACHE_FILE_TYPE);
        }

    }

    public void refreshPreferences() {
        mUseGoogle = mPreferenceManager.getGoogleSearchSuggestionsEnabled();
        if (!mUseGoogle) {
            synchronized (mSuggestions) {
                mSuggestions.clear();
            }
        }
    }

    public void refreshBookmarks() {
        synchronized (mLock) {
            mAllBookmarks.clear();
            mAllBookmarks.addAll(mBookmarkManager.getAllBookmarks(true));
        }
    }

    @Override
    public int getCount() {
        return mFilteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SuggestionHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.two_line_autocomplete, parent, false);

            holder = new SuggestionHolder();
            holder.mTitle = (TextView) convertView.findViewById(R.id.title);
            holder.mUrl = (TextView) convertView.findViewById(R.id.url);
            holder.mImage = (ImageView) convertView.findViewById(R.id.suggestionIcon);
            convertView.setTag(holder);
        } else {
            holder = (SuggestionHolder) convertView.getTag();
        }
        HistoryItem web;
        web = mFilteredList.get(position);
        holder.mTitle.setText(web.getTitle());
        holder.mUrl.setText(web.getUrl());

        Drawable image;
        switch (web.getImageId()) {
            case R.drawable.ic_bookmark: {
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mBookmarkDrawable;
                break;
            }
            case R.drawable.ic_search: {
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mSearchDrawable;
                break;
            }
            case R.drawable.ic_history: {
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mHistoryDrawable;
                break;
            }
            default:
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mSearchDrawable;
                break;
        }

        holder.mImage.setImageDrawable(image);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new SearchFilter();
        }
        return mFilter;
    }

    private static class ClearCacheRunnable implements Runnable {

        private Context context;

        public ClearCacheRunnable(Context context) {
            this.context = BrowserApp.get(context);
        }

        @Override
        public void run() {
            File dir = new File(context.getCacheDir().toString());
            String[] fileList = dir.list(new NameFilter());
            long earliestTimeAllowed = System.currentTimeMillis() - INTERVAL_DAY;
            for (String fileName : fileList) {
                File file = new File(dir.getPath() + fileName);
                if (earliestTimeAllowed > file.lastModified()) {
                    file.delete();
                }
            }
        }

    }

    private class SearchFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null) {
                return results;
            }
            String query = constraint.toString().toLowerCase(Locale.getDefault());
            if (mUseGoogle && !mIncognito && !mIsExecuting) {
                new RetrieveSearchSuggestions().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, query);
            }

            int counter = 0;
            synchronized (mBookmarks) {
                mBookmarks.clear();
                synchronized (mLock) {
                    for (int n = 0; n < mAllBookmarks.size(); n++) {
                        if (counter >= 5) {
                            break;
                        }
                        if (mAllBookmarks.get(n).getTitle().toLowerCase(Locale.getDefault())
                                .startsWith(query)) {
                            mBookmarks.add(mAllBookmarks.get(n));
                            counter++;
                        } else if (mAllBookmarks.get(n).getUrl().contains(query)) {
                            mBookmarks.add(mAllBookmarks.get(n));
                            counter++;
                        }
                    }
                }
            }

            List<HistoryItem> historyList = mDatabaseHandler.findItemsContaining(constraint.toString());
            synchronized (mHistory) {
                mHistory.clear();
                mHistory.addAll(historyList);
            }
            results.count = 1;
            return results;
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return ((HistoryItem) resultValue).getUrl();
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            synchronized (mFilteredList) {
                mFilteredList.clear();
                List<HistoryItem> filtered = getFilteredList();
                Collections.sort(filtered, mComparator);
                mFilteredList.addAll(filtered);
            }
            notifyDataSetChanged();
        }

    }

    private static class SuggestionHolder {
        ImageView mImage;
        TextView mTitle;
        TextView mUrl;
    }

    private class RetrieveSearchSuggestions extends AsyncTask<String, Void, List<HistoryItem>> {

        private XmlPullParserFactory mFactory;
        private XmlPullParser mXpp;

        @Override
        protected List<HistoryItem> doInBackground(String... arg0) {
            mIsExecuting = true;

            List<HistoryItem> filter = new ArrayList<>();
            String query = arg0[0];
            try {
                query = SPACE_PATTERN.matcher(query).replaceAll("+");
                URLEncoder.encode(query, ENCODING);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            File cache = downloadSuggestionsForQuery(query, mLanguage, BrowserApp.get(mContext));
            if (!cache.exists()) {
                return filter;
            }
            InputStream fileInput = null;
            try {
                fileInput = new BufferedInputStream(new FileInputStream(cache));
                if (mFactory == null) {
                    mFactory = XmlPullParserFactory.newInstance();
                    mFactory.setNamespaceAware(true);
                }
                if (mXpp == null) {
                    mXpp = mFactory.newPullParser();
                }
                mXpp.setInput(fileInput, ENCODING);
                int eventType = mXpp.getEventType();
                int counter = 0;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "suggestion".equals(mXpp.getName())) {
                        String suggestion = mXpp.getAttributeValue(null, "data");
                        filter.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                                suggestion, R.drawable.ic_search));
                        counter++;
                        if (counter >= 5) {
                            break;
                        }
                    }
                    eventType = mXpp.next();
                }
            } catch (Exception e) {
                return filter;
            } finally {
                Utils.close(fileInput);
            }
            return filter;
        }

        @Override
        protected void onPostExecute(List<HistoryItem> result) {
            mIsExecuting = false;
            synchronized (mSuggestions) {
                mSuggestions.clear();
                mSuggestions.addAll(result);
            }
            synchronized (mFilteredList) {
                mFilteredList.clear();
                List<HistoryItem> filtered = getFilteredList();
                Collections.sort(filtered, mComparator);
                mFilteredList.addAll(filtered);
                notifyDataSetChanged();
            }
        }

    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not run on the UI thread.
     *
     * @param query the query to get suggestions for
     * @return the cache file containing the suggestions
     */
    private static File downloadSuggestionsForQuery(String query, String language, Application app) {
        File cacheFile = new File(app.getCacheDir(), query.hashCode() + CACHE_FILE_TYPE);
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
            URL url = new URL("https://suggestqueries.google.com/complete/search?output=toolbar&hl="
                    + language + "&q=" + query);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            if (connection.getResponseCode() >= HttpURLConnection.HTTP_MULT_CHOICE ||
                    connection.getResponseCode() < HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Search API Responded with code: " + connection.getResponseCode());
                return cacheFile;
            }
            in = connection.getInputStream();

            if (in != null) {
                //noinspection IOResourceOpenedButNotSafelyClosed
                fos = new FileOutputStream(cacheFile);
                int buffer;
                while ((buffer = in.read()) != -1) {
                    fos.write(buffer);
                }
                fos.flush();
            }
            cacheFile.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(in);
            Utils.close(fos);
        }
        return cacheFile;
    }

    private static boolean isNetworkConnected(Context context) {
        NetworkInfo networkInfo = getActiveNetworkInfo(context);
        return networkInfo != null && networkInfo.isConnected();
    }

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return null;
        }
        return connectivity.getActiveNetworkInfo();
    }

    private synchronized List<HistoryItem> getFilteredList() {
        List<HistoryItem> list = new ArrayList<>(5);
        synchronized (mBookmarks) {
            synchronized (mHistory) {
                synchronized (mSuggestions) {
                    Iterator<HistoryItem> bookmark = mBookmarks.iterator();
                    Iterator<HistoryItem> history = mHistory.iterator();
                    Iterator<HistoryItem> suggestion = mSuggestions.listIterator();
                    while (list.size() < MAX_SUGGESTIONS) {
                        if (!bookmark.hasNext() && !suggestion.hasNext() && !history.hasNext()) {
                            return list;
                        }
                        if (bookmark.hasNext()) {
                            list.add(bookmark.next());
                        }
                        if (suggestion.hasNext() && list.size() < MAX_SUGGESTIONS) {
                            list.add(suggestion.next());
                        }
                        if (history.hasNext() && list.size() < MAX_SUGGESTIONS) {
                            list.add(history.next());
                        }
                    }
                }
            }
        }
        return list;
    }

    private static class SuggestionsComparator implements Comparator<HistoryItem> {

        @Override
        public int compare(HistoryItem lhs, HistoryItem rhs) {
            if (lhs.getImageId() == rhs.getImageId()) return 0;
            if (lhs.getImageId() == R.drawable.ic_bookmark) return -1;
            if (rhs.getImageId() == R.drawable.ic_bookmark) return 1;
            if (lhs.getImageId() == R.drawable.ic_history) return -1;
            return 1;
        }
    }

}
