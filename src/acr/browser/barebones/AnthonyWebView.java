package acr.browser.barebones;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;

public final class AnthonyWebView extends WebView {

	boolean move;
	int API = Barebones.API;
	long timeBetweenDownPress = 0;
	int hitTest;
	boolean showFullScreen = Barebones.showFullScreen;
	View uBar = Barebones.uBar;
	boolean uBarShows = Barebones.uBarShows;
	Animation slideUp = Barebones.slideUp;
	Animation slideDown = Barebones.slideDown;
	
	public AnthonyWebView(Context context) {
		super(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			move = false;
			if (API <= 10 && !Barebones.main[Barebones.pageId].hasFocus()) {
				Barebones.main[Barebones.pageId].requestFocus();
			}
			timeBetweenDownPress = System.currentTimeMillis();
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			move = true;
		}
		case MotionEvent.ACTION_UP: {
			
			try {
				hitTest = getHitTestResult().getType();
			} catch (NullPointerException e) {
			}
			if (showFullScreen && hitTest != 9) {
				if (System.currentTimeMillis() - timeBetweenDownPress < 500
						&& !move) {
					if (!uBarShows) {
						uBar.startAnimation(slideDown);
						uBarShows = true;
					} else if (uBarShows) {
						uBar.startAnimation(slideUp);
						uBarShows = false;
					}
					break;

				} else if (Barebones.main[Barebones.pageId].getScrollY() > 5 && uBarShows) {
					uBar.startAnimation(slideUp);
					uBarShows = false;
					break;
				} else if (Barebones.main[Barebones.pageId].getScrollY() < 5 && !uBarShows) {

					uBar.startAnimation(slideDown);
					uBarShows = true;
					break;
				}
			}
		}
		default:
			break;
		}

		return super.onTouchEvent(event);
	}

}
