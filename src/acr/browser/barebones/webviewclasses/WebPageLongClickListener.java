package acr.browser.barebones.webviewclasses;

import acr.browser.barebones.activities.BrowserActivity;
import android.view.View;
import android.view.View.OnLongClickListener;

public class WebPageLongClickListener implements OnLongClickListener{

	@Override
	public boolean onLongClick(View v) {
		return BrowserActivity.onLongClick();
	}

}
