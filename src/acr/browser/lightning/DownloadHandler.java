/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;

/**
 * Handle download requests
 */
public class DownloadHandler {

	private static final String LOGTAG = "DLHandler";

	private static Activity mActivity;

	/**
	 * Notify the host application a download should be done, or that the data should be streamed if a streaming viewer
	 * is available.
	 *
	 * @param activity           Activity requesting the download.
	 * @param url                The full url to the content that should be downloaded
	 * @param userAgent          User agent of the downloading application.
	 * @param contentDisposition Content-disposition http header, if present.
	 * @param mimetype           The mimetype of the content reported by the server
	 * @param privateBrowsing    If the request is coming from a private browsing tab.
	 */
	public static void onDownloadStart(Activity activity, String url,
			String userAgent, String contentDisposition, String mimetype,
			boolean privateBrowsing) {
		mActivity = activity;
		// if we're dealing wih A/V content that's not explicitly marked
		//     for download, check if it's streamable.
		if (contentDisposition == null
				|| !contentDisposition.regionMatches(
				true, 0, "attachment", 0, 10)) {
			// query the package manager to see if there's a registered handler
			//     that matches.
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(url), mimetype);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ResolveInfo info = activity.getPackageManager().resolveActivity(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
			if (info != null) {
				ComponentName myName = activity.getComponentName();
				// If we resolved to ourselves, we don't want to attempt to
				// load the url only to try and download it again.
				if (!myName.getPackageName().equals(
						info.activityInfo.packageName)
						|| !myName.getClassName().equals(
						info.activityInfo.name)) {
					// someone (other than us) knows how to handle this mime
					// type with this scheme, don't download.
					try {
						activity.startActivity(intent);
						return;
					} catch (ActivityNotFoundException ex) {
						// Best behavior is to fall back to a download in this
						// case
					}
				}
			}
		}
		onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
				mimetype, privateBrowsing);
	}

	// This is to work around the fact that java.net.URI throws Exceptions
	// instead of just encoding URL's properly
	// Helper method for onDownloadStartNoStream
	private static String encodePath(String path) {
		char[] chars = path.toCharArray();

		boolean needed = false;
		for (char c : chars) {
			if (c == '[' || c == ']' || c == '|') {
				needed = true;
				break;
			}
		}
		if (!needed) {
			return path;
		}

		StringBuilder sb = new StringBuilder("");
		for (char c : chars) {
			if (c == '[' || c == ']' || c == '|') {
				sb.append('%');
				sb.append(Integer.toHexString(c));
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	/**
	 * Notify the host application a download should be done, even if there is a streaming viewer available for thise
	 * type.
	 *
	 * @param activity           Activity requesting the download.
	 * @param url                The full url to the content that should be downloaded
	 * @param userAgent          User agent of the downloading application.
	 * @param contentDisposition Content-disposition http header, if present.
	 * @param mimetype           The mimetype of the content reported by the server
	 * @param privateBrowsing    If the request is coming from a private browsing tab.
	 */
	/*package */
	static void onDownloadStartNoStream(Activity activity,
			String url, String userAgent, String contentDisposition,
			String mimetype, boolean privateBrowsing) {

		String filename = URLUtil.guessFileName(url,
				contentDisposition, mimetype);

		// Check to see if we have an SDCard
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			int title;
			String msg;

			// Check to see if the SDCard is busy, same as the music app
			if (status.equals(Environment.MEDIA_SHARED)) {
				msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
				title = R.string.download_sdcard_busy_dlg_title;
			} else {
				msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
				title = R.string.download_no_sdcard_dlg_title;
			}

			new AlertDialog.Builder(activity)
					.setTitle(title)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(msg)
					.setPositiveButton(R.string.action_ok, null)
					.show();
			return;
		}

		// java.net.URI is a lot stricter than KURL so we have to encode some
		// extra characters. Fix for b 2538060 and b 1634719
		WebAddress webAddress;
		try {
			webAddress = new WebAddress(url);
			webAddress.setPath(encodePath(webAddress.getPath()));
		} catch (Exception e) {
			// This only happens for very bad urls, we want to chatch the
			// exception here
			Log.e(LOGTAG, "Exception trying to parse url:" + url);
			return;
		}

		String addressString = webAddress.toString();
		Uri uri = Uri.parse(addressString);
		final DownloadManager.Request request;
		try {
			request = new DownloadManager.Request(uri);
		} catch (IllegalArgumentException e) {
			Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
			return;
		}
		request.setMimeType(mimetype);
		// set downloaded file destination to /sdcard/Download.
		// or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?

		String location = mActivity.getSharedPreferences(PreferenceConstants.PREFERENCES, 0)
				.getString(PreferenceConstants.DOWNLOAD_DIRECTORY, Environment.DIRECTORY_DOWNLOADS);
		request.setDestinationInExternalPublicDir(location, filename);
		// let this downloaded file be scanned by MediaScanner - so that it can
		// show up in Gallery app, for example.
		request.allowScanningByMediaScanner();
		request.setDescription(webAddress.getHost());
		// XXX: Have to use the old url since the cookies were stored using the
		// old percent-encoded url.
		String cookies = CookieManager.getInstance().getCookie(url);
		request.addRequestHeader("cookie", cookies);
		request.setNotificationVisibility(
				DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		if (mimetype == null) {
			if (TextUtils.isEmpty(addressString)) {
				return;
			}
			// We must have long pressed on a link or image to download it. We
			// are not sure of the mimetype in this case, so do a head request
			new FetchUrlMimeType(activity, request, addressString, cookies,
					userAgent).start();
		} else {
			final DownloadManager manager
					= (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
			new Thread("Browser download") {
				@Override
				public void run() {
					manager.enqueue(request);
				}
			}.start();
		}
		Toast.makeText(activity, R.string.download_pending, Toast.LENGTH_SHORT)
				.show();
	}
}
