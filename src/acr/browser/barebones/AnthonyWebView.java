package acr.browser.barebones;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;

public final class AnthonyWebView extends WebView {

	boolean move;
	int API = FinalVars.API;
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
			try {
				hitTest = getHitTestResult().getType();
			} catch (NullPointerException e) {
			}
			if (API <= 10 && !Barebones.main[Barebones.pageId].hasFocus()) {
				Barebones.main[Barebones.pageId].requestFocus();
			}
			if (showFullScreen) {
				if (uBarShows) {
					uBar.startAnimation(slideUp);
					uBarShows = false;
					
				} else if (Barebones.main[Barebones.pageId].getScrollY() <= 5
						&& !uBarShows && hitTest != 9) {

					uBar.startAnimation(slideDown);
					uBarShows = true;
					
				}
			}
			break;
		}
		default:
			break;
		}

		return super.onTouchEvent(event);
	}

}
