package acr.browser.barebones.customwebview;

import acr.browser.barebones.activities.IncognitoModeActivity;
import acr.browser.barebones.utilities.FinalVariables;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;

public final class IncognitoWebView extends WebView {
	private float location;
	private boolean first = false;
	final int API = FinalVariables.API;
	final boolean showFullScreen = IncognitoModeActivity.showFullScreen;
	final View uBar = IncognitoModeActivity.uBar;
	final Animation slideUp = IncognitoModeActivity.slideUp;
	final Animation slideDown = IncognitoModeActivity.slideDown;
	

	public IncognitoWebView(Context context) {
		super(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			first = true;
			if (API <= 10 && !this.hasFocus()) {
				this.requestFocus();
			}
			location = event.getY();
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (showFullScreen && first) {
				if (uBar.isShown() && this.getScrollY() < 5) {
					uBar.startAnimation(slideUp);
				} else if (event.getY() > location && !uBar.isShown()) {
					uBar.startAnimation(slideDown);
				} else if (event.getY() < location && uBar.isShown()) {
					uBar.startAnimation(slideUp);
				}
				first = false;
			}
			break;
		}
		}

		return super.onTouchEvent(event);
	}

}
