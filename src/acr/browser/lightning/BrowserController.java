/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebView;

public interface BrowserController {
	public void updateUrl(String title);

	public void updateProgress(int n);

	public void updateHistory(String title, String url);

	public void openFileChooser(ValueCallback<Uri> uploadMsg);

	public void update();
	
	public void onLongPress();
	
	public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback);
	
	public void onHideCustomView();
	
	public Bitmap getDefaultVideoPoster();
	
	public View getVideoLoadingProgressView();
	
	public void onCreateWindow(boolean isUserGesture, Message resultMsg);
	
	public Activity getActivity();
	
	public void hideActionBar();
	
	public void showActionBar();
	
	public void longClickPage(String url);
	
	public void openBookmarkPage(WebView view);
	
	public boolean isActionBarShowing();
	
	public void closeEmptyTab();
}
