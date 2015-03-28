/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

public class LightningView {

	private Title mTitle;
	private WebView mWebView;
	private BrowserController mBrowserController;
	private GestureDetector mGestureDetector;
	private Activity mActivity;
	private WebSettings mSettings;
	private static int API = android.os.Build.VERSION.SDK_INT;
	private static String mHomepage;
	private static String mDefaultUserAgent;
	private static Bitmap mWebpageBitmap;
	private static SharedPreferences mPreferences;
	private AdBlock mAdBlock;
	private boolean isForegroundTab;
	private IntentUtils mIntentUtils;
	private Paint mPaint = new Paint();
	private boolean mTextReflow = false;
	private boolean mInvertPage = false;
	private static final float[] mNegativeColorArray = { -1.0f, 0, 0, 0, 255, // red
			0, -1.0f, 0, 0, 255, // green
			0, 0, -1.0f, 0, 255, // blue
			0, 0, 0, 1.0f, 0 // alpha
	};

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public LightningView(Activity activity, String url) {

		mActivity = activity;
		mWebView = new WebView(activity);
		mTitle = new Title(activity);
		mAdBlock = new AdBlock(activity);

		mWebpageBitmap = BitmapFactory.decodeResource(activity.getResources(),
				R.drawable.ic_webpage);

		try {
			mBrowserController = (BrowserController) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity + " must implement BrowserController");
		}
		mIntentUtils = new IntentUtils(mBrowserController);
		mWebView.setDrawingCacheBackgroundColor(0x00000000);
		mWebView.setFocusableInTouchMode(true);
		mWebView.setFocusable(true);
		mWebView.setAnimationCacheEnabled(false);
		mWebView.setDrawingCacheEnabled(false);
		mWebView.setWillNotCacheDrawing(true);
		mWebView.setAlwaysDrawnWithCacheEnabled(false);
		mWebView.setBackgroundColor(activity.getResources().getColor(android.R.color.white));

		if (API > 15) {
			mWebView.setBackground(null);
			mWebView.getRootView().setBackground(null);
		} else if (mWebView.getRootView() != null) {
			mWebView.getRootView().setBackgroundDrawable(null);
		}
		mWebView.setScrollbarFadingEnabled(true);
		mWebView.setSaveEnabled(true);
		mWebView.setWebChromeClient(new LightningChromeClient(activity));
		mWebView.setWebViewClient(new LightningWebClient(activity));
		mWebView.setDownloadListener(new LightningDownloadListener(activity));
		mGestureDetector = new GestureDetector(activity, new CustomGestureListener());
		mWebView.setOnTouchListener(new TouchListener());
		mDefaultUserAgent = mWebView.getSettings().getUserAgentString();
		mSettings = mWebView.getSettings();
		initializeSettings(mWebView.getSettings(), activity);
		initializePreferences(activity);

		if (url != null) {
			if (!url.trim().isEmpty()) {
				mWebView.loadUrl(url);
			} else {
				// don't load anything, the user is looking for a blank tab
			}
		} else {
			if (mHomepage.startsWith("about:home")) {
				mWebView.loadUrl(getHomepage());
			} else if (mHomepage.startsWith("about:bookmarks")) {
				mBrowserController.openBookmarkPage(mWebView);
			} else {
				mWebView.loadUrl(mHomepage);
			}
		}
	}

	public String getHomepage() {
		String home;
		home = HomepageVariables.HEAD;
		switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
			case 0:
				// CUSTOM SEARCH
				home = home + "file:///android_asset/lightning.png";
				home = home + HomepageVariables.MIDDLE;
				home = home
						+ mPreferences.getString(PreferenceConstants.SEARCH_URL,
								Constants.GOOGLE_SEARCH);
				break;
			case 1:
				// GOOGLE_SEARCH;
				home = home + "file:///android_asset/google.png";
				// + "https://www.google.com/images/srpr/logo11w.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.GOOGLE_SEARCH;
				break;
			case 2:
				// ANDROID SEARCH;
				home = home + "file:///android_asset/ask.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.ASK_SEARCH;
				break;
			case 3:
				// BING_SEARCH;
				home = home + "file:///android_asset/bing.png";
				// +
				// "http://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/Bing_logo_%282013%29.svg/500px-Bing_logo_%282013%29.svg.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.BING_SEARCH;
				break;
			case 4:
				// YAHOO_SEARCH;
				home = home + "file:///android_asset/yahoo.png";
				// +
				// "http://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Yahoo%21_logo.svg/799px-Yahoo%21_logo.svg.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.YAHOO_SEARCH;
				break;
			case 5:
				// STARTPAGE_SEARCH;
				home = home + "file:///android_asset/startpage.png";
				// + "https://startpage.com/graphics/startp_logo.gif";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.STARTPAGE_SEARCH;
				break;
			case 6:
				// STARTPAGE_MOBILE
				home = home + "file:///android_asset/startpage.png";
				// + "https://startpage.com/graphics/startp_logo.gif";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.STARTPAGE_MOBILE_SEARCH;
			case 7:
				// DUCK_SEARCH;
				home = home + "file:///android_asset/duckduckgo.png";
				// +
				// "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.DUCK_SEARCH;
				break;
			case 8:
				// DUCK_LITE_SEARCH;
				home = home + "file:///android_asset/duckduckgo.png";
				// +
				// "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.DUCK_LITE_SEARCH;
				break;
			case 9:
				// BAIDU_SEARCH;
				home = home + "file:///android_asset/baidu.png";
				// + "http://www.baidu.com/img/bdlogo.gif";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.BAIDU_SEARCH;
				break;
			case 10:
				// YANDEX_SEARCH;
				home = home + "file:///android_asset/yandex.png";
				// +
				// "http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png";
				home = home + HomepageVariables.MIDDLE;
				home = home + Constants.YANDEX_SEARCH;
				break;

		}

		home = home + HomepageVariables.END;

		File homepage = new File(mActivity.getFilesDir(), "homepage.html");
		try {
			FileWriter hWriter = new FileWriter(homepage, false);
			hWriter.write(home);
			hWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Constants.FILE + homepage;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	public synchronized void initializePreferences(Context context) {
		mPreferences = context.getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
		mAdBlock.updatePreference();
		if (mSettings == null && mWebView != null) {
			mSettings = mWebView.getSettings();
		} else if (mSettings == null) {
			return;
		}

		setColorMode(mPreferences.getInt(PreferenceConstants.RENDERING_MODE, 0));

		if (!mBrowserController.isIncognito()) {
			mSettings.setGeolocationEnabled(mPreferences.getBoolean(PreferenceConstants.LOCATION,
					false));
		} else {
			mSettings.setGeolocationEnabled(false);
		}
		if (API < 19) {
			switch (mPreferences.getInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0)) {
				case 0:
					mSettings.setPluginState(PluginState.OFF);
					break;
				case 1:
					mSettings.setPluginState(PluginState.ON_DEMAND);
					break;
				case 2:
					mSettings.setPluginState(PluginState.ON);
					break;
				default:
					break;
			}
		}

		switch (mPreferences.getInt(PreferenceConstants.USER_AGENT, 1)) {
			case 1:
				if (API > 16) {
					mSettings.setUserAgentString(WebSettings.getDefaultUserAgent(context));
				} else {
					mSettings.setUserAgentString(mDefaultUserAgent);
				}
				break;
			case 2:
				mSettings.setUserAgentString(Constants.DESKTOP_USER_AGENT);
				break;
			case 3:
				mSettings.setUserAgentString(Constants.MOBILE_USER_AGENT);
				break;
			case 4:
				mSettings.setUserAgentString(mPreferences.getString(
						PreferenceConstants.USER_AGENT_STRING, mDefaultUserAgent));
				break;
		}

		if (mPreferences.getBoolean(PreferenceConstants.SAVE_PASSWORDS, false)
				&& !mBrowserController.isIncognito()) {
			if (API < 18) {
				mSettings.setSavePassword(true);
			}
			mSettings.setSaveFormData(true);
		} else {
			if (API < 18) {
				mSettings.setSavePassword(false);
			}
			mSettings.setSaveFormData(false);
		}

		if (mPreferences.getBoolean(PreferenceConstants.JAVASCRIPT, true)) {
			mSettings.setJavaScriptEnabled(true);
			mSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		}

		if (mPreferences.getBoolean(PreferenceConstants.TEXT_REFLOW, false)) {
			mTextReflow = true;
			mSettings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
			if (API >= android.os.Build.VERSION_CODES.KITKAT) {
				try {
					mSettings.setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);
				} catch (Exception e) {
					// This shouldn't be necessary, but there are a number
					// of KitKat devices that crash trying to set this
					Log.e(Constants.TAG, "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING");
				}
			}
		} else {
			mTextReflow = false;
			mSettings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		}

		mSettings.setBlockNetworkImage(mPreferences.getBoolean(PreferenceConstants.BLOCK_IMAGES,
				false));
		mSettings.setSupportMultipleWindows(mPreferences.getBoolean(PreferenceConstants.POPUPS,
				true));
		mSettings.setUseWideViewPort(mPreferences.getBoolean(PreferenceConstants.USE_WIDE_VIEWPORT,
				true));
		mSettings.setLoadWithOverviewMode(mPreferences.getBoolean(
				PreferenceConstants.OVERVIEW_MODE, true));
		switch (mPreferences.getInt(PreferenceConstants.TEXT_SIZE, 3)) {
			case 1:
				mSettings.setTextZoom(200);
				break;
			case 2:
				mSettings.setTextZoom(150);
				break;
			case 3:
				mSettings.setTextZoom(100);
				break;
			case 4:
				mSettings.setTextZoom(75);
				break;
			case 5:
				mSettings.setTextZoom(50);
				break;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView,
					!mPreferences.getBoolean(PreferenceConstants.BLOCK_THIRD_PARTY, false));
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	public void initializeSettings(WebSettings settings, Context context) {
		if (API < 18) {
			settings.setAppCacheMaxSize(Long.MAX_VALUE);
		}
		if (API < 17) {
			settings.setEnableSmoothTransition(true);
		}
		if (API > 16) {
			settings.setMediaPlaybackRequiresUserGesture(true);
		}
		if (API >= Build.VERSION_CODES.LOLLIPOP && !mBrowserController.isIncognito()) {
			settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		} else if (API >= Build.VERSION_CODES.LOLLIPOP) {
			// We're in Incognito mode, reject
			settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
		}
		settings.setDomStorageEnabled(true);
		settings.setAppCacheEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_DEFAULT);
		settings.setDatabaseEnabled(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setDisplayZoomControls(false);
		settings.setAllowContentAccess(true);
		settings.setAllowFileAccess(true);
		settings.setDefaultTextEncodingName("utf-8");
		if (API > 16) {
			settings.setAllowFileAccessFromFileURLs(false);
			settings.setAllowUniversalAccessFromFileURLs(false);
		}

		settings.setAppCachePath(context.getDir("appcache", 0).getPath());
		settings.setGeolocationDatabasePath(context.getDir("geolocation", 0).getPath());
		if (API < Build.VERSION_CODES.KITKAT) {
			settings.setDatabasePath(context.getDir("databases", 0).getPath());
		}
	}

	public boolean isShown() {
		return mWebView != null && mWebView.isShown();
	}

	public synchronized void onPause() {
		if (mWebView != null) {
			mWebView.onPause();
		}
	}

	public synchronized void onResume() {
		if (mWebView != null) {
			mWebView.onResume();
		}
	}

	public void setForegroundTab(boolean isForeground) {
		isForegroundTab = isForeground;
		mBrowserController.update();
	}

	public boolean isForegroundTab() {
		return isForegroundTab;
	}

	public int getProgress() {
		if (mWebView != null) {
			return mWebView.getProgress();
		} else {
			return 100;
		}
	}

	public synchronized void stopLoading() {
		if (mWebView != null) {
			mWebView.stopLoading();
		}
	}

	public void setHardwareRendering() {
		mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, mPaint);
	}

	public void setNormalRendering() {
		mWebView.setLayerType(View.LAYER_TYPE_NONE, null);
	}

	public void setSoftwareRendering() {
		mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	public void setColorMode(int mode) {
		mInvertPage = false;
		switch (mode) {
			case 0:
				mPaint.setColorFilter(null);
				// setSoftwareRendering(); // Some devices get segfaults
				// in the WebView with Hardware Acceleration enabled,
				// the only fix is to disable hardware rendering
				setNormalRendering();
				mInvertPage = false;
				break;
			case 1:
				ColorMatrixColorFilter filterInvert = new ColorMatrixColorFilter(
						mNegativeColorArray);
				mPaint.setColorFilter(filterInvert);
				setHardwareRendering();

				mInvertPage = true;
				break;
			case 2:
				ColorMatrix cm = new ColorMatrix();
				cm.setSaturation(0);
				ColorMatrixColorFilter filterGray = new ColorMatrixColorFilter(cm);
				mPaint.setColorFilter(filterGray);
				setHardwareRendering();
				break;
			case 3:
				ColorMatrix matrix = new ColorMatrix();
				matrix.set(mNegativeColorArray);
				ColorMatrix matrixGray = new ColorMatrix();
				matrixGray.setSaturation(0);
				ColorMatrix concat = new ColorMatrix();
				concat.setConcat(matrix, matrixGray);
				ColorMatrixColorFilter filterInvertGray = new ColorMatrixColorFilter(concat);
				mPaint.setColorFilter(filterInvertGray);
				setHardwareRendering();

				mInvertPage = true;
				break;

		}

	}

	public synchronized void pauseTimers() {
		if (mWebView != null) {
			mWebView.pauseTimers();
		}
	}

	public synchronized void resumeTimers() {
		if (mWebView != null) {
			mWebView.resumeTimers();
		}
	}

	public void requestFocus() {
		if (mWebView != null && !mWebView.hasFocus()) {
			mWebView.requestFocus();
		}
	}

	public void setVisibility(int visible) {
		if (mWebView != null) {
			mWebView.setVisibility(visible);
		}
	}

	public void clearCache(boolean disk) {
		if (mWebView != null) {
			mWebView.clearCache(disk);
		}
	}

	public synchronized void reload() {
		if (mWebView != null) {
			mWebView.reload();
		}
	}

	private void cacheFavicon(Bitmap icon) {
		String hash = String.valueOf(Utils.getDomainName(getUrl()).hashCode());
		Log.d(Constants.TAG, "Caching icon for " + Utils.getDomainName(getUrl()));
		File image = new File(mActivity.getCacheDir(), hash + ".png");
		try {
			FileOutputStream fos = new FileOutputStream(image);
			icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public synchronized void find(String text) {
		if (mWebView != null) {
			if (API > 16) {
				mWebView.findAllAsync(text);
			} else {
				mWebView.findAll(text);
			}
		}
	}

	public Activity getActivity() {
		return mActivity;
	}

	public synchronized void onDestroy() {
		if (mWebView != null) {
			mWebView.stopLoading();
			mWebView.onPause();
			mWebView.clearHistory();
			mWebView.setVisibility(View.GONE);
			mWebView.removeAllViews();
			mWebView.destroyDrawingCache();
			// mWebView.destroy(); //this is causing the segfault
			mWebView = null;
		}
	}

	public synchronized void goBack() {
		if (mWebView != null) {
			mWebView.goBack();
		}
	}

	public String getUserAgent() {
		if (mWebView != null) {
			return mWebView.getSettings().getUserAgentString();
		} else {
			return "";
		}
	}

	public synchronized void goForward() {
		if (mWebView != null) {
			mWebView.goForward();
		}
	}

	public boolean canGoBack() {
		return mWebView != null && mWebView.canGoBack();
	}

	public boolean canGoForward() {
		return mWebView != null && mWebView.canGoForward();
	}

	public WebView getWebView() {
		return mWebView;
	}

	public Bitmap getFavicon() {
		return mTitle.getFavicon();
	}

	public synchronized void loadUrl(String url) {
		if (mWebView != null) {
			mWebView.loadUrl(url);
		}
	}

	public synchronized void invalidate() {
		if (mWebView != null) {
			mWebView.invalidate();
		}
	}

	public String getTitle() {
		return mTitle.getTitle();
	}

	public String getUrl() {
		if (mWebView != null) {
			return mWebView.getUrl();
		} else {
			return "";
		}
	}

	public class LightningWebClient extends WebViewClient {

		Context mActivity;

		LightningWebClient(Context context) {
			mActivity = context;
		}

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
			if (mAdBlock.isAd(request.getUrl().getHost())) {
				ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
				return new WebResourceResponse("text/plain", "utf-8", EMPTY);
			}

			return super.shouldInterceptRequest(view, request);
		}

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
			if (mAdBlock.isAd(url)) {
				ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
				return new WebResourceResponse("text/plain", "utf-8", EMPTY);
			}
			return null;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (view.isShown()) {
				mBrowserController.updateUrl(url, true);
				view.postInvalidate();
			}
			if (view.getTitle() == null || view.getTitle().isEmpty()) {
				mTitle.setTitle(mActivity.getString(R.string.untitled));
			} else {
				mTitle.setTitle(view.getTitle());
			}
			if (API >= android.os.Build.VERSION_CODES.KITKAT && mInvertPage) {
				view.evaluateJavascript(Constants.JAVASCRIPT_INVERT_PAGE, null);
			}
			mBrowserController.update();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (isShown()) {
				mBrowserController.updateUrl(url, false);
				mBrowserController.showActionBar();
			}
			mTitle.setFavicon(mWebpageBitmap);
			mBrowserController.update();
		}

		@Override
		public void onReceivedHttpAuthRequest(final WebView view, final HttpAuthHandler handler,
				final String host, final String realm) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			final EditText name = new EditText(mActivity);
			final EditText password = new EditText(mActivity);
			LinearLayout passLayout = new LinearLayout(mActivity);
			passLayout.setOrientation(LinearLayout.VERTICAL);

			passLayout.addView(name);
			passLayout.addView(password);

			name.setHint(mActivity.getString(R.string.hint_username));
			password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			password.setTransformationMethod(new PasswordTransformationMethod());
			password.setHint(mActivity.getString(R.string.hint_password));
			builder.setTitle(mActivity.getString(R.string.title_sign_in));
			builder.setView(passLayout);
			builder.setCancelable(true)
					.setPositiveButton(mActivity.getString(R.string.title_sign_in),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									String user = name.getText().toString();
									String pass = password.getText().toString();
									handler.proceed(user.trim(), pass.trim());
									Log.d(Constants.TAG, "Request Login");

								}
							})
					.setNegativeButton(mActivity.getString(R.string.action_cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									handler.cancel();

								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		}

		boolean isRunning = false;

		@Override
		public void onScaleChanged(final WebView view, final float oldScale, final float newScale) {
			if (view.isShown() && mTextReflow && API >= android.os.Build.VERSION_CODES.KITKAT) {
				view.invalidate();
				if (isRunning)
					return;
				isRunning = view.postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO
						view.evaluateJavascript(Constants.JAVASCRIPT_TEXT_REFLOW, null);
						isRunning = false;
					}

				}, 100);

			}
		}

		@Override
		public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mActivity.getString(R.string.title_warning));
			builder.setMessage(mActivity.getString(R.string.message_untrusted_certificate))
					.setCancelable(true)
					.setPositiveButton(mActivity.getString(R.string.action_yes),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									handler.proceed();
								}
							})
					.setNegativeButton(mActivity.getString(R.string.action_no),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									handler.cancel();
								}
							});
			AlertDialog alert = builder.create();
			if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
				alert.show();
			} else {
				handler.proceed();
			}

		}

		@Override
		public void onFormResubmission(WebView view, final Message dontResend, final Message resend) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mActivity.getString(R.string.title_form_resubmission));
			builder.setMessage(mActivity.getString(R.string.message_form_resubmission))
					.setCancelable(true)
					.setPositiveButton(mActivity.getString(R.string.action_yes),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {

									resend.sendToTarget();
								}
							})
					.setNegativeButton(mActivity.getString(R.string.action_no),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {

									dontResend.sendToTarget();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (mBrowserController.isIncognito()) {
				return super.shouldOverrideUrlLoading(view, url);
			}
			if (url.startsWith("about:")) {
				return super.shouldOverrideUrlLoading(view, url);
			}
			if (url.contains("mailto:")) {
				MailTo mailTo = MailTo.parse(url);
				Intent i = Utils.newEmailIntent(mActivity, mailTo.getTo(), mailTo.getSubject(),
						mailTo.getBody(), mailTo.getCc());
				mActivity.startActivity(i);
				view.reload();
				return true;
			} else if (url.startsWith("intent://")) {
				Intent intent = null;
				try {
					intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
				} catch (URISyntaxException ex) {
					return false;
				}
				if (intent != null) {
					try {
						mActivity.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Log.e(Constants.TAG, "ActivityNotFoundException");
					}
					return true;
				}
			}
			return mIntentUtils.startActivityForUrl(mWebView, url);
		}
	}

	public class LightningChromeClient extends WebChromeClient {

		Context mActivity;

		LightningChromeClient(Context context) {
			mActivity = context;
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if (isShown()) {
				mBrowserController.updateProgress(newProgress);
			}
		}

		@Override
		public void onReceivedIcon(WebView view, Bitmap icon) {
			mTitle.setFavicon(icon);
			mBrowserController.update();
			cacheFavicon(icon);
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			if (!title.isEmpty()) {
				mTitle.setTitle(title);
			} else {
				mTitle.setTitle(mActivity.getString(R.string.untitled));
			}
			mBrowserController.update();
			mBrowserController.updateHistory(title, view.getUrl());
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(final String origin,
				final GeolocationPermissions.Callback callback) {
			final boolean remember = true;
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mActivity.getString(R.string.location));
			String org = null;
			if (origin.length() > 50) {
				org = origin.subSequence(0, 50) + "...";
			} else {
				org = origin;
			}
			builder.setMessage(org + mActivity.getString(R.string.message_location))
					.setCancelable(true)
					.setPositiveButton(mActivity.getString(R.string.action_allow),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									callback.invoke(origin, true, remember);
								}
							})
					.setNegativeButton(mActivity.getString(R.string.action_dont_allow),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									callback.invoke(origin, false, remember);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		}

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
				Message resultMsg) {
			mBrowserController.onCreateWindow(isUserGesture, resultMsg);
			return true;
		}

		@Override
		public void onCloseWindow(WebView window) {
			// TODO Auto-generated method stub
			super.onCloseWindow(window);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg) {
			mBrowserController.openFileChooser(uploadMsg);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
			mBrowserController.openFileChooser(uploadMsg);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
			mBrowserController.openFileChooser(uploadMsg);
		}

		@Override
		public Bitmap getDefaultVideoPoster() {
			return mBrowserController.getDefaultVideoPoster();
		}

		@Override
		public View getVideoLoadingProgressView() {
			return mBrowserController.getVideoLoadingProgressView();
		}

		@Override
		public void onHideCustomView() {
			mBrowserController.onHideCustomView();
			super.onHideCustomView();
		}

		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			// While these lines might look like they work, in practice,
			// Full-screen videos won't work correctly. I may test this out some
			// more
			// if (view instanceof FrameLayout) {
			// FrameLayout frame = (FrameLayout) view;
			// if (frame.getFocusedChild() instanceof VideoView) {
			// VideoView video = (VideoView) frame.getFocusedChild();
			// video.stopPlayback();
			// frame.removeView(video);
			// video.setVisibility(View.GONE);
			// }
			// } else {
			Activity activity = mBrowserController.getActivity();
			mBrowserController.onShowCustomView(view, activity.getRequestedOrientation(), callback);

			// }

			super.onShowCustomView(view, callback);
		}

		@Override
		@Deprecated
		public void onShowCustomView(View view, int requestedOrientation,
				CustomViewCallback callback) {
			// While these lines might look like they work, in practice,
			// Full-screen videos won't work correctly. I may test this out some
			// more
			// if (view instanceof FrameLayout) {
			// FrameLayout frame = (FrameLayout) view;
			// if (frame.getFocusedChild() instanceof VideoView) {
			// VideoView video = (VideoView) frame.getFocusedChild();
			// video.stopPlayback();
			// frame.removeView(video);
			// video.setVisibility(View.GONE);
			// }
			// } else {
			mBrowserController.onShowCustomView(view, requestedOrientation, callback);

			// }

			super.onShowCustomView(view, requestedOrientation, callback);
		}
	}

	public class Title {

		private Bitmap mFavicon;

		private String mTitle;

		private Bitmap mDefaultIcon;

		public Title(Context context) {
			mDefaultIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ic_webpage);
			mFavicon = mDefaultIcon;
			mTitle = mActivity.getString(R.string.action_new_tab);
		}

		public void setFavicon(Bitmap favicon) {
			if (favicon == null) {
				mFavicon = mDefaultIcon;
			} else {
				mFavicon = Utils.padFavicon(favicon);
			}
		}

		public void setTitle(String title) {
			if (title == null) {
				mTitle = "";
			} else {
				mTitle = title;
			}
		}

		public void setTitleAndFavicon(String title, Bitmap favicon) {
			mTitle = title;

			if (favicon == null) {
				mFavicon = mDefaultIcon;
			} else {
				mFavicon = Utils.padFavicon(favicon);
			}
		}

		public String getTitle() {
			return mTitle;
		}

		public Bitmap getFavicon() {
			return mFavicon;
		}

	}

	private class TouchListener implements OnTouchListener {
		float mLocation;

		float mY;

		int mAction;

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View view, MotionEvent arg1) {
			if (view != null && !view.hasFocus()) {
				view.requestFocus();
			}
			mAction = arg1.getAction();
			mY = arg1.getY();
			if (mAction == MotionEvent.ACTION_DOWN) {
				mLocation = mY;
			} else if (mAction == MotionEvent.ACTION_UP) {
				if ((mY - mLocation) > 10) {
					if (mWebView.getScrollY() != 0) {
						mBrowserController.showActionBar();
					} else {
						mBrowserController.toggleActionBar();
					}
				} else if ((mY - mLocation) < -10) {
					mBrowserController.hideActionBar();
				}
				mLocation = 0;
			}
			mGestureDetector.onTouchEvent(arg1);
			return false;
		}
	}

	private class CustomGestureListener extends SimpleOnGestureListener {

		/**
		 * Without this, onLongPress is not called when user is zooming using
		 * two fingers, but is when using only one.
		 * 
		 * The required behaviour is to not trigger this when the user is
		 * zooming, it shouldn't matter how much fingers the user's using.
		 */
		private boolean mCanTriggerLongPress = true;

		@Override
		public void onLongPress(MotionEvent e) {
			if (mCanTriggerLongPress)
				mBrowserController.onLongPress();
		}

		/**
		 * Is called when the user is swiping after the doubletap, which in our
		 * case means that he is zooming.
		 */
		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			mCanTriggerLongPress = false;
			return false;
		}

		/**
		 * Is called when something is starting being pressed, always before
		 * onLongPress.
		 */
		@Override
		public void onShowPress(MotionEvent e) {
			mCanTriggerLongPress = true;
		}
	}
}
