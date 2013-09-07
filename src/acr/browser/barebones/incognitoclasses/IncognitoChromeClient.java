package acr.browser.barebones.incognitoclasses;

import acr.browser.barebones.R;
import acr.browser.barebones.activities.IncognitoModeActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebStorage.QuotaUpdater;
import android.widget.FrameLayout;

@SuppressLint("SetJavaScriptEnabled")
public class IncognitoChromeClient extends WebChromeClient {
	private static Context context;
	private static Activity browserActivity;
	private static View mCustomView;
	private static CustomViewCallback mCustomViewCallback;
	public IncognitoChromeClient(IncognitoModeActivity activity){
		context = activity;
		browserActivity = activity;
	}
	public Bitmap mDefaultVideoPoster;
	public View mVideoProgressView;
	public FrameLayout fullScreenContainer;
	public int orientation;

	@Override
	public void onExceededDatabaseQuota(String url,
			String databaseIdentifier, long quota,
			long estimatedDatabaseSize, long totalQuota,
			QuotaUpdater quotaUpdater) {
		quotaUpdater.updateQuota(totalQuota + estimatedDatabaseSize);

	}

	@Override
	public void onProgressChanged(WebView view, int newProgress) {
		IncognitoModeActivity.onProgressChanged(view.getId(), newProgress);
		super.onProgressChanged(view, newProgress);
	}

	@Override
	public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
			QuotaUpdater quotaUpdater) {
		quotaUpdater.updateQuota(quota + requiredStorage);
	}

	@Override
	public Bitmap getDefaultVideoPoster() {
		if (mDefaultVideoPoster == null) {
			mDefaultVideoPoster = BitmapFactory.decodeResource(
					context.getResources(), android.R.color.black);
		}
		return mDefaultVideoPoster;
	}

	@Override
	public View getVideoLoadingProgressView() {
		if (mVideoProgressView == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			mVideoProgressView = inflater.inflate(
					android.R.layout.simple_spinner_item, null);
		}
		return mVideoProgressView;
	}

	@Override
	public void onCloseWindow(WebView window) {
		Message msg = Message.obtain();
		msg.what = 3;
		msg.arg1 = window.getId();
		IncognitoModeActivity.browserHandler.sendMessage(msg);
		super.onCloseWindow(window);
	}

	@Override
	public boolean onCreateWindow(WebView view, boolean isDialog,
			boolean isUserGesture, final Message resultMsg) {

		if (isUserGesture) {
			IncognitoModeActivity.onCreateWindow(resultMsg);
		}
		return true;
	}
	
	
	@Override
	public void onGeolocationPermissionsShowPrompt(final String origin,
			final GeolocationPermissions.Callback callback) {
			final boolean remember = true;
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Location Access");
			String org = null;
			if (origin.length() > 50) {
				org = (String) origin.subSequence(0, 50) + "...";
			} else {
				org = origin;
			}
			builder.setMessage(org + "\nWould like to use your Location ")
					.setCancelable(true)
					.setPositiveButton("Allow",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									callback.invoke(origin, true, remember);
								}
							})
					.setNegativeButton("Don't Allow",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									callback.invoke(origin, false, remember);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		
	}

	@Override
	public void onHideCustomView() {
		if (mCustomView == null && mCustomViewCallback == null) {
			return;
		}
		
		mCustomView.setKeepScreenOn(false);
		mCustomView = null;
		IncognitoModeActivity.onHideCustomView(fullScreenContainer, mCustomViewCallback, orientation);
	}

	@Override
	public void onReceivedIcon(WebView view, Bitmap favicon) {
		IncognitoModeActivity.setFavicon(view.getId(), favicon);
	}

	@Override
	public void onReceivedTitle(final WebView view, final String title) {
		IncognitoModeActivity.onReceivedTitle(view.getId(), title);
		super.onReceivedTitle(view, title);
	}

	@Override
	public void onShowCustomView(View view, int requestedOrientation,
			CustomViewCallback callback) {
		if (mCustomView != null) {
			callback.onCustomViewHidden();
			return;
		}
		view.setKeepScreenOn(true);
		orientation = browserActivity.getRequestedOrientation();
		FrameLayout screen = (FrameLayout) browserActivity.getWindow().getDecorView();
		fullScreenContainer = new FrameLayout(context);
		fullScreenContainer.setBackgroundColor(context.getResources().getColor(
				R.color.black));
		IncognitoModeActivity.onShowCustomView();
		fullScreenContainer.addView(view,
				ViewGroup.LayoutParams.MATCH_PARENT);
		screen.addView(fullScreenContainer,
				ViewGroup.LayoutParams.MATCH_PARENT);
		mCustomView = view;
		mCustomViewCallback = callback;
		browserActivity.setRequestedOrientation(requestedOrientation);

	}

	@Override
	public void onShowCustomView(View view,
			WebChromeClient.CustomViewCallback callback) {
		if (mCustomView != null) {
			callback.onCustomViewHidden();
			return;
		}
		view.setKeepScreenOn(true);
		orientation = browserActivity.getRequestedOrientation();
		FrameLayout screen = (FrameLayout) browserActivity.getWindow().getDecorView();
		fullScreenContainer = new FrameLayout(context);
		fullScreenContainer.setBackgroundColor(context.getResources().getColor(
				R.color.black));
		IncognitoModeActivity.onShowCustomView();
		fullScreenContainer.addView(view,
				ViewGroup.LayoutParams.MATCH_PARENT);
		screen.addView(fullScreenContainer,
				ViewGroup.LayoutParams.MATCH_PARENT);
		mCustomView = view;
		mCustomViewCallback = callback;
		browserActivity.setRequestedOrientation(browserActivity.getRequestedOrientation());
	}

	public void openFileChooser(ValueCallback<Uri> uploadMsg) {
		IncognitoModeActivity.openFileChooser(uploadMsg);
	}

	public void openFileChooser(ValueCallback<Uri> uploadMsg,
			String acceptType) {
		IncognitoModeActivity.openFileChooser(uploadMsg);
	}

	public void openFileChooser(ValueCallback<Uri> uploadMsg,
			String acceptType, String capture) {
		IncognitoModeActivity.openFileChooser(uploadMsg);
	}

}
