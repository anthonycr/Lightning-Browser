package acr.browser.lightning.search;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.preference.PreferenceManager;

import com.anthonycr.bonsai.Action;
import com.anthonycr.bonsai.Observable;
import com.anthonycr.bonsai.OnSubscribe;
import com.anthonycr.bonsai.Scheduler;
import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.Subscriber;

import acr.browser.lightning.utils.ThemeUtils;

public class SuggestionsAdapter extends BaseAdapter implements Filterable {

    private static final Scheduler FILTER_SCHEDULER = Schedulers.newSingleThreadedScheduler();

    static final String CACHE_FILE_TYPE = ".sgg";

    private final List<HistoryItem> mFilteredList = new ArrayList<>(5);

    private final List<HistoryItem> mHistory = new ArrayList<>(5);
    private final List<HistoryItem> mBookmarks = new ArrayList<>(5);
    private final List<HistoryItem> mSuggestions = new ArrayList<>(5);

    private static final int MAX_SUGGESTIONS = 5;

    @NonNull private final Drawable mSearchDrawable;
    @NonNull private final Drawable mHistoryDrawable;
    @NonNull private final Drawable mBookmarkDrawable;

    private final Comparator<HistoryItem> mFilterComparator = new SuggestionsComparator();

    @Inject HistoryDatabase mDatabaseHandler;
    @Inject BookmarkManager mBookmarkManager;
    @Inject PreferenceManager mPreferenceManager;

    private final List<HistoryItem> mAllBookmarks = new ArrayList<>(5);

    private final boolean mDarkTheme;
    private boolean mIsIncognito = true;
    @NonNull private final Context mContext;
    private PreferenceManager.Suggestion mSuggestionChoice;

    public SuggestionsAdapter(@NonNull Context context, boolean dark, boolean incognito) {
        super();
        BrowserApp.getAppComponent().inject(this);
        mContext = context;
        mDarkTheme = dark || incognito;
        mIsIncognito = incognito;

        refreshPreferences();

        refreshBookmarks();

        mSearchDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_search, mDarkTheme);
        mBookmarkDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_bookmark, mDarkTheme);
        mHistoryDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_history, mDarkTheme);
    }

    public void refreshPreferences() {
        mSuggestionChoice = mPreferenceManager.getSearchSuggestionChoice();
    }

    public void clearCache() {
        Schedulers.io().execute(new ClearCacheRunnable(BrowserApp.get(mContext)));
    }

    public void refreshBookmarks() {
        mAllBookmarks.clear();
        mAllBookmarks.addAll(mBookmarkManager.getAllBookmarks(true));
    }

    @Override
    public int getCount() {
        return mFilteredList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position > mFilteredList.size() || position < 0) {
            return null;
        }
        return mFilteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private static class SuggestionHolder {

        SuggestionHolder(@NonNull View view) {
            mTitle = (TextView) view.findViewById(R.id.title);
            mUrl = (TextView) view.findViewById(R.id.url);
            mImage = (ImageView) view.findViewById(R.id.suggestionIcon);
        }

        final ImageView mImage;
        final TextView mTitle;
        final TextView mUrl;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SuggestionHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.two_line_autocomplete, parent, false);

            holder = new SuggestionHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (SuggestionHolder) convertView.getTag();
        }
        HistoryItem web;
        web = mFilteredList.get(position);
        holder.mTitle.setText(web.getTitle());
        holder.mUrl.setText(web.getUrl());

        if (mDarkTheme) {
            holder.mTitle.setTextColor(Color.WHITE);
        }

        Drawable image;
        switch (web.getImageId()) {
            case R.drawable.ic_bookmark: {
                image = mBookmarkDrawable;
                break;
            }
            case R.drawable.ic_search: {
                image = mSearchDrawable;
                break;
            }
            case R.drawable.ic_history: {
                image = mHistoryDrawable;
                break;
            }
            default:
                image = mSearchDrawable;
                break;
        }

        holder.mImage.setImageDrawable(image);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new SearchFilter(this);
    }

    private synchronized void publishResults(List<HistoryItem> list) {
        mFilteredList.clear();
        mFilteredList.addAll(list);
        notifyDataSetChanged();
    }

    private void clearSuggestions() {
        Observable.create(new Action<Void>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Void> subscriber) {
                mBookmarks.clear();
                mHistory.clear();
                mSuggestions.clear();
                subscriber.onComplete();
            }
        }).subscribeOn(FILTER_SCHEDULER)
            .observeOn(Schedulers.main())
            .subscribe();
    }

    private void combineResults(final @Nullable List<HistoryItem> bookmarkList,
                                final @Nullable List<HistoryItem> historyList,
                                final @Nullable List<HistoryItem> suggestionList) {
        Observable.create(new Action<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> list = new ArrayList<>(5);
                if (bookmarkList != null) {
                    mBookmarks.clear();
                    mBookmarks.addAll(bookmarkList);
                }
                if (historyList != null) {
                    mHistory.clear();
                    mHistory.addAll(historyList);
                }
                if (suggestionList != null) {
                    mSuggestions.clear();
                    mSuggestions.addAll(suggestionList);
                }
                Iterator<HistoryItem> bookmark = mBookmarks.iterator();
                Iterator<HistoryItem> history = mHistory.iterator();
                Iterator<HistoryItem> suggestion = mSuggestions.listIterator();
                while (list.size() < MAX_SUGGESTIONS) {
                    if (!bookmark.hasNext() && !suggestion.hasNext() && !history.hasNext()) {
                        break;
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

                Collections.sort(list, mFilterComparator);
                subscriber.onNext(list);
                subscriber.onComplete();
            }
        }).subscribeOn(FILTER_SCHEDULER)
            .observeOn(Schedulers.main())
            .subscribe(new OnSubscribe<List<HistoryItem>>() {
                @Override
                public void onNext(@Nullable List<HistoryItem> item) {
                    publishResults(item);
                }
            });

    }

    @NonNull
    private Observable<List<HistoryItem>> getBookmarksForQuery(@NonNull final String query) {
        return Observable.create(new Action<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> bookmarks = new ArrayList<>(5);
                int counter = 0;
                for (int n = 0; n < mAllBookmarks.size(); n++) {
                    if (counter >= 5) {
                        break;
                    }
                    if (mAllBookmarks.get(n).getTitle().toLowerCase(Locale.getDefault())
                        .startsWith(query)) {
                        bookmarks.add(mAllBookmarks.get(n));
                        counter++;
                    } else if (mAllBookmarks.get(n).getUrl().contains(query)) {
                        bookmarks.add(mAllBookmarks.get(n));
                        counter++;
                    }
                }
                subscriber.onNext(bookmarks);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    private Observable<List<HistoryItem>> getSuggestionsForQuery(@NonNull final String query) {
        if (mSuggestionChoice == PreferenceManager.Suggestion.SUGGESTION_GOOGLE) {
            return SuggestionsManager.getObservable(query, mContext, SuggestionsManager.Source.GOOGLE);
        } else if (mSuggestionChoice == PreferenceManager.Suggestion.SUGGESTION_DUCK) {
            return SuggestionsManager.getObservable(query, mContext, SuggestionsManager.Source.DUCK);
        } else {
            return Observable.create(new Action<List<HistoryItem>>() {
                @Override
                public void onSubscribe(@NonNull Subscriber<List<HistoryItem>> subscriber) {
                    //TODO add an Observable.empty() method to generate an empty Observable
                    subscriber.onComplete();
                }
            });
        }
    }

    @NonNull
    private Observable<List<HistoryItem>> getHistoryForQuery(@NonNull final String query) {
        return Observable.create(new Action<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<List<HistoryItem>> subscriber) {
                List<HistoryItem> historyList = mDatabaseHandler.findItemsContaining(query);
                subscriber.onNext(historyList);
                subscriber.onComplete();
            }
        });
    }

    private boolean shouldRequestNetwork() {
        return !mIsIncognito && mSuggestionChoice != PreferenceManager.Suggestion.SUGGESTION_NONE;
    }

    private static class SearchFilter extends Filter {

        @NonNull private final SuggestionsAdapter mSuggestionsAdapter;

        SearchFilter(@NonNull SuggestionsAdapter suggestionsAdapter) {
            mSuggestionsAdapter = suggestionsAdapter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                mSuggestionsAdapter.clearSuggestions();
                return results;
            }
            String query = constraint.toString().toLowerCase(Locale.getDefault()).trim();

            if (mSuggestionsAdapter.shouldRequestNetwork() && !SuggestionsManager.isRequestInProgress()) {
                mSuggestionsAdapter.getSuggestionsForQuery(query)
                    .subscribeOn(Schedulers.worker())
                    .observeOn(Schedulers.main())
                    .subscribe(new OnSubscribe<List<HistoryItem>>() {
                        @Override
                        public void onNext(@Nullable List<HistoryItem> item) {
                            mSuggestionsAdapter.combineResults(null, null, item);
                        }
                    });
            }

            mSuggestionsAdapter.getBookmarksForQuery(query)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(new OnSubscribe<List<HistoryItem>>() {
                    @Override
                    public void onNext(@Nullable List<HistoryItem> item) {
                        mSuggestionsAdapter.combineResults(item, null, null);
                    }
                });

            mSuggestionsAdapter.getHistoryForQuery(query)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(new OnSubscribe<List<HistoryItem>>() {
                    @Override
                    public void onNext(@Nullable List<HistoryItem> item) {
                        mSuggestionsAdapter.combineResults(null, item, null);
                    }
                });
            results.count = 1;
            return results;
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return ((HistoryItem) resultValue).getUrl();
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mSuggestionsAdapter.combineResults(null, null, null);
        }
    }

    private static class ClearCacheRunnable implements Runnable {

        @NonNull
        private final Application app;

        ClearCacheRunnable(@NonNull Application app) {
            this.app = app;
        }

        @Override
        public void run() {
            File dir = new File(app.getCacheDir().toString());
            String[] fileList = dir.list(new NameFilter());
            long earliestTimeAllowed = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
            for (String fileName : fileList) {
                File file = new File(dir.getPath() + fileName);
                if (earliestTimeAllowed > file.lastModified()) {
                    file.delete();
                }
            }
        }

        private static class NameFilter implements FilenameFilter {

            @Override
            public boolean accept(File dir, @NonNull String filename) {
                return filename.endsWith(CACHE_FILE_TYPE);
            }
        }
    }

    private static class SuggestionsComparator implements Comparator<HistoryItem> {

        @Override
        public int compare(@NonNull HistoryItem lhs, @NonNull HistoryItem rhs) {
            if (lhs.getImageId() == rhs.getImageId()) return 0;
            if (lhs.getImageId() == R.drawable.ic_bookmark) return -1;
            if (rhs.getImageId() == R.drawable.ic_bookmark) return 1;
            if (lhs.getImageId() == R.drawable.ic_history) return -1;
            return 1;
        }
    }
}
