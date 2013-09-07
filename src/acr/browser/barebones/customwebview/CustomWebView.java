package acr.browser.barebones.customwebview;

import acr.browser.barebones.activities.BrowserActivity;
import acr.browser.barebones.utilities.FinalVariables;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;

public final class CustomWebView extends WebView {
	private boolean first = false;
	static final int API = FinalVariables.API;
	public static boolean showFullScreen;;
	final View uBar = BrowserActivity.uBar;
	final Animation slideUp = BrowserActivity.slideUp;
	final Animation slideDown = BrowserActivity.slideDown;
	static Context CONTEXT;
	static String defaultUser;
	public WebSettings settings;

	public CustomWebView(Context context) {

		super(context);
		defaultUser = BrowserActivity.defaultUser;
		showFullScreen = BrowserActivity.showFullScreen;
		mGestureDetector = new GestureDetector(context,
				new CustomGestureListener());
		CONTEXT = context;
		settings = getSettings();
		browserInitialization(context);
		settingsInitialization(context);
	}

	@SuppressWarnings("deprecation")
	public void browserInitialization(Context context) {
		setDrawingCacheBackgroundColor(0x00000000);
		setFocusableInTouchMode(true);
		setFocusable(true);
		setAnimationCacheEnabled(false);
		setDrawingCacheEnabled(true);
		setBackgroundColor(context.getResources().getColor(
				android.R.color.white));
		if (API >= 16) {
			getRootView().setBackground(null);
		} else {
			getRootView().setBackgroundDrawable(null);
		}
		setWillNotCacheDrawing(false);
		setAlwaysDrawnWithCacheEnabled(true);
		setScrollbarFadingEnabled(true);
		setSaveEnabled(true);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@SuppressWarnings("deprecation")
	public void settingsInitialization(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(
				"settings", 0);
		settings.setDomStorageEnabled(true);
		settings.setAppCacheEnabled(true);
		settings.setAppCachePath(context.getFilesDir().getAbsolutePath()
				+ "/cache");
		settings.setAllowFileAccess(true);
		settings.setDatabaseEnabled(true);
		settings.setDatabasePath(context.getFilesDir().getAbsolutePath()
				+ "/databases");
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		if (API >= 11) {
			settings.setDisplayZoomControls(false);
			settings.setAllowContentAccess(true);
		}

		if (preferences.getBoolean("java", true)) {
			settings.setJavaScriptEnabled(true);
			settings.setJavaScriptCanOpenWindowsAutomatically(true);
		}

		if (API < 14) {
			switch (preferences.getInt("textsize", 3)) {
			case 1:
				settings.setTextSize(WebSettings.TextSize.LARGEST);
				break;
			case 2:
				settings.setTextSize(WebSettings.TextSize.LARGER);
				break;
			case 3:
				settings.setTextSize(WebSettings.TextSize.NORMAL);
				break;
			case 4:
				settings.setTextSize(WebSettings.TextSize.SMALLER);
				break;
			case 5:
				settings.setTextSize(WebSettings.TextSize.SMALLEST);
				break;
			}

		} else {
			switch (preferences.getInt("textsize", 3)) {
			case 1:
				settings.setTextZoom(200);
				break;
			case 2:
				settings.setTextZoom(150);
				break;
			case 3:
				settings.setTextZoom(100);
				break;
			case 4:
				settings.setTextZoom(75);
				break;
			case 5:
				settings.setTextZoom(50);
				break;
			}
		}
		settings.setSupportMultipleWindows(preferences.getBoolean("newwindow",
				true));

		switch (preferences.getInt("enableflash", 0)) {
		case 0:
			break;
		case 1: {
			settings.setPluginState(PluginState.ON_DEMAND);
			break;
		}
		case 2: {
			settings.setPluginState(PluginState.ON);
			break;
		}
		default:
			break;
		}
		if (preferences.getBoolean("passwords", false)) {
			if (API < 18) {
				settings.setSavePassword(true);
			}
			settings.setSaveFormData(true);
		}
		if (API < 18) {
			try {
				settings.setRenderPriority(RenderPriority.HIGH);
			} catch (SecurityException ignored) {

			}
		}
		settings.setGeolocationEnabled(preferences
				.getBoolean("location", false));
		settings.setGeolocationDatabasePath(context.getFilesDir()
				.getAbsolutePath());
		settings.setUseWideViewPort(preferences
				.getBoolean("wideviewport", true));
		settings.setLoadWithOverviewMode(preferences.getBoolean("overviewmode",
				true));

		if (preferences.getBoolean("textreflow", false)) {
			settings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
		} else {
			settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		}

		settings.setBlockNetworkImage(preferences.getBoolean("blockimages",
				false));
		settings.setLoadsImagesAutomatically(true);
		
		switch (preferences.getInt("agentchoose", 1)) {
		case 1:
			getSettings().setUserAgentString(defaultUser);
			break;
		case 2:
			getSettings().setUserAgentString(
					FinalVariables.DESKTOP_USER_AGENT);
			break;
		case 3:
			getSettings().setUserAgentString(
					FinalVariables.MOBILE_USER_AGENT);
			break;
		case 4:
			getSettings().setUserAgentString(
					preferences.getString("userAgentString", defaultUser));
			break;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		if (API >= 11) {
			setActivated(visibility == View.VISIBLE);
		}
		setEnabled(visibility == View.VISIBLE);
		super.onWindowVisibilityChanged(visibility);
	}

	private final GestureDetector mGestureDetector;

	private class CustomGestureListener extends SimpleOnGestureListener {
		final int SWIPE_THRESHOLD = 100;
		final int SWIPE_VELOCITY_THRESHOLD = 100;

		@Override
		public boolean onDown(MotionEvent e) {
			first = true;
			return super.onDown(e);
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (BrowserActivity.currentId != -1) {
				BrowserActivity.onLongClick();
			}
			super.onLongPress(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if (showFullScreen && first) {
				if (uBar.isShown() && getScrollY() < 5) {
					uBar.startAnimation(slideUp);
				} else if (distanceY < -5 && !uBar.isShown()) {
					uBar.startAnimation(slideDown);
				} else if (distanceY > 5 && uBar.isShown()) {
					uBar.startAnimation(slideUp);
				}
				first = false;
			}
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {

				int width;
				if (API < 13) {
					DisplayMetrics metrics = CONTEXT.getResources()
							.getDisplayMetrics();
					width = metrics.widthPixels;
				} else {
					WindowManager wm = (WindowManager) CONTEXT
							.getSystemService(Context.WINDOW_SERVICE);
					Display display = wm.getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					width = size.x;
				}

				if ((width - e1.getX() < width / 12)
						|| (e1.getX() < width / 12)) {
					float diffY = e2.getY() - e1.getY();
					float diffX = e2.getX() - e1.getX();
					if (Math.abs(diffX) > Math.abs(diffY)) {
						if (Math.abs(diffX) > SWIPE_THRESHOLD
								&& Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
							if (diffX > 0) {
								BrowserActivity.goBack(CustomWebView.this);
							} else {
								BrowserActivity.goForward(CustomWebView.this);
							}
						}
					}
				}

			} catch (Exception exception) {
				exception.printStackTrace();
			}
			return super.onFling(e1, e2, velocityX, velocityY);
		}
	}

}
