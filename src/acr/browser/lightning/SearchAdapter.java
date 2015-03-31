package acr.browser.lightning;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.Theme;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchAdapter extends BaseAdapter implements Filterable {

	private List<HistoryItem> mHistory;
	private List<HistoryItem> mBookmarks;
	private List<HistoryItem> mSuggestions;
	private List<HistoryItem> mFilteredList;
	private List<HistoryItem> mAllBookmarks;
	private HistoryDatabase mDatabaseHandler;
	private SharedPreferences mPreferences;
	private Context mContext;
	private boolean mUseGoogle = true;
	private boolean mIsExecuting = false;
	private boolean mDarkTheme;
	private BookmarkManager mBookmarkManager;
	private static final String ENCODING = "ISO-8859-1";
	private static final long INTERVAL_DAY = 86400000;
	private XmlPullParserFactory mFactory;
	private XmlPullParser mXpp;
	private String mSearchSubtitle;
	private static final int API = Build.VERSION.SDK_INT;
	private Theme mTheme;

	public SearchAdapter(Context context, boolean dark) {
		mDatabaseHandler = HistoryDatabase.getInstance(context);
		mTheme = context.getTheme();
		mFilteredList = new ArrayList<HistoryItem>();
		mHistory = new ArrayList<HistoryItem>();
		mBookmarks = new ArrayList<HistoryItem>();
		mSuggestions = new ArrayList<HistoryItem>();
		mBookmarkManager = new BookmarkManager(context);
		mAllBookmarks = mBookmarkManager.getBookmarks(true);
		mPreferences = context.getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mUseGoogle = mPreferences.getBoolean(PreferenceConstants.GOOGLE_SEARCH_SUGGESTIONS, true);
		mContext = context;
		mSearchSubtitle = mContext.getString(R.string.suggestion);
		mDarkTheme = dark;
		Thread delete = new Thread(new Runnable() {

			@Override
			public void run() {
				deleteOldCacheFiles(mContext);
			}

		});
		delete.start();
	}

	private void deleteOldCacheFiles(Context context) {
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

	private class NameFilter implements FilenameFilter {

		private static final String ext = ".sgg";

		@Override
		public boolean accept(File dir, String filename) {
			return filename.endsWith(ext);
		}

	}

	public void refreshPreferences() {
		mUseGoogle = mPreferences.getBoolean(PreferenceConstants.GOOGLE_SEARCH_SUGGESTIONS, true);
		if (!mUseGoogle && mSuggestions != null) {
			mSuggestions.clear();
		}
	}

	public void refreshBookmarks() {
		mAllBookmarks = mBookmarkManager.getBookmarks(true);
	}

	@Override
	public int getCount() {
		if (mFilteredList != null) {
			return mFilteredList.size();
		} else {
			return 0;
		}
	}

	@Override
	public Object getItem(int position) {
		return mFilteredList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		SuggestionHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			row = inflater.inflate(R.layout.two_line_autocomplete, parent, false);

			holder = new SuggestionHolder();
			holder.mTitle = (TextView) row.findViewById(R.id.title);
			holder.mUrl = (TextView) row.findViewById(R.id.url);
			holder.mImage = (ImageView) row.findViewById(R.id.suggestionIcon);
			row.setTag(holder);
		} else {
			holder = (SuggestionHolder) row.getTag();
		}

		HistoryItem web = mFilteredList.get(position);
		holder.mTitle.setText(web.getTitle());
		holder.mUrl.setText(web.getUrl());

		int imageId = R.drawable.ic_bookmark;
		switch (web.getImageId()) {
			case R.drawable.ic_bookmark: {
				if (!mDarkTheme) {
					imageId = R.drawable.ic_bookmark;
				} else {
					holder.mTitle.setTextColor(Color.WHITE);
					imageId = R.drawable.ic_bookmark_dark;
				}
				break;
			}
			case R.drawable.ic_search: {
				if (!mDarkTheme) {
					imageId = R.drawable.ic_search;
				} else {
					holder.mTitle.setTextColor(Color.WHITE);
					imageId = R.drawable.ic_search_dark;
				}
				break;
			}
			case R.drawable.ic_history: {
				if (!mDarkTheme) {
					imageId = R.drawable.ic_history;
				} else {
					holder.mTitle.setTextColor(Color.WHITE);
					imageId = R.drawable.ic_history_dark;
				}
				break;
			}
		}

		if (API < Build.VERSION_CODES.LOLLIPOP) {
			holder.mImage.setImageDrawable(mContext.getResources().getDrawable(imageId));
		} else {
			holder.mImage.setImageDrawable(mContext.getResources().getDrawable(imageId, mTheme));
		}

		return row;
	}

	public void setContents(List<HistoryItem> list) {
		if (mFilteredList != null) {
			mFilteredList.clear();
			mFilteredList.addAll(list);
		} else {
			mFilteredList = list;
		}
		notifyDataSetChanged();
	}

	@Override
	public Filter getFilter() {
		return new SearchFilter();
	}

	public class SearchFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.toString() == null) {
				return results;
			}
			String query = constraint.toString().toLowerCase(Locale.getDefault());
			if (mUseGoogle && !mDarkTheme && !mIsExecuting) {
				new RetrieveSearchSuggestions().execute(query);
			}

			int counter = 0;
			mBookmarks = new ArrayList<HistoryItem>();
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
			if (mDatabaseHandler == null) {
				mDatabaseHandler = HistoryDatabase.getInstance(mContext);
			}
			mHistory = mDatabaseHandler.findItemsContaining(constraint.toString());

			results.count = 1;
			return results;
		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return ((HistoryItem) resultValue).getUrl();
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			mFilteredList = getSuggestions();
			notifyDataSetChanged();
		}

	}

	private class SuggestionHolder {

		ImageView mImage;

		TextView mTitle;

		TextView mUrl;
	}

	private class RetrieveSearchSuggestions extends AsyncTask<String, Void, List<HistoryItem>> {

		@Override
		protected List<HistoryItem> doInBackground(String... arg0) {
			mIsExecuting = true;

			List<HistoryItem> filter = new ArrayList<HistoryItem>();
			String query = arg0[0];
			try {
				query = query.replace(" ", "+");
				URLEncoder.encode(query, ENCODING);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			File cache = downloadSuggestionsForQuery(query);
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
				if (fileInput != null) {
					try {
						fileInput.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return filter;
		}

		@Override
		protected void onPostExecute(List<HistoryItem> result) {
			mSuggestions = result;
			mFilteredList = getSuggestions();
			notifyDataSetChanged();
			mIsExecuting = false;
		}

	}

	private File downloadSuggestionsForQuery(String query) {
		File cacheFile = new File(mContext.getCacheDir(), query.hashCode() + ".sgg");
		if (System.currentTimeMillis() - INTERVAL_DAY < cacheFile.lastModified()) {
			return cacheFile;
		}
		if (!isNetworkConnected(mContext)) {
			return cacheFile;
		}
		try {
			URL url = new URL("http://google.com/complete/search?q=" + query
					+ "&output=toolbar&hl=en");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream in = connection.getInputStream();

			if (in != null) {
				FileOutputStream fos = new FileOutputStream(cacheFile);
				int buffer;
				while ((buffer = in.read()) != -1) {
					fos.write(buffer);
				}
				fos.flush();
				fos.close();
			}
			cacheFile.setLastModified(System.currentTimeMillis());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cacheFile;
	}

	private boolean isNetworkConnected(Context context) {
		NetworkInfo networkInfo = getActiveNetworkInfo(context);
		return networkInfo != null && networkInfo.isConnected();
	}

	private NetworkInfo getActiveNetworkInfo(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			return null;
		}
		return connectivity.getActiveNetworkInfo();
	}

	public List<HistoryItem> getSuggestions() {
		List<HistoryItem> filteredList = new ArrayList<HistoryItem>();

		int suggestionsSize = (mSuggestions == null) ? 0 : mSuggestions.size();
		int historySize = (mHistory == null) ? 0 : mHistory.size();
		int bookmarkSize = (mBookmarks == null) ? 0 : mBookmarks.size();

		int maxSuggestions = (bookmarkSize + historySize < 3) ? (5 - bookmarkSize - historySize)
				: (bookmarkSize < 2) ? (4 - bookmarkSize) : (historySize < 1) ? 3 : 2;
		int maxHistory = (suggestionsSize + bookmarkSize < 4) ? (5 - suggestionsSize - bookmarkSize)
				: 1;
		int maxBookmarks = (suggestionsSize + historySize < 3) ? (5 - suggestionsSize - historySize)
				: 2;

		if (!mUseGoogle || mDarkTheme) {
			maxHistory++;
			maxBookmarks++;
		}

		for (int n = 0; n < bookmarkSize && n < maxBookmarks; n++) {
			filteredList.add(mBookmarks.get(n));
		}

		for (int n = 0; n < historySize && n < maxHistory; n++) {
			filteredList.add(mHistory.get(n));
		}

		for (int n = 0; n < suggestionsSize && n < maxSuggestions; n++) {
			filteredList.add(mSuggestions.get(n));
		}
		return filteredList;
	}

}
