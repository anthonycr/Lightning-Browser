package acr.browser.barebones.customwebview;

import acr.browser.barebones.activities.BrowserActivity;
import acr.browser.barebones.utilities.FinalVariables;
import android.content.Context;
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

public final class CustomWebView extends WebView {
	private float location;
	private boolean first = false;
	final int API = FinalVariables.API;
	final boolean showFullScreen = BrowserActivity.showFullScreen;
	final View uBar = BrowserActivity.uBar;
	final Animation slideUp = BrowserActivity.slideUp;
	final Animation slideDown = BrowserActivity.slideDown;
	static Context CONTEXT;

	public CustomWebView(Context context) {
		super(context);
		mGestureDetector = new GestureDetector(context,
				new CustomGestureListener());
		CONTEXT = context;
		WebSettings settings = this.getSettings();
		browserInitialization(context);
		settingsInitialization(context, settings);
	}

	public void browserInitialization(Context context) {
		this.setDrawingCacheBackgroundColor(0x00000000);
		this.setFocusableInTouchMode(true);
		this.setFocusable(true);
		this.setAnimationCacheEnabled(false);
		this.setDrawingCacheEnabled(true);
		this.setBackgroundColor(context.getResources().getColor(
				android.R.color.white));
		this.setWillNotCacheDrawing(false);
		this.setAlwaysDrawnWithCacheEnabled(true);
		this.setScrollbarFadingEnabled(true);
		this.setSaveEnabled(true);
	}

	@SuppressWarnings("deprecation")
	public void settingsInitialization(Context context, WebSettings settings) {
		settings.setDomStorageEnabled(true);
		settings.setAppCacheEnabled(true);
		settings.setAppCachePath(context.getFilesDir().getAbsolutePath()
				+ "/cache");
		if (API < 18) {
			settings.setLightTouchEnabled(true);
		}
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
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			first = true;
			if (API <= 10 && !hasFocus()) {
				requestFocus();
			}
			location = event.getY();
			break;
		}
		case MotionEvent.ACTION_UP: {

			if (showFullScreen && first) {
				if (uBar.isShown() && getScrollY() < 5) {
					uBar.startAnimation(slideUp);
				} else if ((event.getY() - location) > 20.0 && !uBar.isShown()) {
					uBar.startAnimation(slideDown);
				} else if ((event.getY() - location) < -20.0 && uBar.isShown()) {
					uBar.startAnimation(slideUp);
				}
				first = false;
			}
			break;
		}
		}
		mGestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);

	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		if (API >= 11) {
			this.setActivated(visibility == View.VISIBLE);
		}
		this.setEnabled(visibility == View.VISIBLE);
		super.onWindowVisibilityChanged(visibility);
	}

	private final GestureDetector mGestureDetector;

	private class CustomGestureListener extends SimpleOnGestureListener {
		private final int SWIPE_THRESHOLD = 100;
		private final int SWIPE_VELOCITY_THRESHOLD = 100;

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

				if ((width - e1.getX() < width/12) || (e1.getX() < width/12)) {
					float diffY = e2.getY() - e1.getY();
					float diffX = e2.getX() - e1.getX();
					if (Math.abs(diffX) > Math.abs(diffY)) {
						if (Math.abs(diffX) > SWIPE_THRESHOLD
								&& Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
							if (diffX > 0) {
								BrowserActivity.goBack();
								return false;
							} else {
								BrowserActivity.goForward();
								return false;
							}
						}
					}
				}

			} catch (Exception exception) {
				exception.printStackTrace();
			}
			return false;
		}
	}

}
