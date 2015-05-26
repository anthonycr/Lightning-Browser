/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.provider.MediaStore;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewDatabase;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.VideoView;

import net.i2p.android.ui.I2PAndroidHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.controller.BrowserController;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.object.ClickHandler;
import acr.browser.lightning.object.DrawerArrowDrawable;
import acr.browser.lightning.object.SearchAdapter;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.view.AnimatedProgressBar;
import acr.browser.lightning.view.LightningView;
import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;

public class BrowserActivity extends ThemableActivity implements BrowserController, OnClickListener {

	// Layout
	private DrawerLayout mDrawerLayout;
	private FrameLayout mBrowserFrame;
	private FullscreenHolder mFullscreenContainer;
	private ListView mDrawerListLeft, mDrawerListRight;
	private LinearLayout mDrawerLeft, mDrawerRight, mUiLayout, mToolbarLayout;
	private RelativeLayout mSearchBar;

	// List
	private final List<LightningView> mWebViews = new ArrayList<>();
	private List<HistoryItem> mBookmarkList;
	private LightningView mCurrentView;

	private AnimatedProgressBar mProgressBar;
	private AutoCompleteTextView mSearch;
	private ImageView mArrowImage;
	private VideoView mVideoView;
	private View mCustomView, mVideoProgressView;

	// Adapter
	private BookmarkViewAdapter mBookmarkAdapter;
	private LightningViewAdapter mTitleAdapter;
	private SearchAdapter mSearchAdapter;

	// Callback
	private ClickHandler mClickHandler;
	private CustomViewCallback mCustomViewCallback;
	private ValueCallback<Uri> mUploadMessage;
	private ValueCallback<Uri[]> mFilePathCallback;

	// Context
	private Activity mActivity;

	// Native
	private boolean mSystemBrowser = false, mIsNewIntent = false, mFullScreen, mColorMode,
			mDarkTheme;
	private int mOriginalOrientation, mBackgroundColor, mIdGenerator;
	private String mSearchText, mUntitledTitle, mHomepage, mCameraPhotoPath;

	// Storage
	private HistoryDatabase mHistoryDatabase;
	private BookmarkManager mBookmarkManager;
	private PreferenceManager mPreferences;

	// Image
	private Bitmap mDefaultVideoPoster, mWebpageBitmap;
	private final ColorDrawable mBackground = new ColorDrawable();
	private Drawable mDeleteIcon, mRefreshIcon, mCopyIcon, mIcon;
	private DrawerArrowDrawable mArrowDrawable;

	// Helper
	private I2PAndroidHelper mI2PHelper;
	private boolean mI2PHelperBound;
	private boolean mI2PProxyInitialized;

	// Constant
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private static final LayoutParams MATCH_PARENT = new LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);
	private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private synchronized void initialize() {
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();

		mPreferences = PreferenceManager.getInstance();
		mDarkTheme = mPreferences.getUseDarkTheme() || isIncognito();
		mActivity = this;
		mWebViews.clear();

		mClickHandler = new ClickHandler(this);
		mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
		mToolbarLayout = (LinearLayout) findViewById(R.id.toolbar_layout);
		// initialize background ColorDrawable
		mBackground.setColor(((ColorDrawable) mToolbarLayout.getBackground()).getColor());

		mUiLayout = (LinearLayout) findViewById(R.id.ui_layout);
		mProgressBar = (AnimatedProgressBar) findViewById(R.id.progress_view);
		RelativeLayout newTab = (RelativeLayout) findViewById(R.id.new_tab_button);
		mDrawerLeft = (LinearLayout) findViewById(R.id.left_drawer);
		// Drawer stutters otherwise
		mDrawerLeft.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerListLeft = (ListView) findViewById(R.id.left_drawer_list);
		mDrawerRight = (LinearLayout) findViewById(R.id.right_drawer);
		mDrawerRight.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);

		setNavigationDrawerWidth();
		mDrawerLayout.setDrawerListener(new DrawerLocker());

		mWebpageBitmap = Utils.getWebpageBitmap(getResources(), mDarkTheme);

		mHomepage = mPreferences.getHomepage();

		mTitleAdapter = new LightningViewAdapter(this, R.layout.tab_list_item, mWebViews);
		mDrawerListLeft.setAdapter(mTitleAdapter);
		mDrawerListLeft.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerListLeft.setOnItemLongClickListener(new DrawerItemLongClickListener());

		mDrawerListRight.setOnItemClickListener(new BookmarkItemClickListener());
		mDrawerListRight.setOnItemLongClickListener(new BookmarkItemLongClickListener());

		mHistoryDatabase = HistoryDatabase.getInstance(getApplicationContext());

		// set display options of the ActionBar
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(R.layout.toolbar_content);

		View v = actionBar.getCustomView();
		LayoutParams lp = v.getLayoutParams();
		lp.width = LayoutParams.MATCH_PARENT;
		v.setLayoutParams(lp);

		mArrowDrawable = new DrawerArrowDrawable(this);
		mArrowImage = (ImageView) actionBar.getCustomView().findViewById(R.id.arrow);
		// Use hardware acceleration for the animation
		mArrowImage.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mArrowImage.setImageDrawable(mArrowDrawable);
		LinearLayout arrowButton = (LinearLayout) actionBar.getCustomView().findViewById(
				R.id.arrow_button);
		arrowButton.setOnClickListener(this);

		mI2PHelper = new I2PAndroidHelper(this);

		RelativeLayout back = (RelativeLayout) findViewById(R.id.action_back);
		back.setOnClickListener(this);

		RelativeLayout forward = (RelativeLayout) findViewById(R.id.action_forward);
		forward.setOnClickListener(this);

		// create the search EditText in the ToolBar
		mSearch = (AutoCompleteTextView) actionBar.getCustomView().findViewById(R.id.search);
		mUntitledTitle = getString(R.string.untitled);
		mBackgroundColor = getResources().getColor(R.color.primary_color);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete);
			mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh);
			mCopyIcon = getResources().getDrawable(R.drawable.ic_action_copy);
		} else {
			Theme theme = getTheme();
			mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete, theme);
			mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh, theme);
			mCopyIcon = getResources().getDrawable(R.drawable.ic_action_copy, theme);
		}

		int iconBounds = Utils.convertDpToPixels(24);
		mDeleteIcon.setBounds(0, 0, iconBounds, iconBounds);
		mRefreshIcon.setBounds(0, 0, iconBounds, iconBounds);
		mCopyIcon.setBounds(0, 0, iconBounds, iconBounds);
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
				mBookmarkManager = BookmarkManager.getInstance(mActivity.getApplicationContext());
				mBookmarkList = mBookmarkManager.getBookmarks(true);
				if (mBookmarkList.size() == 0 && mPreferences.getDefaultBookmarks()) {
					for (String[] array : BookmarkManager.DEFAULT_BOOKMARKS) {
						HistoryItem bookmark = new HistoryItem(array[0], array[1]);
						if (mBookmarkManager.addBookmark(bookmark)) {
							mBookmarkList.add(bookmark);
						}
					}
					Collections.sort(mBookmarkList, new SortIgnoreCase());
					mPreferences.setDefaultBookmarks(false);
				}
				mBookmarkAdapter = new BookmarkViewAdapter(mActivity, R.layout.bookmark_list_item,
						mBookmarkList);
				mDrawerListRight.setAdapter(mBookmarkAdapter);
				initializeSearchSuggestions(mSearch);
			}

		});
		initialize.run();

		newTab.setOnClickListener(this);
		newTab.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				String url = mPreferences.getSavedUrl();
				if (url != null) {
					newTab(url, true);
					Toast.makeText(mActivity, R.string.deleted_tab, Toast.LENGTH_SHORT).show();
				}
				mPreferences.setSavedUrl(null);
				return true;
			}

		});

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);
		initializeTabs();

		if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
		}

		checkForProxy();
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
										mActivity,
										mActivity.getResources().getString(
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

	/*
	 * If Orbot/Tor or I2P is installed, prompt the user if they want to enable
	 * proxying for this session
	 */
	private void checkForProxy() {
		boolean useProxy = mPreferences.getUseProxy();

		OrbotHelper oh = new OrbotHelper(this);
		final boolean orbotInstalled = oh.isOrbotInstalled();
		boolean orbotChecked = mPreferences.getCheckedForTor();
		boolean orbot = orbotInstalled && !orbotChecked;

		boolean i2pInstalled = mI2PHelper.isI2PAndroidInstalled();
		boolean i2pChecked = mPreferences.getCheckedForI2P();
		boolean i2p = i2pInstalled && !i2pChecked;

		// TODO Is the idea to show this per-session, or only once?
		if (!useProxy && (orbot || i2p)) {
			if (orbot) mPreferences.setCheckedForTor(true);
			if (i2p) mPreferences.setCheckedForI2P(true);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			if (orbotInstalled && i2pInstalled) {
				String[] proxyChoices = this.getResources().getStringArray(R.array.proxy_choices_array);
				builder.setTitle(getResources().getString(R.string.http_proxy))
						.setSingleChoiceItems(proxyChoices, mPreferences.getProxyChoice(),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mPreferences.setProxyChoice(which);
									}
								})
						.setNeutralButton(getResources().getString(R.string.action_ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										if (mPreferences.getUseProxy())
											initializeProxy();
									}
								});
			} else {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								mPreferences.setProxyChoice(orbotInstalled ? 1 : 2);
								initializeProxy();
								break;
							case DialogInterface.BUTTON_NEGATIVE:
								mPreferences.setProxyChoice(0);
								break;
						}
					}
				};

				builder.setMessage(orbotInstalled ? R.string.use_tor_prompt : R.string.use_i2p_prompt)
						.setPositiveButton(R.string.yes, dialogClickListener)
						.setNegativeButton(R.string.no, dialogClickListener);
			}
			builder.show();
		}
	}

	/*
	 * Initialize WebKit Proxying
	 */
	private void initializeProxy() {
		String host;
		int port;

		switch (mPreferences.getProxyChoice()) {
			case 0:
				// We shouldn't be here
				return;

			case 1:
				OrbotHelper oh = new OrbotHelper(this);
				if (!oh.isOrbotRunning()) {
					oh.requestOrbotStart(this);
				}
				host = "localhost";
				port = 8118;
				break;

			case 2:
				mI2PProxyInitialized = true;
				if (mI2PHelperBound && !mI2PHelper.isI2PAndroidRunning()) {
					mI2PHelper.requestI2PAndroidStart(this);
				}
				host = "localhost";
				port = 4444;
				break;

			default:
				host = mPreferences.getProxyHost();
				port = mPreferences.getProxyPort();
		}

		try {
			WebkitProxy.setProxy(BrowserApp.class.getName(), getApplicationContext(),
					host, port);
		} catch (Exception e) {
			Log.d(Constants.TAG, "error enabling web proxying", e);
		}

	}

	public boolean isProxyReady() {
		if (mPreferences.getProxyChoice() == 2) {
			if (!mI2PHelper.isI2PAndroidRunning()) {
				Utils.showToast(this, getString(R.string.i2p_not_running));
				return false;
			} else if (!mI2PHelper.areTunnelsActive()) {
				Utils.showToast(this, getString(R.string.i2p_tunnels_not_ready));
				return false;
			}
		}

		return true;
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
		if (mPreferences.getRestoreLostTabsEnabled()) {
			String mem = mPreferences.getMemoryUrl();
			mPreferences.setMemoryUrl("");
			String[] array = Utils.getArray(mem);
			int count = 0;
			for (String urlString : array) {
				if (urlString.length() > 0) {
					if (url != null && url.compareTo(urlString) == 0) {
						url = null;
					}
					newTab(urlString, true);
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
			mPreferences = PreferenceManager.getInstance();
		}
		mFullScreen = mPreferences.getFullScreenEnabled();
		mColorMode = mPreferences.getColorModeEnabled();
		mColorMode &= !mDarkTheme;
		if (!isIncognito() && !mColorMode && !mDarkTheme && mWebpageBitmap != null) {
			changeToolbarBackground(mWebpageBitmap);
		} else if (!isIncognito() && mCurrentView != null && !mDarkTheme
				&& mCurrentView.getFavicon() != null) {
			changeToolbarBackground(mCurrentView.getFavicon());
		}

		if (mFullScreen && mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
			mUiLayout.removeView(mToolbarLayout);
			mBrowserFrame.addView(mToolbarLayout);
			mToolbarLayout.bringToFront();
		} else if (mBrowserFrame.findViewById(R.id.toolbar_layout) != null) {
			mBrowserFrame.removeView(mToolbarLayout);
			mUiLayout.addView(mToolbarLayout, 0);
		}
		if (mPreferences.getHideStatusBarEnabled()) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		switch (mPreferences.getSearchChoice()) {
			case 0:
				mSearchText = mPreferences.getSearchUrl();
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
		if (mPreferences.getUseProxy()) {
			initializeProxy();
		} else {
			try {
				WebkitProxy.resetProxy(BrowserApp.class.getName(),
						getApplicationContext());
			} catch (Exception e) {
				e.printStackTrace();
			}
			mI2PProxyInitialized = false;
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
		} else if ((keyCode == KeyEvent.KEYCODE_MENU) && (Build.VERSION.SDK_INT <= 16)
				&& (Build.MANUFACTURER.compareTo("LGE") == 0)) {
			// Workaround for stupid LG devices that crash
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_MENU) && (Build.VERSION.SDK_INT <= 16)
				&& (Build.MANUFACTURER.compareTo("LGE") == 0)) {
			// Workaround for stupid LG devices that crash
			openOptionsMenu();
			return true;
		}
		return super.onKeyUp(keyCode, event);
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
						Utils.showToast(mActivity,
								mActivity.getResources().getString(R.string.message_link_copied));
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
		mSearchBar = (RelativeLayout) findViewById(R.id.search_bar);
		mSearchBar.setVisibility(View.VISIBLE);

		TextView tw = (TextView) findViewById(R.id.search_query);
		tw.setText("'" + text + "'");

		ImageButton up = (ImageButton) findViewById(R.id.button_next);
		up.setOnClickListener(this);

		ImageButton down = (ImageButton) findViewById(R.id.button_back);
		down.setOnClickListener(this);

		ImageButton quit = (ImageButton) findViewById(R.id.button_quit);
		quit.setOnClickListener(this);
	}

	private void showCloseDialog(final int position) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity,
				android.R.layout.simple_dropdown_item_1line);
		adapter.add(mActivity.getString(R.string.close_tab));
		adapter.add(mActivity.getString(R.string.close_all_tabs));
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
			showTab(mWebViews.get(position));
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
			builder.setTitle(mActivity.getResources().getString(R.string.action_bookmarks));
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
		final EditText getTitle = new EditText(mActivity);
		getTitle.setHint(getResources().getString(R.string.hint_title));
		getTitle.setText(mBookmarkList.get(id).getTitle());
		getTitle.setSingleLine();
		final EditText getUrl = new EditText(mActivity);
		getUrl.setHint(getResources().getString(R.string.hint_url));
		getUrl.setText(mBookmarkList.get(id).getUrl());
		getUrl.setSingleLine();
		LinearLayout layout = new LinearLayout(mActivity);
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

		mBrowserFrame.addView(mCurrentView.getWebView(), MATCH_PARENT);
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

	@SuppressWarnings("deprecation")
	@Override
	public void onTrimMemory(int level) {
		if (level > TRIM_MEMORY_MODERATE && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			Log.d(Constants.TAG, "Low Memory, Free Memory");
			for (LightningView view : mWebViews) {
				view.getWebView().freeMemory();
			}
		}
	}

	protected synchronized boolean newTab(String url, boolean show) {
		// Limit number of tabs for limited version of app
		if (!Constants.FULL_VERSION && mWebViews.size() >= 10) {
			Utils.showToast(this, this.getString(R.string.max_tabs));
			return false;
		}
		mIsNewIntent = false;
		LightningView startingTab = new LightningView(mActivity, url, mDarkTheme);
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
		if (reference.getUrl() != null && !reference.getUrl().startsWith(Constants.FILE)
				&& !isIncognito()) {
			mPreferences.setSavedUrl(reference.getUrl());
		}
		boolean isShown = reference.isShown();
		if (isShown) {
			mBrowserFrame.setBackgroundColor(mBackgroundColor);
		}
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
				if (mPreferences.getClearCacheExit() && mCurrentView != null && !isIncognito()) {
					mCurrentView.clearCache(true);
					Log.d(Constants.TAG, "Cache Cleared");

				}
				if (mPreferences.getClearHistoryExitEnabled() && !isIncognito()) {
					clearHistory();
					Log.d(Constants.TAG, "History Cleared");

				}
				if (mPreferences.getClearCookiesExitEnabled() && !isIncognito()) {
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
		if (mPreferences.getClearCacheExit() && mCurrentView != null && !isIncognito()) {
			mCurrentView.clearCache(true);
			Log.d(Constants.TAG, "Cache Cleared");

		}
		if (mPreferences.getClearHistoryExitEnabled() && !isIncognito()) {
			clearHistory();
			Log.d(Constants.TAG, "History Cleared");

		}
		if (mPreferences.getClearCookiesExitEnabled() && !isIncognito()) {
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
		// TODO Break out web storage deletion into its own option/action
		// TODO clear web storage for all sites that are visited in Incognito mode
		WebStorage storage = WebStorage.getInstance();
		storage.deleteAllData();
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
		if (mPreferences.getRestoreLostTabsEnabled()) {
			String s = "";
			for (int n = 0; n < mWebViews.size(); n++) {
				if (mWebViews.get(n).getUrl() != null) {
					s = s + mWebViews.get(n).getUrl() + "|$|SEPARATOR|$|";
				}
			}
			mPreferences.setMemoryUrl(s);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mI2PHelper.unbind();
		mI2PHelperBound = false;
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
	protected void onStart() {
		super.onStart();
		if (mPreferences.getProxyChoice() == 2) {
			// Try to bind to I2P Android
			mI2PHelper.bind(new I2PAndroidHelper.Callback() {
				@Override
				public void onI2PAndroidBound() {
					mI2PHelperBound = true;
					if (mI2PProxyInitialized && !mI2PHelper.isI2PAndroidRunning())
						mI2PHelper.requestI2PAndroidStart(BrowserActivity.this);
				}
			});
		}
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

			mHistoryDatabase = HistoryDatabase.getInstance(getApplicationContext());
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

		supportInvalidateOptionsMenu();
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

		final Context context;
		ColorMatrix colorMatrix;
		ColorMatrixColorFilter filter;
		Paint paint;
		final int layoutResourceId;
		List<LightningView> data = null;
		final CloseTabListener mExitListener;

		public LightningViewAdapter(Context context, int layoutResourceId, List<LightningView> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
			this.mExitListener = new CloseTabListener();
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			LightningViewHolder holder;
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

			holder.exit.setTag(position);
			holder.exit.setOnClickListener(mExitListener);

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

	private class CloseTabListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			deleteTab((int) v.getTag());
		}

	}

	private void changeToolbarBackground(Bitmap favicon) {
		Palette.from(favicon).generate(new Palette.PaletteAsyncListener() {
			@Override
			public void onGenerated(Palette palette) {

				// OR with opaque black to remove transparency glitches
				int color = 0xff000000 | palette.getVibrantColor(mActivity.getResources()
						.getColor(R.color.primary_color));

				int finalColor; // Lighten up the dark color if it is
				// too dark
				if (isColorTooDark(color)) {
					finalColor = mixTwoColors(
							mActivity.getResources().getColor(R.color.primary_color),
							color, 0.25f);
				} else {
					finalColor = color;
				}

				ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(),
						mBackground.getColor(), finalColor);

				anim.addUpdateListener(new AnimatorUpdateListener() {

					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						int color = (Integer) animation.getAnimatedValue();
						mBackground.setColor(color);
						getWindow().setBackgroundDrawable(mBackground);
						mToolbarLayout.setBackgroundColor(color);
					}

				});

				anim.setDuration(300);
				anim.start();

			}
		});
	}

	public static boolean isColorTooDark(int color) {
		final byte RED_CHANNEL = 16;
		final byte GREEN_CHANNEL = 8;
		//final byte BLUE_CHANNEL = 0;

		int r = ((int) ((float) (color >> RED_CHANNEL & 0xff) * 0.3f)) & 0xff;
		int g = ((int) ((float) (color >> GREEN_CHANNEL & 0xff) * 0.59)) & 0xff;
		int b = ((int) ((float) (color & 0xff) * 0.11)) & 0xff;
		int gr = (r + g + b) & 0xff;
		int gray = gr + (gr << GREEN_CHANNEL) + (gr << RED_CHANNEL);

		return gray < 0x727272;
	}

	public static int mixTwoColors(int color1, int color2, float amount) {
		final byte ALPHA_CHANNEL = 24;
		final byte RED_CHANNEL = 16;
		final byte GREEN_CHANNEL = 8;
		//final byte BLUE_CHANNEL = 0;

		final float inverseAmount = 1.0f - amount;

		int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) + ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) + ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int b = ((int) (((float) (color1 & 0xff) * amount) + ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

		return 0xff << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b;
	}

	public class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

		final Context context;
		List<HistoryItem> data = null;
		final int layoutResourceId;

		public BookmarkViewAdapter(Context context, int layoutResourceId, List<HistoryItem> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			BookmarkViewHolder holder;

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

		final ImageView bmImage;
		final HistoryItem mWeb;

		public DownloadImageTask(ImageView bmImage, HistoryItem web) {
			this.bmImage = bmImage;
			this.mWeb = web;
		}

		protected Bitmap doInBackground(String... urls) {
			String url = urls[0];
			Bitmap mIcon = null;
			// unique path for each url that is bookmarked.
			String hash = String.valueOf(Utils.getDomainName(url).hashCode());
			File image = new File(mActivity.getCacheDir(), hash + ".png");
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
			switch (mPreferences.getUrlBoxContentChoice()) {
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
				if (isSystemBrowserAvailable() && mPreferences.getSyncHistoryEnabled()) {
					try {
						Browser.updateVisitedHistory(getContentResolver(), url, true);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					if (mHistoryDatabase == null) {
						mHistoryDatabase = HistoryDatabase.getInstance(mActivity);
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
		boolean browserFlag;
		try {

			Uri bookmarks = Browser.BOOKMARKS_URI;
			c = getContentResolver().query(bookmarks, columns, null, null, null);
		} catch (SQLiteException | IllegalStateException | NullPointerException e) {
			e.printStackTrace();
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
		}
		mPreferences.setSystemBrowserPresent(browserFlag);
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
					if (url.startsWith(mActivity.getString(R.string.suggestion))) {
						url = ((TextView) arg1.findViewById(R.id.title)).getText().toString();
					} else {
						getUrl.setText(url);
					}
					searchTheWeb(url);
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
		mSearchAdapter = new SearchAdapter(mActivity, mDarkTheme, isIncognito());
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
				mCurrentView.loadUrl(HistoryPage.getHistoryPage(mActivity));
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
		StringBuilder bookmarkBuilder = new StringBuilder();
		bookmarkBuilder.append(BookmarkPage.HEADING);
		Iterator<HistoryItem> iter = mBookmarkList.iterator();
		HistoryItem helper;
		while (iter.hasNext()) {
			helper = iter.next();
			bookmarkBuilder.append(BookmarkPage.PART1);
			bookmarkBuilder.append(helper.getUrl());
			bookmarkBuilder.append(BookmarkPage.PART2);
			bookmarkBuilder.append(helper.getUrl());
			bookmarkBuilder.append(BookmarkPage.PART3);
			bookmarkBuilder.append(helper.getTitle());
			bookmarkBuilder.append(BookmarkPage.PART4);
		}
		bookmarkBuilder.append(BookmarkPage.END);
		File bookmarkWebPage = new File(mActivity.getFilesDir(), BookmarkPage.FILENAME);
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkBuilder.toString());
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
		if (API < Build.VERSION_CODES.LOLLIPOP) {
			if (requestCode == 1) {
				if (null == mUploadMessage) {
					return;
				}
				Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
				mUploadMessage.onReceiveValue(result);
				mUploadMessage = null;

			}
		}

		if (requestCode != 1 || mFilePathCallback == null) {
			super.onActivityResult(requestCode, resultCode, intent);
			return;
		}

		Uri[] results = null;

		// Check that the response is a good one
		if (resultCode == Activity.RESULT_OK) {
			if (intent == null) {
				// If there is not data, then we may have taken a photo
				if (mCameraPhotoPath != null) {
					results = new Uri[] { Uri.parse(mCameraPhotoPath) };
				}
			} else {
				String dataString = intent.getDataString();
				if (dataString != null) {
					results = new Uri[] { Uri.parse(dataString) };
				}
			}
		}

		mFilePathCallback.onReceiveValue(results);
		mFilePathCallback = null;
	}

	@Override
	public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
		if (mFilePathCallback != null) {
			mFilePathCallback.onReceiveValue(null);
		}
		mFilePathCallback = filePathCallback;

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				photoFile = Utils.createImageFile();
				takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
			} catch (IOException ex) {
				// Error occurred while creating the File
				Log.e(Constants.TAG, "Unable to create Image File", ex);
			}

			// Continue only if the File was successfully created
			if (photoFile != null) {
				mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
			} else {
				takePictureIntent = null;
			}
		}

		Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
		contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
		contentSelectionIntent.setType("image/*");

		Intent[] intentArray;
		if (takePictureIntent != null) {
			intentArray = new Intent[] { takePictureIntent };
		} else {
			intentArray = new Intent[0];
		}

		Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
		chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
		chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

		mActivity.startActivityForResult(chooserIntent, 1);
	}

	@Override
	/**
	 * handles long presses for the browser, tries to get the
	 * url of the item that was clicked and sends it (it can be null)
	 * to the click handler that does cool stuff with it
	 */
	public void onLongPress() {
		if (mClickHandler == null) {
			mClickHandler = new ClickHandler(mActivity);
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
		setFullscreen(mPreferences.getHideStatusBarEnabled());
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

				Animation hide = AnimationUtils.loadAnimation(mActivity, R.anim.slide_up);
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
				Animation show = AnimationUtils.loadAnimation(mActivity, R.anim.slide_down);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.action_back:
				if (mCurrentView != null) {
					if (mCurrentView.canGoBack()) {
						mCurrentView.goBack();
					} else {
						deleteTab(mDrawerListLeft.getCheckedItemPosition());
					}
				}
				break;
			case R.id.action_forward:
				if (mCurrentView != null) {
					if (mCurrentView.canGoForward()) {
						mCurrentView.goForward();
					}
				}
				break;
			case R.id.arrow_button:
				if (mSearch != null && mSearch.hasFocus()) {
					mCurrentView.requestFocus();
				} else {
					mDrawerLayout.openDrawer(mDrawerLeft);
				}
				break;
			case R.id.new_tab_button:
				newTab(null, true);
				break;
			case R.id.button_next:
				mCurrentView.getWebView().findNext(false);
				break;
			case R.id.button_back:
				mCurrentView.getWebView().findNext(true);
				break;
			case R.id.button_quit:
				mCurrentView.getWebView().clearMatches();
				mSearchBar.setVisibility(View.GONE);
				break;
		}
	}
}
