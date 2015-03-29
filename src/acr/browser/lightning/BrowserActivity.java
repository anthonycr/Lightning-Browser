/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebView.HitTestResult;
import android.widget.*;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class BrowserActivity extends ActionBarActivity implements BrowserController {

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerListLeft;
	private LinearLayout mDrawerLeft;
	private LinearLayout mDrawerRight;
	private ListView mDrawerListRight;
	private RelativeLayout mNewTab;
	private List<LightningView> mWebViews = new ArrayList<LightningView>();
	private LightningView mCurrentView;
	private int mIdGenerator;
	private Toolbar mToolbar;
	private LightningViewAdapter mTitleAdapter;
	private List<HistoryItem> mBookmarkList;
	private BookmarkViewAdapter mBookmarkAdapter;
	private DrawerArrowDrawable mArrowDrawable;
	private ImageView mArrowImage;
	private AutoCompleteTextView mSearch;
	private ClickHandler mClickHandler;
	private AnimatedProgressBar mProgressBar;
	private boolean mSystemBrowser = false;
	private ValueCallback<Uri> mUploadMessage;
	private View mCustomView;
	private int mOriginalOrientation;
	private int mBackgroundColor;
	private ActionBar mActionBar;
	private boolean mFullScreen;
	private boolean mColorMode;
	private FrameLayout mBrowserFrame;
	private LinearLayout mPageLayout;
	private LinearLayout mUiLayout;
	private FullscreenHolder mFullscreenContainer;
	private CustomViewCallback mCustomViewCallback;
	private final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	private Bitmap mDefaultVideoPoster;
	private View mVideoProgressView;
	private LinearLayout mToolbarLayout;
	private HistoryDatabase mHistoryDatabase;
	private SharedPreferences mPreferences;
	private Context mContext;
	private Bitmap mWebpageBitmap;
	private String mSearchText;
	private String mUntitledTitle;
	private Activity mActivity;
	private final int API = android.os.Build.VERSION.SDK_INT;
	private Drawable mDeleteIcon;
	private Drawable mRefreshIcon;
	private Drawable mCopyIcon;
	private Drawable mIcon;
	private String mHomepage;
	private boolean mIsNewIntent = false;
	private VideoView mVideoView;
	private static SearchAdapter mSearchAdapter;
	private static LayoutParams mMatchParent = new LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);
	private BookmarkManager mBookmarkManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private synchronized void initialize() {
		setContentView(R.layout.activity_main);

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);

		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mContext = this;
		if (mWebViews != null) {
			mWebViews.clear();
		} else {
			mWebViews = new ArrayList<LightningView>();
		}
		mBookmarkManager = new BookmarkManager(this);
		if (!mPreferences.getBoolean(PreferenceConstants.OLD_BOOKMARKS_IMPORTED, false)) {
			List<HistoryItem> old = Utils.getOldBookmarks(this);
			mBookmarkManager.addBookmarkList(old);
			mPreferences.edit().putBoolean(PreferenceConstants.OLD_BOOKMARKS_IMPORTED, true)
					.apply();
		}
		mActivity = this;
		mClickHandler = new ClickHandler(this);
		getWindow().setBackgroundDrawable(null);
		mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
		mToolbarLayout = (LinearLayout) findViewById(R.id.toolbar_layout);
		mPageLayout = (LinearLayout) findViewById(R.id.main_layout);
		mUiLayout = (LinearLayout) findViewById(R.id.ui_layout);
		mProgressBar = (AnimatedProgressBar) findViewById(R.id.progress_view);
		mNewTab = (RelativeLayout) findViewById(R.id.new_tab_button);
		mDrawerLeft = (LinearLayout) findViewById(R.id.left_drawer);
		mDrawerLeft.setLayerType(View.LAYER_TYPE_HARDWARE, null); // Drawer
																	// stutters
																	// otherwise
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerListLeft = (ListView) findViewById(R.id.left_drawer_list);
		mDrawerListLeft.setDivider(null);
		mDrawerListLeft.setDividerHeight(0);
		mDrawerRight = (LinearLayout) findViewById(R.id.right_drawer);
		mDrawerRight.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);
		mDrawerListRight.setDivider(null);
		mDrawerListRight.setDividerHeight(0);

		setNavigationDrawerWidth();
		mDrawerLayout.setDrawerListener(new DrawerLocker());

		mWebpageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_webpage);
		mActionBar = getSupportActionBar();

		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);

		mTitleAdapter = new LightningViewAdapter(this, R.layout.tab_list_item, mWebViews);
		mDrawerListLeft.setAdapter(mTitleAdapter);
		mDrawerListLeft.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerListLeft.setOnItemLongClickListener(new DrawerItemLongClickListener());

		mBookmarkList = mBookmarkManager.getBookmarks(true);
		mBookmarkAdapter = new BookmarkViewAdapter(this, R.layout.bookmark_list_item, mBookmarkList);
		mDrawerListRight.setAdapter(mBookmarkAdapter);
		mDrawerListRight.setOnItemClickListener(new BookmarkItemClickListener());
		mDrawerListRight.setOnItemLongClickListener(new BookmarkItemLongClickListener());

		mHistoryDatabase = HistoryDatabase.getInstance(this);

		// set display options of the ActionBar

		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setHomeButtonEnabled(false);
		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayUseLogoEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setCustomView(R.layout.toolbar_content);

		View v = mActionBar.getCustomView();
		LayoutParams lp = v.getLayoutParams();
		lp.width = LayoutParams.MATCH_PARENT;
		v.setLayoutParams(lp);

		// TODO

		mArrowDrawable = new DrawerArrowDrawable(this);
		mArrowImage = (ImageView) mActionBar.getCustomView().findViewById(R.id.arrow);
		mArrowImage.setLayerType(View.LAYER_TYPE_HARDWARE, null); // Use a
																	// hardware
																	// layer for
																	// the
																	// animation
		mArrowImage.setImageDrawable(mArrowDrawable);
		LinearLayout arrowButton = (LinearLayout) mActionBar.getCustomView().findViewById(
				R.id.arrow_button);
		arrowButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mSearch != null && mSearch.hasFocus()) {
					mCurrentView.requestFocus();
				} else {
					mDrawerLayout.openDrawer(mDrawerLeft);
				}
			}

		});

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
							deleteTab(mDrawerListLeft.getCheckedItemPosition());
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

		// create the search EditText in the ToolBar
		mSearch = (AutoCompleteTextView) mActionBar.getCustomView().findViewById(R.id.search);
		mUntitledTitle = (String) this.getString(R.string.untitled);
		mBackgroundColor = getResources().getColor(R.color.primary_color);
		Theme theme = getTheme();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete);
			mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh);
			mCopyIcon = getResources().getDrawable(R.drawable.ic_action_copy);
		} else {
			mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete, theme);
			mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh, theme);
			mCopyIcon = getResources().getDrawable(R.drawable.ic_action_copy, theme);
		}

		mDeleteIcon.setBounds(0, 0, Utils.convertDpToPixels(24), Utils.convertDpToPixels(24));
		mRefreshIcon.setBounds(0, 0, Utils.convertDpToPixels(24), Utils.convertDpToPixels(24));
		mCopyIcon.setBounds(0, 0, Utils.convertDpToPixels(24), Utils.convertDpToPixels(24));
		mIcon = mRefreshIcon;
		SearchClass search = new SearchClass();
		mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
		mSearch.setOnKeyListener(search.new KeyListener());
		mSearch.setOnFocusChangeListener(search.new FocusChangeListener());
		mSearch.setOnEditorActionListener(search.new EditorActionListener());
		mSearch.setOnTouchListener(search.new TouchListener());

		mSystemBrowser = getSystemBrowser();
		Thread initialize = new Thread(new Runnable() {

			@Override
			public void run() {
				initializeSearchSuggestions(mSearch);
			}

		});
		initialize.run();

		mNewTab.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				newTab(null, true);
			}

		});

		mNewTab.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				String url = mPreferences.getString(PreferenceConstants.SAVE_URL, null);
				if (url != null) {
					newTab(url, true);
					Toast.makeText(mContext, R.string.deleted_tab, Toast.LENGTH_SHORT).show();
				}
				mPreferences.edit().putString(PreferenceConstants.SAVE_URL, null).apply();
				return true;
			}

		});

		// mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);
		initializePreferences();
		initializeTabs();

		if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
		}

		checkForTor();

	}

	private class SearchClass {

		public class KeyListener implements OnKeyListener {

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

		}

		public class EditorActionListener implements OnEditorActionListener {
			@Override
			public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
				// hide the keyboard and search the web when the enter key
				// button is pressed
				if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
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
		}

		public class FocusChangeListener implements OnFocusChangeListener {
			@Override
			public void onFocusChange(View v, final boolean hasFocus) {
				if (!hasFocus && mCurrentView != null) {
					if (mCurrentView.getProgress() < 100) {
						setIsLoading();
					} else {
						setIsFinishedLoading();
					}
					updateUrl(mCurrentView.getUrl(), true);
				} else if (hasFocus) {
					String url = mCurrentView.getUrl();
					if (url == null || url.startsWith(Constants.FILE)) {
						mSearch.setText("");
					} else {
						mSearch.setText(url);
					}
					((AutoCompleteTextView) v).selectAll(); // Hack to make sure
															// the text gets
															// selected
					mIcon = mCopyIcon;
					mSearch.setCompoundDrawables(null, null, mCopyIcon, null);
				}
				final Animation anim = new Animation() {

					@Override
					protected void applyTransformation(float interpolatedTime, Transformation t) {
						if (!hasFocus) {
							mArrowDrawable.setProgress(1.0f - interpolatedTime);
						} else {
							mArrowDrawable.setProgress(interpolatedTime);
						}
					}

					@Override
					public boolean willChangeBounds() {
						return true;
					}

				};
				anim.setDuration(300);
				anim.setInterpolator(new DecelerateInterpolator());
				anim.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						if (!hasFocus) {
							mArrowDrawable.setProgress(0.0f);
						} else {
							mArrowDrawable.setProgress(1.0f);
						}
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

				});
				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						mArrowImage.startAnimation(anim);
					}

				}, 100);

				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
				}
			}
		}

		public class TouchListener implements OnTouchListener {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mSearch.getCompoundDrawables()[2] != null) {
					boolean tappedX = event.getX() > (mSearch.getWidth()
							- mSearch.getPaddingRight() - mIcon.getIntrinsicWidth());
					if (tappedX) {
						if (event.getAction() == MotionEvent.ACTION_UP) {
							if (mSearch.hasFocus()) {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label", mSearch.getText()
										.toString());
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

		}
	}

	private class DrawerLocker implements DrawerListener {

		@Override
		public void onDrawerClosed(View v) {
			if (v == mDrawerRight) {
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLeft);
			} else {
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerRight);
			}
		}

		@Override
		public void onDrawerOpened(View v) {
			if (v == mDrawerRight) {
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerLeft);
			} else {
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerRight);
			}
		}

		@Override
		public void onDrawerSlide(View v, float arg) {
		}

		@Override
		public void onDrawerStateChanged(int arg) {
		}

	}

	public boolean handleMenuItemClick(MenuItem item) {
		// Handle action buttons
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
				// mDrawerToggle.syncState();
				return true;
			case R.id.action_back:
				if (mCurrentView != null) {
					if (mCurrentView.canGoBack()) {
						mCurrentView.goBack();
					}
				}
				return true;
			case R.id.action_forward:
				if (mCurrentView != null) {
					if (mCurrentView.canGoForward()) {
						mCurrentView.goForward();
					}
				}
				return true;
			case R.id.action_new_tab:
				newTab(null, true);
				return true;
			case R.id.action_incognito:
				startActivity(new Intent(this, IncognitoActivity.class));
				return true;
			case R.id.action_share:
				if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
					Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
					shareIntent.setType("text/plain");
					shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
							mCurrentView.getTitle());
					String shareMessage = mCurrentView.getUrl();
					shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
					startActivity(Intent.createChooser(shareIntent,
							getResources().getString(R.string.dialog_title_share)));
				}
				return true;
			case R.id.action_bookmarks:
				openBookmarks();
				return true;
			case R.id.action_copy:
				if (mCurrentView != null) {
					if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("label", mCurrentView.getUrl());
						clipboard.setPrimaryClip(clip);
						Utils.showToast(mContext,
								mContext.getResources().getString(R.string.message_link_copied));
					}
				}
				return true;
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			case R.id.action_history:
				openHistory();
				return true;
			case R.id.action_add_bookmark:
				if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
					HistoryItem bookmark = new HistoryItem(mCurrentView.getUrl(),
							mCurrentView.getTitle());
					if (mBookmarkManager.addBookmark(bookmark)) {
						mBookmarkList.add(bookmark);
						Collections.sort(mBookmarkList, new SortIgnoreCase());
						notifyBookmarkDataSetChanged();
						mSearchAdapter.refreshBookmarks();
					}
				}
				return true;
			case R.id.action_find:
				findInPage();
				return true;
			case R.id.action_reading_mode:
				Intent read = new Intent(this, ReadingActivity.class);
				read.putExtra(Constants.LOAD_READING_URL, mCurrentView.getUrl());
				startActivity(read);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * If Orbot/Tor is installed, prompt the user if they want to enable
	 * proxying for this session
	 */
	public boolean checkForTor() {
		boolean useProxy = mPreferences.getBoolean(PreferenceConstants.USE_PROXY, false);

		OrbotHelper oh = new OrbotHelper(this);
		if (oh.isOrbotInstalled()
				&& !mPreferences.getBoolean(PreferenceConstants.INITIAL_CHECK_FOR_TOR, false)) {
			mPreferences.edit().putBoolean(PreferenceConstants.INITIAL_CHECK_FOR_TOR, true).apply();
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							mPreferences.edit().putBoolean(PreferenceConstants.USE_PROXY, true)
									.apply();

							initializeTor();
							break;
						case DialogInterface.BUTTON_NEGATIVE:
							mPreferences.edit().putBoolean(PreferenceConstants.USE_PROXY, false)
									.apply();
							break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.use_tor_prompt)
					.setPositiveButton(R.string.yes, dialogClickListener)
					.setNegativeButton(R.string.no, dialogClickListener).show();

			return true;
		} else if (oh.isOrbotInstalled() & useProxy) {
			initializeTor();
			return true;
		} else {
			mPreferences.edit().putBoolean(PreferenceConstants.USE_PROXY, false).apply();
			return false;
		}
	}

	/*
	 * Initialize WebKit Proxying for Tor
	 */
	public void initializeTor() {

		OrbotHelper oh = new OrbotHelper(this);
		if (!oh.isOrbotRunning()) {
			oh.requestOrbotStart(this);
		}
		try {
			String host = mPreferences.getString(PreferenceConstants.USE_PROXY_HOST, "localhost");
			int port = mPreferences.getInt(PreferenceConstants.USE_PROXY_PORT, 8118);
			WebkitProxy.setProxy(this.getPackageName() + ".BrowserApp", getApplicationContext(),
					host, port);
		} catch (Exception e) {
			Log.d(Constants.TAG, "error enabling web proxying", e);
		}

	}

	private boolean isTablet() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	private void setNavigationDrawerWidth() {
		int width = getResources().getDisplayMetrics().widthPixels - Utils.convertDpToPixels(56);
		int maxWidth;
		if (isTablet()) {
			maxWidth = Utils.convertDpToPixels(320);
		} else {
			maxWidth = Utils.convertDpToPixels(300);
		}
		if (width > maxWidth) {
			DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerLeft
					.getLayoutParams();
			params.width = maxWidth;
			mDrawerLeft.setLayoutParams(params);
			mDrawerLeft.requestLayout();
			DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight
					.getLayoutParams();
			paramsRight.width = maxWidth;
			mDrawerRight.setLayoutParams(paramsRight);
			mDrawerRight.requestLayout();
		} else {
			DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerLeft
					.getLayoutParams();
			params.width = width;
			mDrawerLeft.setLayoutParams(params);
			mDrawerLeft.requestLayout();
			DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight
					.getLayoutParams();
			paramsRight.width = width;
			mDrawerRight.setLayoutParams(paramsRight);
			mDrawerRight.requestLayout();
		}
	}

	/*
	 * Override this class
	 */
	public synchronized void initializeTabs() {

	}

	public void restoreOrNewTab() {
		mIdGenerator = 0;

		String url = null;
		if (getIntent() != null) {
			url = getIntent().getDataString();
			if (url != null) {
				if (url.startsWith(Constants.FILE)) {
					Utils.showToast(this, getResources().getString(R.string.message_blocked_local));
					url = null;
				}
			}
		}
		if (mPreferences.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
			String mem = mPreferences.getString(PreferenceConstants.URL_MEMORY, "");
			mPreferences.edit().putString(PreferenceConstants.URL_MEMORY, "").apply();
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
			mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		}
		mFullScreen = mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN, false);
		mColorMode = mPreferences.getBoolean(PreferenceConstants.ENABLE_COLOR_MODE, true);

		if (!isIncognito() && !mColorMode && mWebpageBitmap != null)
			changeToolbarBackground(mWebpageBitmap);
		else if (!isIncognito() && mCurrentView != null && mCurrentView.getFavicon() != null)
			changeToolbarBackground(mCurrentView.getFavicon());

		if (mFullScreen && mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
			mUiLayout.removeView(mToolbarLayout);
			mBrowserFrame.addView(mToolbarLayout);
			mToolbarLayout.bringToFront();
		} else if (mBrowserFrame.findViewById(R.id.toolbar_layout) != null) {
			mBrowserFrame.removeView(mToolbarLayout);
			mUiLayout.addView(mToolbarLayout, 0);
		}
		if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
			case 0:
				mSearchText = mPreferences.getString(PreferenceConstants.SEARCH_URL,
						Constants.GOOGLE_SEARCH);
				if (!mSearchText.startsWith(Constants.HTTP)
						&& !mSearchText.startsWith(Constants.HTTPS)) {
					mSearchText = Constants.GOOGLE_SEARCH;
				}
				break;
			case 1:
				mSearchText = Constants.GOOGLE_SEARCH;
				break;
			case 2:
				mSearchText = Constants.ASK_SEARCH;
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
				WebkitProxy.resetProxy("acr.browser.lightning.BrowserApp", getApplicationContext());
			} catch (Exception e) {
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
		// Handle action buttons
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
				// mDrawerToggle.syncState();
				return true;
			case R.id.action_back:
				if (mCurrentView != null) {
					if (mCurrentView.canGoBack()) {
						mCurrentView.goBack();
					}
				}
				return true;
			case R.id.action_forward:
				if (mCurrentView != null) {
					if (mCurrentView.canGoForward()) {
						mCurrentView.goForward();
					}
				}
				return true;
			case R.id.action_new_tab:
				newTab(null, true);
				return true;
			case R.id.action_incognito:
				startActivity(new Intent(this, IncognitoActivity.class));
				return true;
			case R.id.action_share:
				if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
					Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
					shareIntent.setType("text/plain");
					shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
							mCurrentView.getTitle());
					String shareMessage = mCurrentView.getUrl();
					shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
					startActivity(Intent.createChooser(shareIntent,
							getResources().getString(R.string.dialog_title_share)));
				}
				return true;
			case R.id.action_bookmarks:
				openBookmarks();
				return true;
			case R.id.action_copy:
				if (mCurrentView != null) {
					if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("label", mCurrentView.getUrl());
						clipboard.setPrimaryClip(clip);
						Utils.showToast(mContext,
								mContext.getResources().getString(R.string.message_link_copied));
					}
				}
				return true;
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			case R.id.action_history:
				openHistory();
				return true;
			case R.id.action_add_bookmark:
				if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
					HistoryItem bookmark = new HistoryItem(mCurrentView.getUrl(),
							mCurrentView.getTitle());
					if (mBookmarkManager.addBookmark(bookmark)) {
						mBookmarkList.add(bookmark);
						Collections.sort(mBookmarkList, new SortIgnoreCase());
						notifyBookmarkDataSetChanged();
						mSearchAdapter.refreshBookmarks();
					}
				}
				return true;
			case R.id.action_find:
				findInPage();
				return true;
			case R.id.action_reading_mode:
				Intent read = new Intent(this, ReadingActivity.class);
				read.putExtra(Constants.LOAD_READING_URL, mCurrentView.getUrl());
				startActivity(read);
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
		finder.setPositiveButton(getResources().getString(R.string.search_hint),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String query = getHome.getText().toString();
						if (query.length() > 0)
							showSearchInterfaceBar(query);
					}
				});
		finder.show();
	}

	private void showSearchInterfaceBar(String text) {
		if (mCurrentView != null) {
			mCurrentView.find(text);
		}
		final RelativeLayout bar = (RelativeLayout) findViewById(R.id.search_bar);
		bar.setVisibility(View.VISIBLE);

		TextView tw = (TextView) findViewById(R.id.search_query);
		tw.setText("'" + text + "'");

		ImageButton up = (ImageButton) findViewById(R.id.button_next);
		up.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCurrentView.getWebView().findNext(false);
			}
		});
		ImageButton down = (ImageButton) findViewById(R.id.button_back);
		down.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCurrentView.getWebView().findNext(true);
			}
		});

		ImageButton quit = (ImageButton) findViewById(R.id.button_quit);
		quit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCurrentView.getWebView().clearMatches();
				bar.setVisibility(View.GONE);
			}
		});
	}

	private void showCloseDialog(final int position) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
				android.R.layout.simple_dropdown_item_1line);
		adapter.add(mContext.getString(R.string.close_tab));
		adapter.add(mContext.getString(R.string.close_all_tabs));
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0:
						deleteTab(position);
						break;
					case 1:
						closeBrowser();
						break;
					default:
						break;
				}
			}
		});
		builder.show();
	}

	/**
	 * The click listener for ListView in the navigation drawer
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mIsNewIntent = false;
			selectItem(position);
		}
	}

	/**
	 * long click listener for Navigation Drawer
	 */
	private class DrawerItemLongClickListener implements ListView.OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
			showCloseDialog(position);
			return true;
		}
	}

	private class BookmarkItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (mCurrentView != null) {
				mCurrentView.loadUrl(mBookmarkList.get(position).getUrl());
			}
			// keep any jank from happening when the drawer is closed after the
			// URL starts to load
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
			}, 150);
		}
	}

	private class BookmarkItemLongClickListener implements ListView.OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mContext.getResources().getString(R.string.action_bookmarks));
			builder.setMessage(getResources().getString(R.string.dialog_bookmark))
					.setCancelable(true)
					.setPositiveButton(getResources().getString(R.string.action_new_tab),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									newTab(mBookmarkList.get(position).getUrl(), false);
									mDrawerLayout.closeDrawers();
								}
							})
					.setNegativeButton(getResources().getString(R.string.action_delete),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (mBookmarkManager.deleteBookmark(mBookmarkList.get(position)
											.getUrl())) {
										mBookmarkList.remove(position);
										notifyBookmarkDataSetChanged();
										mSearchAdapter.refreshBookmarks();
										openBookmarks();
									}
								}
							})
					.setNeutralButton(getResources().getString(R.string.action_edit),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
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
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
		homePicker.setTitle(getResources().getString(R.string.title_edit_bookmark));
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
		homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBookmarkList.get(id).setTitle(getTitle.getText().toString());
						mBookmarkList.get(id).setUrl(getUrl.getText().toString());
						mBookmarkManager.overwriteBookmarks(mBookmarkList);
						Collections.sort(mBookmarkList, new SortIgnoreCase());
						notifyBookmarkDataSetChanged();
						if (mCurrentView != null) {
							if (mCurrentView.getUrl().startsWith(Constants.FILE)
									&& mCurrentView.getUrl().endsWith("bookmarks.html")) {
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
		// Set the background color so the color mode color doesn't show through
		mBrowserFrame.setBackgroundColor(mBackgroundColor);
		if (view == null) {
			return;
		}
		mBrowserFrame.removeAllViews();
		if (mCurrentView != null) {
			mCurrentView.setForegroundTab(false);
			mCurrentView.onPause();
		}
		mCurrentView = view;
		mCurrentView.setForegroundTab(true);
		if (mCurrentView.getWebView() != null) {
			updateUrl(mCurrentView.getUrl(), true);
			updateProgress(mCurrentView.getProgress());
		} else {
			updateUrl("", true);
			updateProgress(0);
		}

		mBrowserFrame.addView(mCurrentView.getWebView(), mMatchParent);
		// Remove browser frame background to reduce overdraw
		mBrowserFrame.setBackgroundColor(0);
		mCurrentView.requestFocus();
		mCurrentView.onResume();

		// Use a delayed handler to make the transition smooth
		// otherwise it will get caught up with the showTab code
		// and cause a janky motion
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mDrawerLayout.closeDrawers();
			}
		}, 150);
	}

	/**
	 * creates a new tab with the passed in URL if it isn't null
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	public void handleNewIntent(Intent intent) {

		String url = null;
		if (intent != null) {
			url = intent.getDataString();
		}
		int num = 0;
		if (intent != null && intent.getExtras() != null) {
			num = intent.getExtras().getInt(getPackageName() + ".Origin");
		}
		if (num == 1) {
			mCurrentView.loadUrl(url);
		} else if (url != null) {
			if (url.startsWith(Constants.FILE)) {
				Utils.showToast(this, getResources().getString(R.string.message_blocked_local));
				url = null;
			}
			newTab(url, true);
			mIsNewIntent = true;
		}
	}

	@Override
	public void closeEmptyTab() {
		if (mCurrentView != null && mCurrentView.getWebView().copyBackForwardList().getSize() == 0) {
			closeCurrentTab();
		}
	}

	private void closeCurrentTab() {
		// don't delete the tab because the browser will close and mess stuff up
	}

	private void selectItem(final int position) {
		// update selected item and title, then close the drawer

		showTab(mWebViews.get(position));

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onTrimMemory(int level) {
		if (level > TRIM_MEMORY_RUNNING_MODERATE
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			Log.d(Constants.TAG, "Low Memory, Free Memory");
			for (LightningView view : mWebViews) {
				view.getWebView().freeMemory();
			}
		}
	}

	protected synchronized boolean newTab(String url, boolean show) {
		mIsNewIntent = false;
		LightningView startingTab = new LightningView(mActivity, url);
		if (mIdGenerator == 0) {
			startingTab.resumeTimers();
		}
		mIdGenerator++;
		mWebViews.add(startingTab);

		mTitleAdapter.notifyDataSetChanged();
		if (show) {
			mDrawerListLeft.setItemChecked(mWebViews.size() - 1, true);
			showTab(startingTab);
		}
		return true;
	}

	private synchronized void deleteTab(int position) {
		if (position >= mWebViews.size()) {
			return;
		}

		int current = mDrawerListLeft.getCheckedItemPosition();
		LightningView reference = mWebViews.get(position);
		if (reference == null) {
			return;
		}
		if (reference.getUrl() != null && !reference.getUrl().startsWith(Constants.FILE)) {
			mPreferences.edit().putString(PreferenceConstants.SAVE_URL, reference.getUrl()).apply();
		}
		boolean isShown = reference.isShown();
		if (current > position) {
			mWebViews.remove(position);
			mDrawerListLeft.setItemChecked(current - 1, true);
			reference.onDestroy();
		} else if (mWebViews.size() > position + 1) {
			if (current == position) {
				showTab(mWebViews.get(position + 1));
				mWebViews.remove(position);
				mDrawerListLeft.setItemChecked(position, true);
			} else {
				mWebViews.remove(position);
			}

			reference.onDestroy();
		} else if (mWebViews.size() > 1) {
			if (current == position) {
				showTab(mWebViews.get(position - 1));
				mWebViews.remove(position);
				mDrawerListLeft.setItemChecked(position - 1, true);
			} else {
				mWebViews.remove(position);
			}

			reference.onDestroy();
		} else {
			if (mCurrentView.getUrl() == null || mCurrentView.getUrl().startsWith(Constants.FILE)
					|| mCurrentView.getUrl().equals(mHomepage)) {
				closeActivity();
			} else {
				mWebViews.remove(position);
				if (mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT, false)
						&& mCurrentView != null && !isIncognito()) {
					mCurrentView.clearCache(true);
					Log.d(Constants.TAG, "Cache Cleared");

				}
				if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT, false)
						&& !isIncognito()) {
					clearHistory();
					Log.d(Constants.TAG, "History Cleared");

				}
				if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT, false)
						&& !isIncognito()) {
					clearCookies();
					Log.d(Constants.TAG, "Cookies Cleared");

				}
				reference.pauseTimers();
				reference.onDestroy();
				mCurrentView = null;
				mTitleAdapter.notifyDataSetChanged();
				finish();

			}
		}
		mTitleAdapter.notifyDataSetChanged();

		if (mIsNewIntent && isShown) {
			mIsNewIntent = false;
			closeActivity();
		}

		Log.d(Constants.TAG, "deleted tab");
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showCloseDialog(mDrawerListLeft.getCheckedItemPosition());
		}
		return true;
	}

	private void closeBrowser() {
		mBrowserFrame.setBackgroundColor(mBackgroundColor);
		if (mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT, false)
				&& mCurrentView != null && !isIncognito()) {
			mCurrentView.clearCache(true);
			Log.d(Constants.TAG, "Cache Cleared");

		}
		if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT, false)
				&& !isIncognito()) {
			clearHistory();
			Log.d(Constants.TAG, "History Cleared");

		}
		if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT, false)
				&& !isIncognito()) {
			clearCookies();
			Log.d(Constants.TAG, "Cookies Cleared");

		}
		mCurrentView = null;
		for (int n = 0; n < mWebViews.size(); n++) {
			if (mWebViews.get(n) != null) {
				mWebViews.get(n).onDestroy();
			}
		}
		mWebViews.clear();
		mTitleAdapter.notifyDataSetChanged();
		finish();
	}

	@SuppressWarnings("deprecation")
	public void clearHistory() {
		this.deleteDatabase(HistoryDatabase.DATABASE_NAME);
		WebViewDatabase m = WebViewDatabase.getInstance(this);
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
		Utils.trimCache(this);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void clearCookies() {
		CookieManager c = CookieManager.getInstance();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			c.removeAllCookies(null);
		} else {
			CookieSyncManager.createInstance(this);
			c.removeAllCookie();
		}
	}

	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
			mDrawerLayout.closeDrawer(mDrawerLeft);
		} else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
			mDrawerLayout.closeDrawer(mDrawerRight);
		} else {
			if (mCurrentView != null) {
				Log.d(Constants.TAG, "onBackPressed");
				if (mSearch.hasFocus()) {
					mCurrentView.requestFocus();
				} else if (mCurrentView.canGoBack()) {
					if (!mCurrentView.isShown()) {
						onHideCustomView();
					} else {
						mCurrentView.goBack();
					}
				} else {
					deleteTab(mDrawerListLeft.getCheckedItemPosition());
				}
			} else {
				Log.e(Constants.TAG, "This shouldn't happen ever");
				super.onBackPressed();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(Constants.TAG, "onPause");
		if (mCurrentView != null) {
			mCurrentView.pauseTimers();
			mCurrentView.onPause();
		}
	}

	public void saveOpenTabs() {
		if (mPreferences.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
			String s = "";
			for (int n = 0; n < mWebViews.size(); n++) {
				if (mWebViews.get(n).getUrl() != null) {
					s = s + mWebViews.get(n).getUrl() + "|$|SEPARATOR|$|";
				}
			}
			mPreferences.edit().putString(PreferenceConstants.URL_MEMORY, s).apply();
		}
	}

	@Override
	protected void onDestroy() {
		Log.d(Constants.TAG, "onDestroy");
		if (mHistoryDatabase != null) {
			mHistoryDatabase.close();
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(Constants.TAG, "onResume");
		if (mSearchAdapter != null) {
			mSearchAdapter.refreshPreferences();
			mSearchAdapter.refreshBookmarks();
		}
		if (mCurrentView != null) {
			mCurrentView.resumeTimers();
			mCurrentView.onResume();

			mHistoryDatabase = HistoryDatabase.getInstance(this);
			mBookmarkList = mBookmarkManager.getBookmarks(true);
			notifyBookmarkDataSetChanged();
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
				&& (query.replace(".", "").length() >= 4) && query.contains("."));
		boolean aboutScheme = query.contains("about:");
		boolean validURL = (query.startsWith("ftp://") || query.startsWith(Constants.HTTP)
				|| query.startsWith(Constants.FILE) || query.startsWith(Constants.HTTPS))
				|| isIPAddress;
		boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

		if (isIPAddress
				&& (!query.startsWith(Constants.HTTP) || !query.startsWith(Constants.HTTPS))) {
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

	public class LightningViewAdapter extends ArrayAdapter<LightningView> {

		Context context;
		ColorMatrix colorMatrix;
		ColorMatrixColorFilter filter;
		Paint paint;
		int layoutResourceId;
		List<LightningView> data = null;

		public LightningViewAdapter(Context context, int layoutResourceId, List<LightningView> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			LightningViewHolder holder = null;
			if (row == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new LightningViewHolder();
				holder.txtTitle = (TextView) row.findViewById(R.id.textTab);
				holder.favicon = (ImageView) row.findViewById(R.id.faviconTab);
				holder.exit = (ImageView) row.findViewById(R.id.deleteButton);
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

			ViewCompat.jumpDrawablesToCurrentState(holder.exit);

			LightningView web = data.get(position);
			holder.txtTitle.setText(web.getTitle());
			if (web.isForegroundTab()) {
				holder.txtTitle.setTextAppearance(context, R.style.boldText);
			} else {
				holder.txtTitle.setTextAppearance(context, R.style.normalText);
			}

			Bitmap favicon = web.getFavicon();
			if (web.isForegroundTab()) {

				holder.favicon.setImageBitmap(favicon);
				if (!isIncognito() && mColorMode)
					changeToolbarBackground(favicon);
			} else {
				Bitmap grayscaleBitmap = Bitmap.createBitmap(favicon.getWidth(),
						favicon.getHeight(), Bitmap.Config.ARGB_8888);

				Canvas c = new Canvas(grayscaleBitmap);
				if (colorMatrix == null || filter == null || paint == null) {
					paint = new Paint();
					colorMatrix = new ColorMatrix();
					colorMatrix.setSaturation(0);
					filter = new ColorMatrixColorFilter(colorMatrix);
					paint.setColorFilter(filter);
				}

				c.drawBitmap(favicon, 0, 0, paint);
				holder.favicon.setImageBitmap(grayscaleBitmap);
			}
			return row;
		}

		class LightningViewHolder {

			TextView txtTitle;

			ImageView favicon;

			ImageView exit;
		}
	}

	private void changeToolbarBackground(Bitmap favicon) {
		android.support.v7.graphics.Palette.generateAsync(favicon,
				new android.support.v7.graphics.Palette.PaletteAsyncListener() {

					@Override
					public void onGenerated(Palette palette) {
						int color = 0xff000000 | palette.getVibrantColor(mContext.getResources()
								.getColor(R.color.primary_color));

						int finalColor; // Lighten up the dark color if it is
										// too dark
						if (isColorTooDark(color)) {
							finalColor = mixTwoColors(
									mContext.getResources().getColor(R.color.primary_color), color,
									0.25f);
						} else {
							finalColor = color;
						}

						ColorDrawable draw = (ColorDrawable) mPageLayout.getBackground();
						ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(),
								draw.getColor(), finalColor);

						anim.addUpdateListener(new AnimatorUpdateListener() {

							@Override
							public void onAnimationUpdate(ValueAnimator animation) {
								int color = (Integer) animation.getAnimatedValue();
								mPageLayout.setBackgroundColor(color);
								mToolbarLayout.setBackgroundColor(color);
							}

						});

						mToolbarLayout.setBackgroundColor(finalColor);
						mPageLayout.setBackgroundColor(finalColor);
						anim.setDuration(300);
						anim.start();
					}
				});
	}

	public static boolean isColorTooDark(int color) {
		final byte RED_CHANNEL = 16;
		final byte GREEN_CHANNEL = 8;
		final byte BLUE_CHANNEL = 0;

		int r = ((int) ((float) (color >> RED_CHANNEL & 0xff) * 0.3f)) & 0xff;
		int g = ((int) ((float) (color >> GREEN_CHANNEL & 0xff) * 0.59)) & 0xff;
		int b = ((int) ((float) (color >> BLUE_CHANNEL & 0xff) * 0.11)) & 0xff;
		int gr = (r + g + b) & 0xff;
		int gray = gr + (gr << GREEN_CHANNEL) + (gr << RED_CHANNEL);

		return gray < 0x727272;
	}

	public static int mixTwoColors(int color1, int color2, float amount) {
		final byte ALPHA_CHANNEL = 24;
		final byte RED_CHANNEL = 16;
		final byte GREEN_CHANNEL = 8;
		final byte BLUE_CHANNEL = 0;

		final float inverseAmount = 1.0f - amount;

		int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) + ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) + ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int b = ((int) (((float) (color1 & 0xff) * amount) + ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

		return 0xff << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
	}

	public class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

		Context context;

		int layoutResourceId;

		List<HistoryItem> data = null;

		public BookmarkViewAdapter(Context context, int layoutResourceId, List<HistoryItem> data) {
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
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new BookmarkViewHolder();
				holder.txtTitle = (TextView) row.findViewById(R.id.textBookmark);
				holder.favicon = (ImageView) row.findViewById(R.id.faviconBookmark);
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

	private void getImage(ImageView image, HistoryItem web) {
		new DownloadImageTask(image, web).execute(web.getUrl());
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		ImageView bmImage;
		HistoryItem mWeb;

		public DownloadImageTask(ImageView bmImage, HistoryItem web) {
			this.bmImage = bmImage;
			this.mWeb = web;
		}

		protected Bitmap doInBackground(String... urls) {
			String url = urls[0];
			Bitmap mIcon = null;
			// unique path for each url that is bookmarked.
			String hash = String.valueOf(Utils.getDomainName(url).hashCode());
			File image = new File(mContext.getCacheDir(), hash + ".png");
			String urldisplay;
			try {
				urldisplay = Utils.getProtocol(url) + getDomainName(url) + "/favicon.ico";
			} catch (URISyntaxException e) {
				e.printStackTrace();
				urldisplay = "https://www.google.com/s2/favicons?domain_url=" + url;
			}
			// checks to see if the image exists
			if (!image.exists()) {
				try {
					// if not, download it...
					URL urlDownload = new URL(urldisplay);
					HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
					connection.setDoInput(true);
					connection.connect();
					InputStream in = connection.getInputStream();

					if (in != null) {
						mIcon = BitmapFactory.decodeStream(in);
					}
					// ...and cache it
					if (mIcon != null) {
						FileOutputStream fos = new FileOutputStream(image);
						mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
						fos.flush();
						fos.close();
						Log.d(Constants.TAG, "Downloaded: " + urldisplay);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// if it exists, retrieve it from the cache
				mIcon = BitmapFactory.decodeFile(image.getPath());
			}
			if (mIcon == null) {
				try {
					// if not, download it...
					URL urlDownload = new URL("https://www.google.com/s2/favicons?domain_url="
							+ url);
					HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
					connection.setDoInput(true);
					connection.connect();
					InputStream in = connection.getInputStream();

					if (in != null) {
						mIcon = BitmapFactory.decodeStream(in);
					}
					// ...and cache it
					if (mIcon != null) {
						FileOutputStream fos = new FileOutputStream(image);
						mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
						fos.flush();
						fos.close();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (mIcon == null) {
				return mWebpageBitmap;
			} else {
				return mIcon;
			}
		}

		protected void onPostExecute(Bitmap result) {
			Bitmap fav = Utils.padFavicon(result);
			bmImage.setImageBitmap(fav);
			mWeb.setBitmap(fav);
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
	public void updateUrl(String url, boolean shortUrl) {
		if (url == null || mSearch == null || mSearch.hasFocus()) {
			return;
		}
		if (shortUrl && !url.startsWith(Constants.FILE)) {
			switch (mPreferences.getInt(PreferenceConstants.URL_BOX_CONTENTS, 0)) {
				case 0: // Default, show only the domain
					url = url.replaceFirst(Constants.HTTP, "");
					url = Utils.getDomainName(url);
					mSearch.setText(url);
					break;
				case 1: // URL, show the entire URL
					mSearch.setText(url);
					break;
				case 2: // Title, show the page's title
					if (mCurrentView != null && !mCurrentView.getTitle().isEmpty()) {
						mSearch.setText(mCurrentView.getTitle());
					} else {
						mSearch.setText(mUntitledTitle);
					}
					break;
			}

		} else {
			if (url.startsWith(Constants.FILE)) {
				url = "";
			}
			mSearch.setText(url);
		}
	}

	@Override
	public void updateProgress(int n) {
		/*
		 * if (n > mProgressBar.getProgress()) { ObjectAnimator animator =
		 * ObjectAnimator.ofInt(mProgressBar, "progress", n);
		 * animator.setDuration(200); animator.setInterpolator(new
		 * DecelerateInterpolator()); animator.start(); } else if (n <
		 * mProgressBar.getProgress()) { ObjectAnimator animator =
		 * ObjectAnimator.ofInt(mProgressBar, "progress", 0, n);
		 * animator.setDuration(200); animator.setInterpolator(new
		 * DecelerateInterpolator()); animator.start(); } if (n >= 100) {
		 * Handler handler = new Handler(); handler.postDelayed(new Runnable() {
		 * 
		 * @Override public void run() {
		 * mProgressBar.setVisibility(View.INVISIBLE); setIsFinishedLoading(); }
		 * }, 200);
		 * 
		 * } else { mProgressBar.setVisibility(View.VISIBLE); setIsLoading(); }
		 */
		if (n >= 100) {
			setIsFinishedLoading();
		} else {
			setIsLoading();
		}
		mProgressBar.setProgress(n);
	}

	@Override
	public void updateHistory(final String title, final String url) {

	}

	public void addItemToHistory(final String title, final String url) {
		Runnable update = new Runnable() {
			@Override
			public void run() {
				if (isSystemBrowserAvailable()
						&& mPreferences.getBoolean(PreferenceConstants.SYNC_HISTORY, true)) {
					try {
						Browser.updateVisitedHistory(getContentResolver(), url, true);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					if (mHistoryDatabase == null) {
						mHistoryDatabase = HistoryDatabase.getInstance(mContext);
					}
					mHistoryDatabase.visitHistoryItem(url, title);
				} catch (IllegalStateException e) {
					Log.e(Constants.TAG, "IllegalStateException in updateHistory");
				} catch (NullPointerException e) {
					Log.e(Constants.TAG, "NullPointerException in updateHistory");
				} catch (SQLiteException e) {
					Log.e(Constants.TAG, "SQLiteException in updateHistory");
				}
			}
		};
		if (url != null && !url.startsWith(Constants.FILE)) {
			new Thread(update).start();
		}
	}

	public boolean isSystemBrowserAvailable() {
		return mSystemBrowser;
	}

	public boolean getSystemBrowser() {
		Cursor c = null;
		String[] columns = new String[] { "url", "title" };
		boolean browserFlag = false;
		try {

			Uri bookmarks = Browser.BOOKMARKS_URI;
			c = getContentResolver().query(bookmarks, columns, null, null, null);
		} catch (SQLiteException ignored) {
		} catch (IllegalStateException ignored) {
		} catch (NullPointerException ignored) {
		}

		if (c != null) {
			Log.d("Browser", "System Browser Available");
			browserFlag = true;
		} else {
			Log.e("Browser", "System Browser Unavailable");
			browserFlag = false;
		}
		if (c != null) {
			c.close();
			c = null;
		}
		mPreferences.edit().putBoolean(PreferenceConstants.SYSTEM_BROWSER_PRESENT, browserFlag)
				.apply();
		return browserFlag;
	}

	/**
	 * method to generate search suggestions for the AutoCompleteTextView from
	 * previously searched URLs
	 */
	private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

		getUrl.setThreshold(1);
		getUrl.setDropDownWidth(-1);
		getUrl.setDropDownAnchor(R.id.toolbar_layout);
		getUrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				try {
					String url;
					url = ((TextView) arg1.findViewById(R.id.url)).getText().toString();
					if (url.startsWith(mContext.getString(R.string.suggestion))) {
						url = ((TextView) arg1.findViewById(R.id.title)).getText().toString();
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
					Log.e("Browser Error: ", "NullPointerException on item click");
				}
			}

		});

		getUrl.setSelectAllOnFocus(true);
		mSearchAdapter = new SearchAdapter(mContext, isIncognito());
		getUrl.setAdapter(mSearchAdapter);
	}

	@Override
	public boolean isIncognito() {
		return false;
	}

	/**
	 * function that opens the HTML history page in the browser
	 */
	private void openHistory() {
		// use a thread so that history retrieval doesn't block the UI
		Thread history = new Thread(new Runnable() {

			@Override
			public void run() {
				mCurrentView.loadUrl(HistoryPage.getHistoryPage(mContext));
				mSearch.setText("");
			}

		});
		history.run();
	}

	/**
	 * helper function that opens the bookmark drawer
	 */
	private void openBookmarks() {
		if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
			mDrawerLayout.closeDrawers();
		}
		mDrawerLayout.openDrawer(mDrawerRight);
	}

	public void closeDrawers() {
		mDrawerLayout.closeDrawers();
	}

	@Override
	/**
	 * open the HTML bookmarks page, parameter view is the WebView that should show the page
	 */
	public void openBookmarkPage(WebView view) {
		String bookmarkHtml = BookmarkPage.HEADING;
		Iterator<HistoryItem> iter = mBookmarkList.iterator();
		HistoryItem helper;
		while (iter.hasNext()) {
			helper = iter.next();
			bookmarkHtml += (BookmarkPage.PART1 + helper.getUrl() + BookmarkPage.PART2
					+ helper.getUrl() + BookmarkPage.PART3 + helper.getTitle() + BookmarkPage.PART4);
		}
		bookmarkHtml += BookmarkPage.END;
		File bookmarkWebPage = new File(mContext.getFilesDir(), BookmarkPage.FILENAME);
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkHtml);
			bookWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		view.loadUrl(Constants.FILE + bookmarkWebPage);
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
		startActivityForResult(Intent.createChooser(i, getString(R.string.title_file_chooser)), 1);
	}

	@Override
	/**
	 * used to allow uploading into the browser
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 1) {
			if (null == mUploadMessage) {
				return;
			}
			Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
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
			mCurrentView.getWebView().requestFocusNodeHref(click);
		}
	}

	@Override
	public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
		if (view == null) {
			return;
		}
		if (mCustomView != null && callback != null) {
			callback.onCustomViewHidden();
			return;
		}
		try {
			view.setKeepScreenOn(true);
		} catch (SecurityException e) {
			Log.e(Constants.TAG, "WebView is not allowed to keep the screen on");
		}
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
				mVideoView.setOnCompletionListener(new VideoCompletionListener());
			}
		}
		mCustomViewCallback = callback;
	}

	@Override
	public void onHideCustomView() {
		if (mCustomView == null || mCustomViewCallback == null || mCurrentView == null) {
			return;
		}
		Log.d(Constants.TAG, "onHideCustomView");
		mCurrentView.setVisibility(View.VISIBLE);
		try {
			mCustomView.setKeepScreenOn(false);
		} catch (SecurityException e) {
			Log.e(Constants.TAG, "WebView is not allowed to keep the screen on");
		}
		setFullscreen(mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false));
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

	private class VideoCompletionListener implements MediaPlayer.OnCompletionListener,
			MediaPlayer.OnErrorListener {

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
				mBrowserFrame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
			setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
		}

		@SuppressLint("ClickableViewAccessibility")
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

	@SuppressLint("InflateParams")
	@Override
	/**
	 * dumb method that returns the loading progress for a video
	 */
	public View getVideoLoadingProgressView() {
		if (mVideoProgressView == null) {
			LayoutInflater inflater = LayoutInflater.from(this);
			mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, null);
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
		if (newTab("", true)) {
			WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
			transport.setWebView(mCurrentView.getWebView());
			resultMsg.sendToTarget();
		}
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
		if (mFullScreen) {
			if (mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
				mUiLayout.removeView(mToolbarLayout);
				mBrowserFrame.addView(mToolbarLayout);
				mToolbarLayout.bringToFront();
				Log.d(Constants.TAG, "Move view to browser frame");
			}
			if (mToolbarLayout.getVisibility() != View.GONE) {

				Animation hide = AnimationUtils.loadAnimation(mContext, R.anim.slide_up);
				hide.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						mToolbarLayout.setVisibility(View.GONE);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

				});
				mToolbarLayout.startAnimation(hide);
				Log.d(Constants.TAG, "Hide");
			}
		}
	}

	@Override
	public void toggleActionBar() {
		if (mFullScreen) {
			if (mToolbarLayout.getVisibility() != View.VISIBLE) {
				showActionBar();
			} else {
				hideActionBar();
			}
		}
	}

	@Override
	/**
	 * obviously it shows the action bar if it's hidden
	 */
	public void showActionBar() {
		if (mFullScreen) {
			if (mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
				mUiLayout.removeView(mToolbarLayout);
				mBrowserFrame.addView(mToolbarLayout);
				mToolbarLayout.bringToFront();
				Log.d(Constants.TAG, "Move view to browser frame");
			}
			if (mToolbarLayout.getVisibility() != View.VISIBLE) {
				Animation show = AnimationUtils.loadAnimation(mContext, R.anim.slide_down);
				show.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						mToolbarLayout.setVisibility(View.VISIBLE);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

				});
				mToolbarLayout.startAnimation(show);
				Log.d(Constants.TAG, "Show");
			}

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
								case DialogInterface.BUTTON_POSITIVE:
									newTab(url, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(url);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									if (API > 8) {
										Utils.downloadFile(mActivity, url,
												mCurrentView.getUserAgent(), "attachment", false);
									}
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(url.replace(Constants.HTTP, ""))
							.setMessage(getResources().getString(R.string.dialog_image))
							.setPositiveButton(getResources().getString(R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									newTab(url, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(url);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									ClipData clip = ClipData.newPlainText("label", url);
									clipboard.setPrimaryClip(clip);
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(url)
							.setMessage(getResources().getString(R.string.dialog_link))
							.setPositiveButton(getResources().getString(R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_copy),
									dialogClickListener).show();
				}
			} else {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								newTab(url, false);
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								mCurrentView.loadUrl(url);
								break;

							case DialogInterface.BUTTON_NEUTRAL:
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label", url);
								clipboard.setPrimaryClip(clip);

								break;
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
				builder.setTitle(url)
						.setMessage(getResources().getString(R.string.dialog_link))
						.setPositiveButton(getResources().getString(R.string.action_new_tab),
								dialogClickListener)
						.setNegativeButton(getResources().getString(R.string.action_open),
								dialogClickListener)
						.setNeutralButton(getResources().getString(R.string.action_copy),
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
								case DialogInterface.BUTTON_POSITIVE:
									newTab(newUrl, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(newUrl);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									if (API > 8) {
										Utils.downloadFile(mActivity, newUrl,
												mCurrentView.getUserAgent(), "attachment", false);
									}
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(newUrl.replace(Constants.HTTP, ""))
							.setMessage(getResources().getString(R.string.dialog_image))
							.setPositiveButton(getResources().getString(R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									newTab(newUrl, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(newUrl);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									ClipData clip = ClipData.newPlainText("label", newUrl);
									clipboard.setPrimaryClip(clip);

									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(newUrl)
							.setMessage(getResources().getString(R.string.dialog_link))
							.setPositiveButton(getResources().getString(R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_copy),
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

	@Override
	public int getMenu() {
		return R.menu.main;
	}
}
