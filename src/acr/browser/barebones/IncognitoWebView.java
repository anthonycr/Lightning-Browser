package acr.browser.barebones;

import java.lang.reflect.Method;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.ZoomButtonsController;

public final class IncognitoWebView extends WebView {
	private float location;
	private boolean first = false;
    private final int API = FinalVars.API;
	private final boolean showFullScreen = IncognitoMode.showFullScreen;
	private final View uBar = IncognitoMode.uBar;
	private final Animation slideUp = IncognitoMode.slideUp;
	private final Animation slideDown = IncognitoMode.slideDown;
	private ZoomButtonsController zoomControl;
	public IncognitoWebView(Context context) {
		super(context);
		getControls();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(API<11&&zoomControl!=null){
			zoomControl.getZoomControls().setVisibility(View.INVISIBLE);
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			first = true;
			if (API <= 10 && !this.hasFocus()) {
				this.requestFocus();
			}
			location = event.getY();
			break;
		}
		case MotionEvent.ACTION_UP:{
			if (showFullScreen&&first) {
				if (uBar.isShown()&&this.getScrollY()<5) {
					uBar.startAnimation(slideUp);
				} else if (event.getY()>location && !uBar.isShown()) {
					uBar.startAnimation(slideDown);
				} else if (event.getY()<location && uBar.isShown()){
					uBar.startAnimation(slideUp);
				}
				first = false;
			}
			break;
		}
		}

		return super.onTouchEvent(event);
	}
	private void getControls() {
		if(API<11){
        try {
            Class<?> webview = Class.forName("android.webkit.WebView");
            Method method = webview.getMethod("getZoomButtonsController");
            zoomControl = (ZoomButtonsController) method.invoke(this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
		}
    }
}
