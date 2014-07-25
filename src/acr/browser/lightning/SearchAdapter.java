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
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

	private DatabaseHandler mDatabaseHandler;

	private SharedPreferences mPreferences;

	private boolean mUseGoogle = true;

	private Context mContext;

	private boolean mIncognito;

	public SearchAdapter(Context context, boolean incognito) {
		mDatabaseHandler = new DatabaseHandler(context);
		mFilteredList = new ArrayList<HistoryItem>();
		mHistory = new ArrayList<HistoryItem>();
		mBookmarks = new ArrayList<HistoryItem>();
		mSuggestions = new ArrayList<HistoryItem>();
		mAllBookmarks = Utils.getBookmarks(context);
		mPreferences = context.getSharedPreferences(
				PreferenceConstants.PREFERENCES, 0);
		mUseGoogle = mPreferences.getBoolean(
				PreferenceConstants.GOOGLE_SEARCH_SUGGESTIONS, true);
		mContext = context;
		mIncognito = incognito;
	}

	public void refreshPreferences() {
		mUseGoogle = mPreferences.getBoolean(
				PreferenceConstants.GOOGLE_SEARCH_SUGGESTIONS, true);
		if (!mUseGoogle && mSuggestions != null) {
			mSuggestions.clear();
		}
	}

	public void refreshBookmarks() {
		mAllBookmarks = Utils.getBookmarks(mContext);
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
			row = inflater.inflate(R.layout.two_line_autocomplete, parent,
					false);

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

		holder.mImage.setImageDrawable(mContext.getResources().getDrawable(
				imageId));

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
			String query = constraint.toString().toLowerCase(
					Locale.getDefault());
			if (query == null) {
				return results;
			}
			if (mUseGoogle && !mIncognito) {
				new RetrieveSearchSuggestions().execute(query);
			}

			List<HistoryItem> filter = new ArrayList<HistoryItem>();

			int counter = 0;
			mBookmarks = new ArrayList<HistoryItem>();
			for (int n = 0; n < mAllBookmarks.size(); n++) {
				if (counter >= 5) {
					break;
				}
				if (mAllBookmarks.get(n).getTitle()
						.toLowerCase(Locale.getDefault()).startsWith(query)) {
					filter.add(mAllBookmarks.get(n));
					mBookmarks.add(mAllBookmarks.get(n));
					counter++;
				}

			}
			if (mDatabaseHandler == null || !mDatabaseHandler.isOpen()) {
				mDatabaseHandler = new DatabaseHandler(mContext);
			}
			mHistory = mDatabaseHandler.findItemsContaining(constraint
					.toString());
			for (int n = 0; n < mHistory.size(); n++) {
				if (n >= 5) {
					break;
				}
				filter.add(mHistory.get(n));
			}

			for (int n = 0; n < mSuggestions.size(); n++) {
				if (filter.size() >= 5) {
					break;
				}
				filter.add(mSuggestions.get(n));
			}

			results.count = filter.size();
			results.values = filter;
			return results;
		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return ((HistoryItem) resultValue).getUrl();
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			synchronized (mFilteredList) {
				mFilteredList = getSuggestions();
				notifyDataSetChanged();
			}
		}

	}

	private class SuggestionHolder {

		ImageView mImage;

		TextView mTitle;

		TextView mUrl;
	}

	private class RetrieveSearchSuggestions extends
			AsyncTask<String, Void, List<HistoryItem>> {

		@Override
		protected List<HistoryItem> doInBackground(String... arg0) {
			if (!isNetworkConnected(mContext)) {
				return new ArrayList<HistoryItem>();
			}
			List<HistoryItem> filter = new ArrayList<HistoryItem>();
			String query = arg0[0];
			try {
				query = query.replace(" ", "+");
				URLEncoder.encode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			InputStream download = null;
			try {
				try {
					download = new java.net.URL(
							"http://google.com/complete/search?q=" + query
									+ "&output=toolbar&hl=en").openStream();
					XmlPullParserFactory factory = XmlPullParserFactory
							.newInstance();
					factory.setNamespaceAware(true);
					XmlPullParser xpp = factory.newPullParser();
					xpp.setInput(download, "iso-8859-1");
					int eventType = xpp.getEventType();
					int counter = 0;
					while (eventType != XmlPullParser.END_DOCUMENT) {
						if (eventType == XmlPullParser.START_TAG) {
							if ("suggestion".equals(xpp.getName())) {
								String suggestion = xpp.getAttributeValue(null,
										"data");
								filter.add(new HistoryItem(mContext
										.getString(R.string.suggestion)
										+ " \""
										+ suggestion + '"', suggestion,
										R.drawable.ic_search));
								counter++;
								if (counter >= 5) {
									break;
								}
							}
						}
						eventType = xpp.next();
					}
				} finally {
					if (download != null) {
						download.close();
					}
				}
			} catch (FileNotFoundException e) {
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			} catch (XmlPullParserException e) {
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

		int suggestionsSize = 0;
		int historySize = 0;
		int bookmarkSize = 0;

		if (mSuggestions != null) {
			suggestionsSize = mSuggestions.size();
		}
		if (mHistory != null) {
			historySize = mHistory.size();
		}
		if (mBookmarks != null) {
			bookmarkSize = mBookmarks.size();
		}

		int maxSuggestions = 2;
		int maxHistory = 1;
		int maxBookmarks = 2;

		if (!mUseGoogle || mIncognito) {
			maxHistory++;
			maxBookmarks++;
		}

		if (bookmarkSize + historySize < 3) {
			maxSuggestions = 5 - (bookmarkSize + historySize);
		} else if (bookmarkSize < 2) {
			maxSuggestions += 2 - bookmarkSize;
		} else if (historySize < 1) {
			maxSuggestions += 1;
		}
		if (suggestionsSize + bookmarkSize < 4) {
			maxHistory = 5 - (suggestionsSize + bookmarkSize);
		}
		if (suggestionsSize + historySize < 3) {
			maxBookmarks = 5 - (suggestionsSize + historySize);
		}

		for (int n = 0; n < bookmarkSize; n++) {
			if (n >= maxBookmarks) {
				break;
			}
			filteredList.add(mBookmarks.get(n));
		}

		for (int n = 0; n < historySize; n++) {
			if (n >= maxHistory) {
				break;
			}
			filteredList.add(mHistory.get(n));
		}

		for (int n = 0; n < suggestionsSize; n++) {
			if (n >= maxSuggestions) {
				break;
			}
			filteredList.add(mSuggestions.get(n));
		}

		return filteredList;
	}
}
