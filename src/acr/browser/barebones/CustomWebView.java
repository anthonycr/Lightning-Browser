package acr.browser.barebones;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;

public final class CustomWebView extends WebView {

    private final int API = FinalVars.API;
	private int hitTest;
	private final boolean showFullScreen = Barebones.showFullScreen;
	private final View uBar = Barebones.uBar;
	private final Animation slideUp = Barebones.slideUp;
	private final Animation slideDown = Barebones.slideDown;

	public CustomWebView(Context context) {
		super(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			hitTest = 0;
			try {
				hitTest = getHitTestResult().getType();
			} catch (NullPointerException ignored) {
			}
			if (API <= 10 && !this.hasFocus()) {
				this.requestFocus();
			}
			if (showFullScreen) {
				if (uBar.isShown()) {
					uBar.startAnimation(slideUp);
					
				} else if (this.getScrollY() <= 5
						&& !uBar.isShown() && hitTest != 9) {
					uBar.startAnimation(slideDown);
					
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
