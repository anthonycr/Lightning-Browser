/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.provider.Browser;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebViewDatabase;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.VideoView;

public class BrowserActivity extends Activity implements BrowserController {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private RelativeLayout mDrawer;
	private LinearLayout mDrawerRight;
	private ListView mDrawerListRight;
	private TextView mNewTab;
	private ActionBarDrawerToggle mDrawerToggle;
	private List<LightningView> mWebViews = new ArrayList<LightningView>();
	private List<Integer> mIdList = new ArrayList<Integer>();
	private LightningView mCurrentView;
	private int mIdGenerator;
	private LightningViewAdapter mTitleAdapter;
	private List<HistoryItem> mBookmarkList;
	private BookmarkViewAdapter mBookmarkAdapter;
	private AutoCompleteTextView mSearch;
	private ClickHandler mClickHandler;
	private ProgressBar mProgress;
	private boolean mSystemBrowser = false;
	private ValueCallback<Uri> mUploadMessage;
	private View mCustomView;
	private int mOriginalOrientation;
	private int mActionBarSize;
	private ActionBar mActionBar;
	private boolean mFullScreen;
	private FrameLayout mBrowserFrame;
	private FullscreenHolder mFullscreenContainer;
	private CustomViewCallback mCustomViewCallback;
	private final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT);
	private Bitmap mDefaultVideoPoster;
	private View mVideoProgressView;
	private DatabaseHandler mHistoryHandler;
	private SQLiteDatabase mHistoryDatabase;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor mEditPrefs;
	private Context mContext;
	private Bitmap mWebpageBitmap;
	private String mSearchText;
	private Activity mActivity;
	private CookieManager mCookieManager;
	private final int API = android.os.Build.VERSION.SDK_INT;
	private Drawable mDeleteIcon;
	private Drawable mRefreshIcon;
	private Drawable mCopyIcon;
	private Drawable mIcon;
	private int mActionBarSizeDp;
	private int mNumberIconColor;
	private String mHomepage;
	private boolean mIsNewIntent = false;
	private VideoView mVideoView;
	private static SearchAdapter mSearchAdapter;
	private boolean isIncognito = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}

	private synchronized void initialize() {
		setContentView(R.layout.activity_main);
		TypedValue typedValue = new TypedValue();
		Theme theme = getTheme();
		theme.resolveAttribute(R.attr.numberColor, typedValue, true);
		mNumberIconColor = typedValue.data;
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mEditPrefs = mPreferences.edit();
		mContext = this;
		if (mIdList != null) {
			mIdList.clear();
		} else {
			mIdList = new ArrayList<Integer>();
		}
		if (mWebViews != null) {
			mWebViews.clear();
		} else {
			mWebViews = new ArrayList<LightningView>();
		}

		mActivity = this;
		mClickHandler = new ClickHandler(this);
		mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
		mProgress = (ProgressBar) findViewById(R.id.activity_bar);
		mProgress.setVisibility(View.GONE);
		mNewTab = (TextView) findViewById(R.id.new_tab_button);
		mDrawer = (RelativeLayout) findViewById(R.id.drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerRight = (LinearLayout) findViewById(R.id.right_drawer);
		mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);
		mWebpageBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_webpage);
		mActionBar = getActionBar();
		final TypedArray styledAttributes = mContext.getTheme()
				.obtainStyledAttributes(
						new int[] { android.R.attr.actionBarSize });
		mActionBarSize = (int) styledAttributes.getDimension(0, 0);
		if (pixelsToDp(mActionBarSize) < 48) {
			mActionBarSize = getDp(48);
		}
		mActionBarSizeDp = pixelsToDp(mActionBarSize);
		styledAttributes.recycle();

		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE,
				Constants.HOMEPAGE);

		mTitleAdapter = new LightningViewAdapter(this, R.layout.tab_list_item,
				mWebViews);
		mDrawerList.setAdapter(mTitleAdapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerList
				.setOnItemLongClickListener(new DrawerItemLongClickListener());

		mBookmarkList = getBookmarks();
		mBookmarkAdapter = new BookmarkViewAdapter(this,
				R.layout.bookmark_list_item, mBookmarkList);
		mDrawerListRight.setAdapter(mBookmarkAdapter);
		mDrawerListRight
				.setOnItemClickListener(new BookmarkItemClickListener());
		mDrawerListRight
				.setOnItemLongClickListener(new BookmarkItemLongClickListener());

		if (mHistoryHandler == null) {
			mHistoryHandler = new DatabaseHandler(this);
		} else if (!mHistoryHandler.isOpen()) {
			mHistoryHandler = new DatabaseHandler(this);
		}
		mHistoryDatabase = mHistoryHandler.getReadableDatabase();

		// set display options of the ActionBar
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setDisplayShowHomeEnabled(true);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setCustomView(R.layout.search);

		RelativeLayout back = (RelativeLayout) findViewById(R.id.action_back);
		RelativeLayout forward = (RelativeLayout) findViewById(R.id.action_forward);
		if (back != null) {
			back.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCurrentView != null) {
						if (mCurrentView.canGoBack()) {
							mCurrentView.goBack();
						} else {
							deleteTab(mDrawerList.getCheckedItemPosition());
						}
					}
				}

			});
		}
		if (forward != null) {
			forward.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCurrentView != null) {
						if (mCurrentView.canGoForward()) {
							mCurrentView.goForward();
						}
					}
				}

			});
		}

		// create the search EditText in the ActionBar
		mSearch = (AutoCompleteTextView) mActionBar.getCustomView()
				.findViewById(R.id.search);
		mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete);
		mDeleteIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 24),
				Utils.convertToDensityPixels(mContext, 24));
		mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh);
		mRefreshIcon.setBounds(0, 0,
				Utils.convertToDensityPixels(mContext, 24),
				Utils.convertToDensityPixels(mContext, 24));
		mCopyIcon = getResources().getDrawable(R.drawable.ic_action_copy);
		mCopyIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 24),
				Utils.convertToDensityPixels(mContext, 24));
		mIcon = mRefreshIcon;
		mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
		mSearch.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

				switch (arg1) {
				case KeyEvent.KEYCODE_ENTER:
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
					searchTheWeb(mSearch.getText().toString());
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
					return true;
				default:
					break;
				}
				return false;
			}

		});
		mSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && mCurrentView != null) {
					if (mCurrentView != null) {
						if (mCurrentView.getProgress() < 100) {
							setIsLoading();
						} else {
							setIsFinishedLoading();
						}
					}
					updateUrl(mCurrentView.getUrl());
				} else if (hasFocus) {
					mIcon = mCopyIcon;
					mSearch.setCompoundDrawables(null, null, mCopyIcon, null);
				}
			}
		});
		mSearch.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView arg0, int actionId,
					KeyEvent arg2) {
				// hide the keyboard and search the web when the enter key
				// button is pressed
				if (actionId == EditorInfo.IME_ACTION_GO
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT
						|| actionId == EditorInfo.IME_ACTION_SEND
						|| actionId == EditorInfo.IME_ACTION_SEARCH
						|| (arg2.getAction() == KeyEvent.KEYCODE_ENTER)) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
					searchTheWeb(mSearch.getText().toString());
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
					return true;
				}
				return false;
			}

		});

		mSearch.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mSearch.getCompoundDrawables()[2] != null) {
					boolean tappedX = event.getX() > (mSearch.getWidth()
							- mSearch.getPaddingRight() - mIcon
							.getIntrinsicWidth());
					if (tappedX) {
						if (event.getAction() == MotionEvent.ACTION_UP) {
							if (mSearch.hasFocus()) {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label",
										mSearch.getText().toString());
								clipboard.setPrimaryClip(clip);
								Utils.showToast(
										mContext,
										mContext.getResources().getString(
												R.string.message_text_copied));
							} else {
								refreshOrStop();
							}
						}
						return true;
					}
				}
				return false;
			}

		});

		mSystemBrowser = getSystemBrowser();
		Thread initialize = new Thread(new Runnable() {

			@Override
			public void run() {
				initializeSearchSuggestions(mSearch);
			}

		});
		initialize.run();
		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				if (view.equals(mDrawer)) {
					mDrawerLayout.setDrawerLockMode(
							DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerRight);
				} else if (view.equals(mDrawerRight)) {
					mDrawerLayout.setDrawerLockMode(
							DrawerLayout.LOCK_MODE_UNLOCKED, mDrawer);
				}
			}

			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				if (drawerView.equals(mDrawer)) {
					mDrawerLayout.closeDrawer(mDrawerRight);
					mDrawerLayout.setDrawerLockMode(
							DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerRight);
				} else if (drawerView.equals(mDrawerRight)) {
					mDrawerLayout.closeDrawer(mDrawer);
					mDrawerLayout.setDrawerLockMode(
							DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawer);
				}
			}

		};

		mNewTab.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				newTab(null, true);
			}

		});

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		initializePreferences();
		initializeTabs();

		if (API < 19) {
			WebIconDatabase.getInstance().open(
					getDir("icons", MODE_PRIVATE).getPath());
		}

		checkForTor();

	}

	/*
	 * If Orbot/Tor is installed, prompt the user if they want to enable
	 * proxying for this session
	 */
	public boolean checkForTor() {
		boolean useProxy = mPreferences.getBoolean(
				PreferenceConstants.USE_PROXY, false);

		OrbotHelper oh = new OrbotHelper(this);
		if (oh.isOrbotInstalled()
				&& !mPreferences.getBoolean(
						PreferenceConstants.INITIAL_CHECK_FOR_TOR, false)) {
			mEditPrefs.putBoolean(PreferenceConstants.INITIAL_CHECK_FOR_TOR,
					true);
			mEditPrefs.apply();
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						mPreferences
								.edit()
								.putBoolean(PreferenceConstants.USE_PROXY, true)
								.apply();

						initializeTor();
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						mPreferences
								.edit()
								.putBoolean(PreferenceConstants.USE_PROXY,
										false).apply();
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.use_tor_prompt)
					.setPositiveButton(R.string.yes, dialogClickListener)
					.setNegativeButton(R.string.no, dialogClickListener).show();

			return true;
		} else if (oh.isOrbotInstalled() & useProxy == true) {
			initializeTor();
			return true;
		} else {
			mEditPrefs.putBoolean(PreferenceConstants.USE_PROXY, false);
			mEditPrefs.apply();
			return false;
		}
	}

	/*
	 * Initialize WebKit Proxying for Tor
	 */
	public void initializeTor() {

		OrbotHelper oh = new OrbotHelper(this);
		if (!oh.isOrbotRunning())
			oh.requestOrbotStart(this);
		
		WebkitProxy wkp = new WebkitProxy();
		try {
			String host = mPreferences.getString(
					PreferenceConstants.USE_PROXY_HOST, "localhost");
			int port = mPreferences.getInt(PreferenceConstants.USE_PROXY_PORT,
					8118);
			wkp.setProxy("acr.browser.lightning.BrowserApp",
					getApplicationContext(), host, port);
		} catch (Exception e) {
			Log.d(Constants.LOGTAG, "error enabling web proxying", e);
		}

	}

	/*
	 * Override this class
	 */
	public synchronized void initializeTabs() {

	}
	
	public void setIsIncognito(boolean letsAsk){
		isIncognito = letsAsk;
	}
	
	public boolean isThisIncognito(){
		return isIncognito;
	}

	public void restoreOrNewTab() {
		mIdGenerator = 0;

		String url = null;
		if (getIntent() != null) {
			url = getIntent().getDataString();
			if (url != null) {
				if (url.startsWith(Constants.FILE)) {
					Utils.showToast(
							this,
							getResources().getString(
									R.string.message_blocked_local));
					url = null;
				}
			}
		}
		if (mPreferences
				.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
			String mem = mPreferences.getString(PreferenceConstants.URL_MEMORY,
					"");
			mEditPrefs.putString(PreferenceConstants.URL_MEMORY, "");
			String[] array = Utils.getArray(mem);
			int count = 0;
			for (int n = 0; n < array.length; n++) {
				if (array[n].length() > 0) {
					newTab(array[n], true);
					count++;
				}
			}
			if (url != null) {
				newTab(url, true);
			} else if (count == 0) {
				newTab(null, true);
			}
		} else {
			newTab(url, true);
		}
	}

	public void initializePreferences() {
		if (mPreferences == null) {
			mPreferences = getSharedPreferences(
					PreferenceConstants.PREFERENCES, 0);
		}
		mFullScreen = mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN,
				false);
		if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
		case 0:
			mSearchText = mPreferences.getString(
					PreferenceConstants.SEARCH_URL, Constants.GOOGLE_SEARCH);
			if (!mSearchText.startsWith(Constants.HTTP)
					&& !mSearchText.startsWith(Constants.HTTPS)) {
				mSearchText = Constants.GOOGLE_SEARCH;
			}
			break;
		case 1:
			mSearchText = Constants.GOOGLE_SEARCH;
			break;
		case 2:
			mSearchText = Constants.ANDROID_SEARCH;
			break;
		case 3:
			mSearchText = Constants.BING_SEARCH;
			break;
		case 4:
			mSearchText = Constants.YAHOO_SEARCH;
			break;
		case 5:
			mSearchText = Constants.STARTPAGE_SEARCH;
			break;
		case 6:
			mSearchText = Constants.STARTPAGE_MOBILE_SEARCH;
			break;
		case 7:
			mSearchText = Constants.DUCK_SEARCH;
			break;
		case 8:
			mSearchText = Constants.DUCK_LITE_SEARCH;
			break;
		case 9:
			mSearchText = Constants.BAIDU_SEARCH;
			break;
		case 10:
			mSearchText = Constants.YANDEX_SEARCH;
			break;
		}

		updateCookiePreference();
		if (mPreferences.getBoolean(PreferenceConstants.USE_PROXY, false)) {
			initializeTor();
		} else {
			try {
				WebkitProxy.resetProxy("acr.browser.lightning.BrowserApp",
						getApplicationContext());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * Override this if class overrides BrowserActivity
	 */
	public void updateCookiePreference() {

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (mSearch.hasFocus()) {
				searchTheWeb(mSearch.getText().toString());
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action buttons
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
				mDrawerLayout.closeDrawer(mDrawerRight);
			}
			mDrawerToggle.syncState();
			return true;
		case R.id.action_back:
			if (mCurrentView != null) {
				if (mCurrentView.canGoBack())
					mCurrentView.goBack();
			}
			return true;
		case R.id.action_forward:
			if (mCurrentView != null) {
				if (mCurrentView.canGoForward())
					mCurrentView.goForward();
			}
			return true;
		case R.id.action_new_tab:
			newTab(null, true);
			return true;
		case R.id.action_incognito:
			startActivity(new Intent(Constants.INCOGNITO_INTENT));
			return true;
		case R.id.action_share:
			if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
				Intent shareIntent = new Intent(
						android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
						mCurrentView.getTitle());
				String shareMessage = mCurrentView.getUrl();
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						shareMessage);
				startActivity(Intent.createChooser(shareIntent, getResources()
						.getString(R.string.dialog_title_share)));
			}
			return true;
		case R.id.action_bookmarks:
			openBookmarks();
			return true;
		case R.id.action_copy:
			if (mCurrentView != null) {
				if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("label", mCurrentView
							.getUrl().toString());
					clipboard.setPrimaryClip(clip);
					Utils.showToast(mContext, mContext.getResources()
							.getString(R.string.message_link_copied));
				}
			}
			return true;
		case R.id.action_settings:
			startActivity(new Intent(Constants.SETTINGS_INTENT));
			return true;
		case R.id.action_history:
			openHistory();
			return true;
		case R.id.action_add_bookmark:
			if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
				addBookmark(this, mCurrentView.getTitle(),
						mCurrentView.getUrl());
			}
			return true;
		case R.id.action_find:
			findInPage();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * refreshes the underlying list of the Bookmark adapter since the bookmark
	 * adapter doesn't always change when notifyDataChanged gets called.
	 */
	private void notifyBookmarkDataSetChanged() {
		mBookmarkAdapter.clear();
		mBookmarkAdapter.addAll(mBookmarkList);
		mBookmarkAdapter.notifyDataSetChanged();
	}

	/**
	 * method that shows a dialog asking what string the user wishes to search
	 * for. It highlights the text entered.
	 */
	private void findInPage() {
		final AlertDialog.Builder finder = new AlertDialog.Builder(mActivity);
		finder.setTitle(getResources().getString(R.string.action_find));
		final EditText getHome = new EditText(this);
		getHome.setHint(getResources().getString(R.string.search_hint));
		finder.setView(getHome);
		finder.setPositiveButton(
				getResources().getString(R.string.search_hint),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getHome.getText().toString();
						if (mCurrentView != null) {
							mCurrentView.find(text);
						}
					}
				});
		finder.show();
	}

	/** The click listener for ListView in the navigation drawer */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mIsNewIntent = false;
			selectItem(position);
		}
	}

	/** long click listener for Navigation Drawer */
	private class DrawerItemLongClickListener implements
			ListView.OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int position, long arg3) {
			deleteTab(position);
			return false;
		}
	}

	private class BookmarkItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (mCurrentView != null) {
				mCurrentView.loadUrl(mBookmarkList.get(position).getUrl());
			}
			mDrawerLayout.closeDrawer(mDrawerRight);
		}
	}

	private class BookmarkItemLongClickListener implements
			ListView.OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				final int position, long arg3) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mContext.getResources().getString(
					R.string.action_bookmarks));
			builder.setMessage(
					getResources().getString(R.string.dialog_bookmark))
					.setCancelable(true)
					.setPositiveButton(
							getResources().getString(R.string.action_new_tab),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									newTab(mBookmarkList.get(position).getUrl(),
											false);
									mDrawerLayout.closeDrawers();
								}
							})
					.setNegativeButton(
							getResources().getString(R.string.action_delete),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									deleteBookmark(mBookmarkList.get(position)
											.getUrl());
								}
							})
					.setNeutralButton(
							getResources().getString(R.string.action_edit),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									editBookmark(position);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
	}

	/**
	 * Takes in the id of which bookmark was selected and shows a dialog that
	 * allows the user to rename and change the url of the bookmark
	 * 
	 * @param id
	 *            which id in the list was chosen
	 */
	public synchronized void editBookmark(final int id) {
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(
				mActivity);
		homePicker.setTitle(getResources().getString(
				R.string.title_edit_bookmark));
		final EditText getTitle = new EditText(mContext);
		getTitle.setHint(getResources().getString(R.string.hint_title));
		getTitle.setText(mBookmarkList.get(id).getTitle());
		getTitle.setSingleLine();
		final EditText getUrl = new EditText(mContext);
		getUrl.setHint(getResources().getString(R.string.hint_url));
		getUrl.setText(mBookmarkList.get(id).getUrl());
		getUrl.setSingleLine();
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(getTitle);
		layout.addView(getUrl);
		homePicker.setView(layout);
		homePicker.setPositiveButton(
				getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBookmarkList.get(id).setTitle(
								getTitle.getText().toString());
						mBookmarkList.get(id).setUrl(
								getUrl.getText().toString());
						notifyBookmarkDataSetChanged();
						File book = new File(getFilesDir(), "bookmarks");
						File bookUrl = new File(getFilesDir(), "bookurl");
						try {
							BufferedWriter bookWriter = new BufferedWriter(
									new FileWriter(book));
							BufferedWriter urlWriter = new BufferedWriter(
									new FileWriter(bookUrl));
							Iterator<HistoryItem> iter = mBookmarkList
									.iterator();
							HistoryItem item;
							while (iter.hasNext()) {
								item = iter.next();

								bookWriter.write(item.getTitle());
								urlWriter.write(item.getUrl());
								bookWriter.newLine();
								urlWriter.newLine();

							}

							bookWriter.close();
							urlWriter.close();
						} catch (FileNotFoundException e) {
						} catch (IOException e) {
						}
						Collections.sort(mBookmarkList, new SortIgnoreCase());
						notifyBookmarkDataSetChanged();
						if (mCurrentView != null) {
							if (mCurrentView.getUrl()
									.startsWith(Constants.FILE)
									&& mCurrentView.getUrl().endsWith(
											"bookmarks.html")) {
								openBookmarkPage(mCurrentView.getWebView());
							}
						}
					}
				});
		homePicker.show();
	}

	/**
	 * displays the WebView contained in the LightningView Also handles the
	 * removal of previous views
	 * 
	 * @param view
	 *            the LightningView to show
	 */
	private synchronized void showTab(LightningView view) {
		if (view == null) {
			return;
		}
		if (mCurrentView != null) {
			mCurrentView.onPause();
		}
		mCurrentView = view;

		if (view.getWebView() != null) {
			updateUrl(view.getUrl());
			updateProgress(view.getProgress());
		} else {
			updateUrl("");
			updateProgress(0);
		}

		mBrowserFrame.removeAllViews();
		mCurrentView.onResume();
		mBrowserFrame.addView(view.getWebView());
	}

	/**
	 * creates a new tab with the passed in URL if it isn't null
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	public void handleNewIntent(Intent intent) {
		if (mCurrentView == null) {
			initialize();
		}

		String url = null;
		if (intent != null) {
			url = intent.getDataString();
		}
		int num = 0;
		if (intent != null && intent.getExtras() != null)
			num = intent.getExtras().getInt(getPackageName() + ".Origin");
		if (num == 1) {
			mCurrentView.loadUrl(url);
		} else if (url != null) {
			if (url.startsWith(Constants.FILE)) {
				Utils.showToast(this,
						getResources()
								.getString(R.string.message_blocked_local));
				url = null;
			}
			newTab(url, true);
			mIsNewIntent = true;
		}
	}

	private void selectItem(int position) {
		// update selected item and title, then close the drawer

		mDrawerList.setItemChecked(position, true);
		showTab(mWebViews.get(position));

		mDrawerLayout.closeDrawer(mDrawer);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	protected synchronized void newTab(String url, boolean show) {
		mIsNewIntent = false;
		LightningView startingTab = new LightningView(mActivity, url,
				mCookieManager);
		if (mIdGenerator == 0) {
			startingTab.resumeTimers();
		}
		mIdList.add(mIdGenerator);
		mIdGenerator++;
		mWebViews.add(startingTab);

		Drawable icon = writeOnDrawable(mWebViews.size());
		mActionBar.setIcon(icon);
		mTitleAdapter.notifyDataSetChanged();
		if (show) {
			mDrawerList.setItemChecked(mWebViews.size() - 1, true);
			showTab(startingTab);
		}
	}

	private synchronized void deleteTab(int position) {
		if (position >= mWebViews.size()) {
			return;
		}

		int current = mDrawerList.getCheckedItemPosition();
		LightningView reference = mWebViews.get(position);
		if (reference == null) {
			return;
		}
		boolean isShown = reference.isShown();
		if (current > position) {
			mIdList.remove(position);
			mWebViews.remove(position);
			mDrawerList.setItemChecked(current - 1, true);
			reference.onDestroy();
		} else if (mWebViews.size() > position + 1) {
			mIdList.remove(position);
			showTab(mWebViews.get(position + 1));
			mWebViews.remove(position);
			mDrawerList.setItemChecked(position, true);
			reference.onDestroy();
		} else if (mWebViews.size() > 1) {
			mIdList.remove(position);
			showTab(mWebViews.get(position - 1));
			mWebViews.remove(position);
			mDrawerList.setItemChecked(position - 1, true);
			reference.onDestroy();
		} else {
			if (mCurrentView.getUrl().startsWith(Constants.FILE)
					|| mCurrentView.getUrl().equals(mHomepage)) {
				closeActivity();
			} else {
				mIdList.remove(position);
				mWebViews.remove(position);
				if (mPreferences.getBoolean(
						PreferenceConstants.CLEAR_CACHE_EXIT, false)
						&& mCurrentView != null && !isThisIncognito()) {
					mCurrentView.clearCache(true);
					Log.i(Constants.LOGTAG, "Cache Cleared");

				}
				if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT,
						false) && !isThisIncognito()) {
					clearHistory();
					Log.i(Constants.LOGTAG, "History Cleared");

				}
				if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT,
						false) && !isThisIncognito()) {
					clearCookies();
					Log.i(Constants.LOGTAG, "Cookies Cleared");

				}
				if (reference != null) {
					reference.pauseTimers();
					reference.onDestroy();
				}
				mCurrentView = null;
				mTitleAdapter.notifyDataSetChanged();
				finish();

			}
		}
		mTitleAdapter.notifyDataSetChanged();
		Drawable icon = writeOnDrawable(mWebViews.size());
		mActionBar.setIcon(icon);

		if (mIsNewIntent && isShown) {
			mIsNewIntent = false;
			closeActivity();
		}

		Log.i(Constants.LOGTAG, "deleted tab");
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT,
					false) && mCurrentView != null && !isThisIncognito()) {
				mCurrentView.clearCache(true);
				Log.i(Constants.LOGTAG, "Cache Cleared");

			}
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT,
					false) && !isThisIncognito()) {
				clearHistory();
				Log.i(Constants.LOGTAG, "History Cleared");

			}
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT,
					false) && !isThisIncognito()) {
				clearCookies();
				Log.i(Constants.LOGTAG, "Cookies Cleared");

			}
			mCurrentView = null;
			for (int n = 0; n < mWebViews.size(); n++) {
				if (mWebViews.get(n) != null)
					mWebViews.get(n).onDestroy();
			}
			mWebViews.clear();
			mTitleAdapter.notifyDataSetChanged();
			finish();
		}
		return true;
	}
	
	public void clearHistory() {
		this.deleteDatabase(DatabaseHandler.DATABASE_NAME);
		WebViewDatabase m = WebViewDatabase
				.getInstance(this);
		m.clearFormData();
		m.clearHttpAuthUsernamePassword();
		if (API < 18) {
			m.clearUsernamePassword();
			WebIconDatabase.getInstance().removeAllIcons();
		}
		if (mSystemBrowser) {
			try {
				Browser.clearHistory(getContentResolver());
			} catch (NullPointerException ignored) {
			}
		}
		SettingsController.setClearHistory(true);
		Utils.trimCache(this);
	}
	
	public void clearCookies() {
		CookieManager c = CookieManager.getInstance();
		CookieSyncManager.createInstance(this);
		c.removeAllCookie();
	}

	@Override
	public void onBackPressed() {
		if (!mActionBar.isShowing()) {
			mActionBar.show();
		}
		if (mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);
		} else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
			mDrawerLayout.closeDrawer(mDrawerRight);
		} else {
			if (mCurrentView != null) {
				Log.i(Constants.LOGTAG, "onBackPressed");
				if (mCurrentView.canGoBack()) {
					if (!mCurrentView.isShown()) {
						onHideCustomView();
					} else {
						mCurrentView.goBack();
					}
				} else {
					deleteTab(mDrawerList.getCheckedItemPosition());
				}
			} else {
				Log.e(Constants.LOGTAG,
						"So madness. Much confusion. Why happen.");
				super.onBackPressed();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(Constants.LOGTAG, "onPause");
		if (mCurrentView != null) {
			mCurrentView.pauseTimers();
			mCurrentView.onPause();
		}
		if (mHistoryDatabase != null) {
			if (mHistoryDatabase.isOpen())
				mHistoryDatabase.close();
		}
		if (mHistoryHandler != null) {
			if (mHistoryHandler.isOpen())
				mHistoryHandler.close();
		}

	}

	public void saveOpenTabs() {
		if (mPreferences
				.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
			String s = "";
			for (int n = 0; n < mWebViews.size(); n++) {
				if (mWebViews.get(n).getUrl() != null) {
					s = s + mWebViews.get(n).getUrl() + "|$|SEPARATOR|$|";
				}
			}
			mEditPrefs.putString(PreferenceConstants.URL_MEMORY, s);
			mEditPrefs.commit();
		}
	}

	@Override
	protected void onDestroy() {
		Log.i(Constants.LOGTAG, "onDestroy");
		if (mHistoryDatabase != null) {
			if (mHistoryDatabase.isOpen())
				mHistoryDatabase.close();
		}
		if (mHistoryHandler != null) {
			if (mHistoryHandler.isOpen())
				mHistoryHandler.close();
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(Constants.LOGTAG, "onResume");
		if (SettingsController.getClearHistory()) {
		}
		if (mSearchAdapter != null) {
			mSearchAdapter.refreshPreferences();
			mSearchAdapter.refreshBookmarks();
		}
		if (mActionBar != null) {
			if (!mActionBar.isShowing()) {
				mActionBar.show();
			}
		}
		if (mCurrentView != null) {
			mCurrentView.resumeTimers();
			mCurrentView.onResume();

			if (mHistoryHandler == null) {
				mHistoryHandler = new DatabaseHandler(this);
			} else if (!mHistoryHandler.isOpen()) {
				mHistoryHandler = new DatabaseHandler(this);
			}
			mHistoryDatabase = mHistoryHandler.getReadableDatabase();
			mBookmarkList = getBookmarks();
			notifyBookmarkDataSetChanged();
		} else {
			initialize();
		}
		initializePreferences();
		if (mWebViews != null) {
			for (int n = 0; n < mWebViews.size(); n++) {
				if (mWebViews.get(n) != null) {
					mWebViews.get(n).initializePreferences(this);
				} else {
					mWebViews.remove(n);
				}
			}
		} else {
			initialize();
		}
	}

	/**
	 * searches the web for the query fixing any and all problems with the input
	 * checks if it is a search, url, etc.
	 */
	void searchTheWeb(String query) {
		if (query.equals("")) {
			return;
		}
		String SEARCH = mSearchText;
		query = query.trim();
		mCurrentView.stopLoading();

		if (query.startsWith("www.")) {
			query = Constants.HTTP + query;
		} else if (query.startsWith("ftp.")) {
			query = "ftp://" + query;
		}

		boolean containsPeriod = query.contains(".");
		boolean isIPAddress = (TextUtils.isDigitsOnly(query.replace(".", ""))
				&& (query.replace(".", "").length() >= 4) && query
				.contains("."));
		boolean aboutScheme = query.contains("about:");
		boolean validURL = (query.startsWith("ftp://")
				|| query.startsWith(Constants.HTTP)
				|| query.startsWith(Constants.FILE) || query
					.startsWith(Constants.HTTPS)) || isIPAddress;
		boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

		if (isIPAddress
				&& (!query.startsWith(Constants.HTTP) || !query
						.startsWith(Constants.HTTPS))) {
			query = Constants.HTTP + query;
		}

		if (isSearch) {
			try {
				query = URLEncoder.encode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			mCurrentView.loadUrl(SEARCH + query);
		} else if (!validURL) {
			mCurrentView.loadUrl(Constants.HTTP + query);
		} else {
			mCurrentView.loadUrl(query);
		}
	}

	public void deleteBookmark(String url) {
		File book = new File(getFilesDir(), "bookmarks");
		File bookUrl = new File(getFilesDir(), "bookurl");
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl));
			Iterator<HistoryItem> iter = mBookmarkList.iterator();
			HistoryItem item;
			int num = 0;
			int deleteIndex = -1;
			while (iter.hasNext()) {
				item = iter.next();
				if (!item.getUrl().equalsIgnoreCase(url)) {
					bookWriter.write(item.getTitle());
					urlWriter.write(item.getUrl());
					bookWriter.newLine();
					urlWriter.newLine();
				} else {
					deleteIndex = num;
				}
				num++;
			}
			if (deleteIndex != -1) {
				mBookmarkList.remove(deleteIndex);
			}
			bookWriter.close();
			urlWriter.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		notifyBookmarkDataSetChanged();
		mSearchAdapter.refreshBookmarks();
		openBookmarks();
	}

	/**
	 * converts the int num into density pixels
	 * 
	 * @param num
	 * @return density pixels
	 */
	private int getDp(int num) {
		float scale = getResources().getDisplayMetrics().density;
		return (int) (num * scale + 0.5f);
	}

	private int pixelsToDp(int num) {
		float scale = getResources().getDisplayMetrics().density;
		return (int) ((num - 0.5f) / scale);
	}

	/**
	 * writes the number of open tabs on the icon.
	 */
	public BitmapDrawable writeOnDrawable(int number) {

		Bitmap bm = Bitmap.createBitmap(mActionBarSize, mActionBarSize,
				Config.ARGB_8888);
		String text = number + "";
		Paint paint = new Paint();
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(mNumberIconColor);
		if (number > 99)
			number = 99;
		// pixels, 36 dp
		if (mActionBarSizeDp < 50) {
			if (number > 9) {
				paint.setTextSize(mActionBarSize * 3 / 4); // originally
															// 40
															// pixels,
															// 24 dp
			} else {
				paint.setTextSize(mActionBarSize * 9 / 10); // originally 50
															// pixels, 30 dp
			}
		} else {
			paint.setTextSize(mActionBarSize * 3 / 4);
		}
		Canvas canvas = new Canvas(bm);
		// originally only vertical padding of 5 pixels

		int xPos = (canvas.getWidth() / 2);
		int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint
				.ascent()) / 2));

		canvas.drawText(text, xPos, yPos, paint);

		return new BitmapDrawable(getResources(), bm);
	}

	public class LightningViewAdapter extends ArrayAdapter<LightningView> {

		Context context;
		int layoutResourceId;
		List<LightningView> data = null;

		public LightningViewAdapter(Context context, int layoutResourceId,
				List<LightningView> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View row = convertView;
			LightningViewHolder holder = null;
			if (row == null) {
				LayoutInflater inflater = ((Activity) context)
						.getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new LightningViewHolder();
				holder.txtTitle = (TextView) row.findViewById(R.id.text1);
				holder.favicon = (ImageView) row.findViewById(R.id.favicon1);
				holder.exit = (ImageView) row.findViewById(R.id.delete1);
				holder.exit.setTag(position);
				row.setTag(holder);
			} else {
				holder = (LightningViewHolder) row.getTag();
			}

			holder.exit.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					deleteTab(position);
				}

			});

			LightningView web = data.get(position);
			holder.txtTitle.setText(web.getTitle());
			Bitmap favicon = web.getFavicon();
			holder.favicon.setImageBitmap(favicon);
			return row;
		}

		class LightningViewHolder {
			TextView txtTitle;
			ImageView favicon;
			ImageView exit;
		}
	}

	public class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

		Context context;
		int layoutResourceId;
		List<HistoryItem> data = null;

		public BookmarkViewAdapter(Context context, int layoutResourceId,
				List<HistoryItem> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			BookmarkViewHolder holder = null;

			if (row == null) {
				LayoutInflater inflater = ((Activity) context)
						.getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new BookmarkViewHolder();
				holder.txtTitle = (TextView) row.findViewById(R.id.text1);
				holder.favicon = (ImageView) row.findViewById(R.id.favicon1);
				row.setTag(holder);
			} else {
				holder = (BookmarkViewHolder) row.getTag();
			}

			HistoryItem web = data.get(position);
			holder.txtTitle.setText(web.getTitle());
			holder.favicon.setImageBitmap(mWebpageBitmap);
			if (web.getBitmap() == null) {
				getImage(holder.favicon, web);
			} else {
				holder.favicon.setImageBitmap(web.getBitmap());
			}
			return row;
		}

		class BookmarkViewHolder {
			TextView txtTitle;
			ImageView favicon;
		}
	}

	public void getImage(ImageView image, HistoryItem web) {
		try {
			new DownloadImageTask(image, web).execute(Constants.HTTP
					+ getDomainName(web.getUrl()) + "/favicon.ico");
		} catch (URISyntaxException e) {
			new DownloadImageTask(image, web)
					.execute("https://www.google.com/s2/favicons?domain_url="
							+ web.getUrl());
			e.printStackTrace();
		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;
		HistoryItem mWeb;

		public DownloadImageTask(ImageView bmImage, HistoryItem web) {
			this.bmImage = bmImage;
			this.mWeb = web;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon = null;
			// unique path for each url that is bookmarked.
			String hash = String.valueOf(urldisplay.hashCode());
			File image = new File(mContext.getCacheDir(), hash + ".png");
			// checks to see if the image exists
			if (!image.exists()) {
				try {
					// if not, download it...
					InputStream in = new java.net.URL(urldisplay).openStream();

					if (in != null) {
						mIcon = BitmapFactory.decodeStream(in);
					}
					// ...and cache it
					if (mIcon != null) {
						FileOutputStream fos = new FileOutputStream(image);
						mIcon.compress(Bitmap.CompressFormat.PNG, 85, fos);
						fos.flush();
						fos.close();
					}

				} catch (Exception e) {
				}
			} else {
				// if it exists, retrieve it from the cache
				mIcon = BitmapFactory.decodeFile(image.getPath());
			}
			if (mIcon == null) {
				try {
					// if not, download it...
					InputStream in = new java.net.URL(
							"https://www.google.com/s2/favicons?domain_url="
									+ urldisplay).openStream();

					if (in != null) {
						mIcon = BitmapFactory.decodeStream(in);
					}
					// ...and cache it
					if (mIcon != null) {
						FileOutputStream fos = new FileOutputStream(image);
						mIcon.compress(Bitmap.CompressFormat.PNG, 85, fos);
						fos.flush();
						fos.close();
					}

				} catch (Exception e) {
				}
			}
			if (mIcon == null) {
				return mWebpageBitmap;
			} else {
				return mIcon;
			}
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
			mWeb.setBitmap(result);
			notifyBookmarkDataSetChanged();
		}
	}

	static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		if (domain == null) {
			return url;
		}
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	@Override
	public void updateUrl(String url) {
		if (url == null)
			return;
		url = url.replaceFirst(Constants.HTTP, "");
		if (url.startsWith(Constants.FILE)) {
			url = "";
		}

		mSearch.setText(url);
	}

	@Override
	public void updateProgress(int n) {
		if (!mProgress.isShown())
			mProgress.setVisibility(View.VISIBLE);
		mProgress.setProgress(n);
		if (n == 100) {
			mProgress.setVisibility(View.INVISIBLE);
			setIsFinishedLoading();
		} else {
			setIsLoading();
		}
	}

	@Override
	public void updateHistory(final String title, final String url) {

	}

	public void addItemToHistory(final String title, final String url) {
		Runnable update = new Runnable() {
			@Override
			public void run() {
				if (isSystemBrowserAvailable()) {
					try {
						Browser.updateVisitedHistory(getContentResolver(), url,
								true);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					StringBuilder sb = new StringBuilder("url" + " = ");
					DatabaseUtils.appendEscapedSQLString(sb, url);

					if (mHistoryHandler == null) {
						mHistoryHandler = new DatabaseHandler(mContext);
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					} else if (!mHistoryHandler.isOpen()) {
						mHistoryHandler = new DatabaseHandler(mContext);
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					} else if (mHistoryDatabase == null) {
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					} else if (!mHistoryDatabase.isOpen()) {
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					}
					Cursor cursor = mHistoryDatabase.query(
							DatabaseHandler.TABLE_HISTORY, new String[] { "id",
									"url", "title" }, sb.toString(), null,
							null, null, null);
					if (!cursor.moveToFirst()) {
						mHistoryHandler.addHistoryItem(new HistoryItem(url,
								title));
					} else {
						mHistoryHandler.delete(url);
						mHistoryHandler.addHistoryItem(new HistoryItem(url,
								title));
					}
					cursor.close();
					cursor = null;
				} catch (IllegalStateException e) {
					Log.e(Constants.LOGTAG,
							"IllegalStateException in updateHistory");
				} catch (NullPointerException e) {
					Log.e(Constants.LOGTAG,
							"NullPointerException in updateHistory");
				} catch (SQLiteException e) {
					Log.e(Constants.LOGTAG, "SQLiteException in updateHistory");
				}
			}
		};
		if (url != null) {
			if (!url.startsWith(Constants.FILE)) {
				new Thread(update).start();

			}
		}
	}

	/**
	 * 1, 2, 3, testing... is there a system browser that has some nice
	 * bookmarks for us?
	 */
	public boolean isSystemBrowserAvailable() {
		return mSystemBrowser;
	}

	/**
	 * 1, 2, 3, testing... is there a system browser that has some nice
	 * bookmarks for us? helper method for isSystemBrowserAvailable
	 */
	public boolean getSystemBrowser() {
		Cursor c = null;
		String[] columns = new String[] { "url", "title" };
		boolean browserFlag = false;
		try {

			Uri bookmarks = Browser.BOOKMARKS_URI;
			c = getContentResolver()
					.query(bookmarks, columns, null, null, null);
		} catch (SQLiteException ignored) {
		} catch (IllegalStateException ignored) {
		} catch (NullPointerException ignored) {
		}

		if (c != null) {
			Log.i("Browser", "System Browser Available");
			browserFlag = true;
		} else {
			Log.e("Browser", "System Browser Unavailable");
			browserFlag = false;
		}
		if (c != null) {
			c.close();
			c = null;
		}
		mEditPrefs.putBoolean("SystemBrowser", browserFlag);
		mEditPrefs.commit();
		return browserFlag;
	}

	/**
	 * method to generate search suggestions for the AutoCompleteTextView from
	 * previously searched URLs
	 */
	private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

		getUrl.setThreshold(1);
		getUrl.setDropDownWidth(-1);
		getUrl.setDropDownAnchor(R.id.progressWrapper);
		getUrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				try {
					String url;
					url = ((TextView) arg1.findViewById(R.id.url)).getText()
							.toString();
					if (url.startsWith(mContext.getString(R.string.suggestion))) {
						url = ((TextView) arg1.findViewById(R.id.title))
								.getText().toString();
					} else {
						getUrl.setText(url);
					}
					searchTheWeb(url);
					url = null;
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
				} catch (NullPointerException e) {
					Log.e("Browser Error: ",
							"NullPointerException on item click");
				}
			}

		});

		getUrl.setSelectAllOnFocus(true);
		mSearchAdapter = new SearchAdapter(mContext, isIncognito());
		getUrl.setAdapter(mSearchAdapter);
	}

	public boolean isIncognito() {
		return false;
	}

	// Damn it, I regret not using SQLite in the first place for this
	private List<HistoryItem> getBookmarks() {
		List<HistoryItem> bookmarks = new ArrayList<HistoryItem>();
		File bookUrl = new File(getApplicationContext().getFilesDir(),
				"bookurl");
		File book = new File(getApplicationContext().getFilesDir(), "bookmarks");
		try {
			BufferedReader readUrl = new BufferedReader(new FileReader(bookUrl));
			BufferedReader readBook = new BufferedReader(new FileReader(book));
			String u, t;
			while ((u = readUrl.readLine()) != null
					&& (t = readBook.readLine()) != null) {
				HistoryItem map = new HistoryItem(u, t);
				bookmarks.add(map);
			}
			readBook.close();
			readUrl.close();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
		Collections.sort(bookmarks, new SortIgnoreCase());
		return bookmarks;
	}

	/**
	 * returns a list of HistoryItems
	 * 
	 * @return
	 */
	private List<HistoryItem> getLatestHistory() {
		DatabaseHandler historyHandler = new DatabaseHandler(mContext);
		return historyHandler.getLastHundredItems();
	}

	/**
	 * function that opens the HTML history page in the browser
	 */
	private void openHistory() {

		Thread history = new Thread(new Runnable() {

			@Override
			public void run() {
				String historyHtml = HistoryPageVariables.Heading;
				List<HistoryItem> historyList = getLatestHistory();
				Iterator<HistoryItem> it = historyList.iterator();
				HistoryItem helper;
				while (it.hasNext()) {
					helper = it.next();
					historyHtml += HistoryPageVariables.Part1 + helper.getUrl()
							+ HistoryPageVariables.Part2 + helper.getTitle()
							+ HistoryPageVariables.Part3 + helper.getUrl()
							+ HistoryPageVariables.Part4;
				}

				historyHtml += HistoryPageVariables.End;
				File historyWebPage = new File(getFilesDir(), "history.html");
				try {
					FileWriter hWriter = new FileWriter(historyWebPage, false);
					hWriter.write(historyHtml);
					hWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				mCurrentView.loadUrl(Constants.FILE + historyWebPage);
				mSearch.setText("");
			}

		});
		history.run();
	}

	/**
	 * helper function that opens the bookmark drawer
	 */
	private void openBookmarks() {
		if (mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawers();
		}
		mDrawerToggle.syncState();
		mDrawerLayout.openDrawer(mDrawerRight);
	}

	@Override
	/**
	 * open the HTML bookmarks page, parameter view is the WebView that should show the page
	 */
	public void openBookmarkPage(WebView view) {
		String bookmarkHtml = BookmarkPageVariables.Heading;
		Iterator<HistoryItem> iter = mBookmarkList.iterator();
		HistoryItem helper;
		while (iter.hasNext()) {
			helper = iter.next();
			bookmarkHtml += (BookmarkPageVariables.Part1 + helper.getUrl()
					+ BookmarkPageVariables.Part2 + helper.getUrl()
					+ BookmarkPageVariables.Part3 + helper.getTitle() + BookmarkPageVariables.Part4);
		}
		bookmarkHtml += BookmarkPageVariables.End;
		File bookmarkWebPage = new File(mContext.getCacheDir(),
				"bookmarks.html");
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkHtml);
			bookWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		view.loadUrl(Constants.FILE + bookmarkWebPage);
	}

	/**
	 * adds a bookmark with a title and url. Simple.
	 */
	public void addBookmark(Context context, String title, String url) {
		File book = new File(context.getFilesDir(), "bookmarks");
		File bookUrl = new File(context.getFilesDir(), "bookurl");
		HistoryItem bookmark = new HistoryItem(url, title);

		try {
			BufferedReader readUrlRead = new BufferedReader(new FileReader(
					bookUrl));
			String u;
			while ((u = readUrlRead.readLine()) != null) {
				if (u.contentEquals(url)) {
					readUrlRead.close();
					return;
				}
			}
			readUrlRead.close();

		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book,
					true));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl, true));
			bookWriter.write(title);
			urlWriter.write(url);
			bookWriter.newLine();
			urlWriter.newLine();
			bookWriter.close();
			urlWriter.close();
			mBookmarkList.add(bookmark);
			Collections.sort(mBookmarkList, new SortIgnoreCase());
			notifyBookmarkDataSetChanged();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
		mSearchAdapter.refreshBookmarks();
	}

	@Override
	public void update() {
		mTitleAdapter.notifyDataSetChanged();
	}

	@Override
	/**
	 * opens a file chooser
	 * param ValueCallback is the message from the WebView indicating a file chooser
	 * should be opened
	 */
	public void openFileChooser(ValueCallback<Uri> uploadMsg) {
		mUploadMessage = uploadMsg;
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");
		startActivityForResult(Intent.createChooser(i, "File Chooser"), 1);
	}

	@Override
	/**
	 * used to allow uploading into the browser, doesn't get called in KitKat :(
	 */
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == 1) {
			if (null == mUploadMessage)
				return;
			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;

		}
	}

	@Override
	/**
	 * handles long presses for the browser, tries to get the
	 * url of the item that was clicked and sends it (it can be null)
	 * to the click handler that does cool stuff with it
	 */
	public void onLongPress() {
		if (mClickHandler == null) {
			mClickHandler = new ClickHandler(mContext);
		}
		Message click = mClickHandler.obtainMessage();
		if (click != null) {
			click.setTarget(mClickHandler);
		}
		mCurrentView.getWebView().requestFocusNodeHref(click);
	}

	@Override
	public void onShowCustomView(View view, int requestedOrientation,
			CustomViewCallback callback) {
		if (view == null) {
			return;
		}
		if (mCustomView != null && callback != null) {
			callback.onCustomViewHidden();
			return;
		}
		view.setKeepScreenOn(true);
		mOriginalOrientation = getRequestedOrientation();
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		mFullscreenContainer = new FullscreenHolder(this);
		mCustomView = view;
		mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
		decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
		setFullscreen(true);
		mCurrentView.setVisibility(View.GONE);
		if (view instanceof FrameLayout) {
			if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
				mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
				mVideoView.setOnErrorListener(new VideoCompletionListener());
				mVideoView
						.setOnCompletionListener(new VideoCompletionListener());
			}
		}
		mCustomViewCallback = callback;
	}

	@Override
	public void onHideCustomView() {
		if (mCustomView == null || mCustomViewCallback == null
				|| mCurrentView == null)
			return;
		Log.i(Constants.LOGTAG, "onHideCustomView");
		mCurrentView.setVisibility(View.VISIBLE);
		mCustomView.setKeepScreenOn(false);
		setFullscreen(mPreferences.getBoolean(
				PreferenceConstants.HIDE_STATUS_BAR, false));
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		if (decor != null) {
			decor.removeView(mFullscreenContainer);
		}

		if (API < 19) {
			try {
				mCustomViewCallback.onCustomViewHidden();
			} catch (Throwable ignored) {

			}
		}
		mFullscreenContainer = null;
		mCustomView = null;
		if (mVideoView != null) {
			mVideoView.setOnErrorListener(null);
			mVideoView.setOnCompletionListener(null);
			mVideoView = null;
		}
		setRequestedOrientation(mOriginalOrientation);
	}

	private class VideoCompletionListener implements
			MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			onHideCustomView();
		}

	}

	/**
	 * turns on fullscreen mode in the app
	 * 
	 * @param enabled
	 *            whether to enable fullscreen or not
	 */
	public void setFullscreen(boolean enabled) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (enabled) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
			if (mCustomView != null) {
				mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			} else {
				mBrowserFrame
						.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		}
		win.setAttributes(winParams);
	}

	/**
	 * a class extending FramLayout used to display fullscreen videos
	 */
	static class FullscreenHolder extends FrameLayout {

		public FullscreenHolder(Context ctx) {
			super(ctx);
			setBackgroundColor(ctx.getResources().getColor(
					android.R.color.black));
		}

		@Override
		public boolean onTouchEvent(MotionEvent evt) {
			return true;
		}

	}

	@Override
	/**
	 * a stupid method that returns the bitmap image to display in place of
	 * a loading video
	 */
	public Bitmap getDefaultVideoPoster() {
		if (mDefaultVideoPoster == null) {
			mDefaultVideoPoster = BitmapFactory.decodeResource(getResources(),
					android.R.drawable.ic_media_play);
		}
		return mDefaultVideoPoster;
	}

	@Override
	/**
	 * dumb method that returns the loading progress for a video
	 */
	public View getVideoLoadingProgressView() {
		if (mVideoProgressView == null) {
			LayoutInflater inflater = LayoutInflater.from(this);
			mVideoProgressView = inflater.inflate(
					R.layout.video_loading_progress, null);
		}
		return mVideoProgressView;
	}

	@Override
	/**
	 * handles javascript requests to create a new window in the browser
	 */
	public void onCreateWindow(boolean isUserGesture, Message resultMsg) {
		if (resultMsg == null) {
			return;
		}
		newTab("", true);
		WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
		transport.setWebView(mCurrentView.getWebView());
		resultMsg.sendToTarget();
	}

	@Override
	/**
	 * returns the Activity instance for this activity,
	 * very helpful when creating things in other classes... I think
	 */
	public Activity getActivity() {
		return mActivity;
	}

	/**
	 * it hides the action bar, seriously what else were you expecting
	 */
	@Override
	public void hideActionBar() {
		if (mActionBar.isShowing() && mFullScreen) {
			mActionBar.hide();
		}
	}

	@Override
	/**
	 * obviously it shows the action bar if it's hidden
	 */
	public void showActionBar() {
		if (!mActionBar.isShowing() && mFullScreen) {
			mActionBar.show();
		}
	}

	@Override
	/**
	 * handles a long click on the page, parameter String url 
	 * is the url that should have been obtained from the WebView touch node
	 * thingy, if it is null, this method tries to deal with it and find a workaround
	 */
	public void longClickPage(final String url) {
		HitTestResult result = null;
		if (mCurrentView.getWebView() != null) {
			result = mCurrentView.getWebView().getHitTestResult();
		}
		if (url != null) {
			if (result != null) {
				if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
						|| result.getType() == HitTestResult.IMAGE_TYPE) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(url, false);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(url);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API > 8) {
									Utils.downloadFile(mActivity, url,
											mCurrentView.getUserAgent(),
											"attachment", false);
								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(url.replace(Constants.HTTP, ""))
							.setMessage(
									getResources().getString(
											R.string.dialog_image))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(url, false);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(url);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label",
										url);
								clipboard.setPrimaryClip(clip);

								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(url)
							.setMessage(
									getResources().getString(
											R.string.dialog_link))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_copy),
									dialogClickListener).show();
				}
			} else {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE: {
							newTab(url, false);
							break;
						}
						case DialogInterface.BUTTON_NEGATIVE: {
							mCurrentView.loadUrl(url);
							break;
						}
						case DialogInterface.BUTTON_NEUTRAL: {
							ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("label", url);
							clipboard.setPrimaryClip(clip);

							break;
						}
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
				builder.setTitle(url)
						.setMessage(
								getResources().getString(R.string.dialog_link))
						.setPositiveButton(
								getResources().getString(
										R.string.action_new_tab),
								dialogClickListener)
						.setNegativeButton(
								getResources().getString(R.string.action_open),
								dialogClickListener)
						.setNeutralButton(
								getResources().getString(R.string.action_copy),
								dialogClickListener).show();
			}
		} else if (result != null) {
			if (result.getExtra() != null) {
				final String newUrl = result.getExtra();
				if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
						|| result.getType() == HitTestResult.IMAGE_TYPE) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(newUrl, false);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(newUrl);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API > 8) {
									Utils.downloadFile(mActivity, newUrl,
											mCurrentView.getUserAgent(),
											"attachment", false);
								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(newUrl.replace(Constants.HTTP, ""))
							.setMessage(
									getResources().getString(
											R.string.dialog_image))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(newUrl, false);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(newUrl);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label",
										newUrl);
								clipboard.setPrimaryClip(clip);

								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(newUrl)
							.setMessage(
									getResources().getString(
											R.string.dialog_link))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_copy),
									dialogClickListener).show();
				}

			}

		}

	}

	/**
	 * This method lets the search bar know that the page is currently loading
	 * and that it should display the stop icon to indicate to the user that
	 * pressing it stops the page from loading
	 */
	public void setIsLoading() {
		if (!mSearch.hasFocus()) {
			mIcon = mDeleteIcon;
			mSearch.setCompoundDrawables(null, null, mDeleteIcon, null);
		}
	}

	/**
	 * This tells the search bar that the page is finished loading and it should
	 * display the refresh icon
	 */
	public void setIsFinishedLoading() {
		if (!mSearch.hasFocus()) {
			mIcon = mRefreshIcon;
			mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
		}
	}

	/**
	 * handle presses on the refresh icon in the search bar, if the page is
	 * loading, stop the page, if it is done loading refresh the page.
	 * 
	 * See setIsFinishedLoading and setIsLoading for displaying the correct icon
	 */
	public void refreshOrStop() {
		if (mCurrentView != null) {
			if (mCurrentView.getProgress() < 100) {
				mCurrentView.stopLoading();
			} else {
				mCurrentView.reload();
			}
		}
	}

	@Override
	public boolean isActionBarShowing() {
		if (mActionBar != null) {
			return mActionBar.isShowing();
		} else {
			return false;
		}
	}

	// Override this, use finish() for Incognito, moveTaskToBack for Main
	public void closeActivity() {
		finish();
	}

	public class SortIgnoreCase implements Comparator<HistoryItem> {

		public int compare(HistoryItem o1, HistoryItem o2) {
			return o1.getTitle().toLowerCase(Locale.getDefault())
					.compareTo(o2.getTitle().toLowerCase(Locale.getDefault()));
		}

	}

}