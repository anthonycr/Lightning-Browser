package acr.browser.barebones.customwebview;

import java.lang.reflect.Method;
import acr.browser.barebones.activities.BarebonesActivity;
import acr.browser.barebones.utilities.FinalVariables;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.ZoomButtonsController;

public final class CustomWebView extends WebView {
	private float location;
	private boolean first = false;
	final int API = FinalVariables.API;
	final boolean showFullScreen = BarebonesActivity.showFullScreen;
	final View uBar = BarebonesActivity.uBar;
	final Animation slideUp = BarebonesActivity.slideUp;
	final Animation slideDown = BarebonesActivity.slideDown;
	private ZoomButtonsController zoomControl;
	private boolean zoomShouldDie = false;

	public CustomWebView(Context context) {
		super(context);
		this.setBackgroundResource(0);
		
		//getControls();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		/*if (zoomShouldDie) {
			try {
				zoomControl.getZoomControls().setVisibility(View.INVISIBLE);
			} catch (IllegalArgumentException ignored) {
			}
		}*/
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

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		if(API >= 11){
			this.setActivated(visibility == View.VISIBLE);
		}
		super.onWindowVisibilityChanged(visibility);
	}
/*
	private void getControls() {
		if (API < 11) {
			try {
				Class<?> webview = Class.forName("android.webkit.WebView");
				Method method = webview.getMethod("getZoomButtonsController");
				
				zoomControl = (ZoomButtonsController) method.invoke(this, (Object[])null);
				if (zoomControl != null) {
					zoomShouldDie = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
*/
}
