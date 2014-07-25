package acr.browser.lightning;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentUtils {

	private Activity mActivity;

	private BrowserController mController;

	static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)"
			+ // switch on case insensitive matching
			"("
			+ // begin group for schema
			"(?:http|https|file):\\/\\/" + "|(?:inline|data|about|javascript):"
			+ "|(?:.*:.*@)" + ")" + "(.*)");

	public IntentUtils(BrowserController controller) {
		mController = controller;
		mActivity = mController.getActivity();
	}

	public boolean startActivityForUrl(WebView tab, String url) {
		Intent intent;
		try {
			intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
		} catch (URISyntaxException ex) {
			Log.w("Browser", "Bad URI " + url + ": " + ex.getMessage());
			return false;
		}

		if (mActivity.getPackageManager().resolveActivity(intent, 0) == null) {
			String packagename = intent.getPackage();
			if (packagename != null) {
				intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse("market://search?q=pname:" + packagename));
				intent.addCategory(Intent.CATEGORY_BROWSABLE);
				mActivity.startActivity(intent);
				return true;
			} else {
				return false;
			}
		}
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.setComponent(null);
		if (tab != null) {
			intent.putExtra(mActivity.getPackageName() + ".Origin", 1);
		}

		Matcher m = ACCEPTED_URI_SCHEMA.matcher(url);
		if (m.matches() && !isSpecializedHandlerAvailable(intent)) {
			return false;
		}
		try {
			if (mActivity.startActivityIfNeeded(intent, -1)) {
				return true;
			}
		} catch (ActivityNotFoundException ex) {
		}
		return false;
	}

	/**
	 * Search for intent handlers that are specific to this URL aka, specialized apps like google maps or youtube
	 */
	private boolean isSpecializedHandlerAvailable(Intent intent) {
		PackageManager pm = mActivity.getPackageManager();
		List<ResolveInfo> handlers = pm.queryIntentActivities(intent,
				PackageManager.GET_RESOLVED_FILTER);
		if (handlers == null || handlers.isEmpty()) {
			return false;
		}
		for (ResolveInfo resolveInfo : handlers) {
			IntentFilter filter = resolveInfo.filter;
			if (filter == null) {
				// No intent filter matches this intent?
				// Error on the side of staying in the browser, ignore
				continue;
			}
			// NOTICE: Use of && instead of || will cause the browser
			// to launch a new intent for every URL, using OR only 
			// launches a new one if there is a non-browser app that
			// can handle it.
			if (filter.countDataAuthorities() == 0
					|| filter.countDataPaths() == 0) {
				// Generic handler, skip
				continue;
			}
			return true;
		}
		return false;
	}
}
