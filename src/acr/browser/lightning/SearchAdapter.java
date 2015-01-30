package acr.browser.lightning;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchAdapter extends BaseAdapter implements Filterable {

	private List<HistoryItem> mHistory;
	private List<HistoryItem> mBookmarks;
	private List<HistoryItem> mSuggestions;
	private List<HistoryItem> mFilteredList;
	private List<HistoryItem> mAllBookmarks;
	private HistoryDatabaseHandler mDatabaseHandler;
	private SharedPreferences mPreferences;
	private boolean mUseGoogle = true;
	private Context mContext;
	private boolean mIncognito;
	private BookmarkManager mBookmarkManager;
	private static final String ENCODING = "ISO-8859-1";
	private XmlPullParserFactory mFactory;
	private XmlPullParser mXpp;
	private String mSearchSubtitle;

	public SearchAdapter(Context context, boolean incognito) {
		mDatabaseHandler = new HistoryDatabaseHandler(context);
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
		mIncognito = incognito;
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
				if (!mIncognito) {
					imageId = R.drawable.ic_bookmark;
				} else {
					holder.mTitle.setTextColor(Color.WHITE);
					imageId = R.drawable.ic_bookmark_dark;
				}
				break;
			}
			case R.drawable.ic_search: {
				if (!mIncognito) {
					imageId = R.drawable.ic_search;
				} else {
					holder.mTitle.setTextColor(Color.WHITE);
					imageId = R.drawable.ic_search_dark;
				}
				break;
			}
			case R.drawable.ic_history: {
				if (!mIncognito) {
					imageId = R.drawable.ic_history;
				} else {
					holder.mTitle.setTextColor(Color.WHITE);
					imageId = R.drawable.ic_history_dark;
				}
				break;
			}
		}

		holder.mImage.setImageDrawable(mContext.getResources().getDrawable(imageId));

		return row;
	}

	public void setContents(List<HistoryItem> list) {
		if (mFilteredList != null) {
			mFilteredList.clear();
			mFilteredList.addAll(list);
		}
	}

	@Override
	public Filter getFilter() {
		return new SearchFilter();
	}

	public class SearchFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null) {
				return results;
			}
			String query = constraint.toString().toLowerCase(Locale.getDefault());
			if (query == null) {
				return results;
			}
			if (mUseGoogle && !mIncognito) {
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
			if (mDatabaseHandler == null || !mDatabaseHandler.isOpen()) {
				mDatabaseHandler = new HistoryDatabaseHandler(mContext);
			}
			mHistory = mDatabaseHandler.findItemsContaining(constraint.toString());

			mFilteredList = getSuggestions();
			results.count = 1;
			return results;
		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return ((HistoryItem) resultValue).getUrl();
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
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
			if (!isNetworkConnected(mContext)) {
				return new ArrayList<HistoryItem>();
			}
			List<HistoryItem> filter = new ArrayList<HistoryItem>();
			String query = arg0[0];
			try {
				query = query.replace(" ", "+");
				URLEncoder.encode(query, ENCODING);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			InputStream download = null;
			try {
				download = new java.net.URL("http://google.com/complete/search?q=" + query
						+ "&output=toolbar&hl=en").openStream();
				if (mFactory == null) {
					mFactory = XmlPullParserFactory.newInstance();
					mFactory.setNamespaceAware(true);
				}
				if (mXpp == null) {
					mXpp = mFactory.newPullParser();
				}
				mXpp.setInput(download, ENCODING);
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
			} catch (Exception e){
				return filter;
			} finally {
				if (download != null) {
					try {
						download.close();
					} catch (IOException e) {
						return filter;
					}
				}
			}

			return filter;
		}

		@Override
		protected void onPostExecute(List<HistoryItem> result) {
			synchronized (mFilteredList) {
				mSuggestions = result;

				mFilteredList = getSuggestions();
				notifyDataSetChanged();
			}
		}

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

		if (!mUseGoogle || mIncognito) {
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
