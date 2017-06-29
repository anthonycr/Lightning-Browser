package acr.browser.lightning.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;

import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;

public class IntentUtils {

    private final Activity mActivity;

    private static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)"
        + // switch on case insensitive matching
        '('
        + // begin group for schema
        "(?:http|https|file)://" + "|(?:inline|data|about|javascript):" + "|(?:.*:.*@)"
        + ')' + "(.*)");

    public IntentUtils(@NonNull Activity activity) {
        mActivity = activity;
    }

    public boolean startActivityForUrl(@Nullable WebView tab, @NonNull String url) {
        Intent intent;
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException ex) {
            Log.w("Browser", "Bad URI " + url + ": " + ex.getMessage());
            return false;
        }

        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setComponent(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            intent.setSelector(null);
        }

        if (mActivity.getPackageManager().resolveActivity(intent, 0) == null) {
            String packagename = intent.getPackage();
            if (packagename != null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:"
                    + packagename));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                mActivity.startActivity(intent);
                return true;
            } else {
                return false;
            }
        }
        if (tab != null) {
            intent.putExtra(Constants.INTENT_ORIGIN, tab.hashCode());
        }

        Matcher m = ACCEPTED_URI_SCHEMA.matcher(url);
        if (m.matches() && !isSpecializedHandlerAvailable(intent)) {
            return false;
        }
        try {
            if (mActivity.startActivityIfNeeded(intent, -1)) {
                return true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            // TODO: 6/5/17 fix case where this could throw a FileUriExposedException due to file:// urls
        }
        return false;
    }

    /**
     * Search for intent handlers that are specific to this URL aka, specialized
     * apps like google maps or youtube
     */
    private boolean isSpecializedHandlerAvailable(@NonNull Intent intent) {
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
            // Previously we checked the number of data paths, but it is unnecessary
            // filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0
            if (filter.countDataAuthorities() == 0) {
                // Generic handler, skip
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * Shares a URL to the system.
     *
     * @param url   the URL to share. If the URL is null
     *              or a special URL, no sharing will occur.
     * @param title the title of the URL to share. This
     *              is optional.
     */
    public void shareUrl(@Nullable String url, @Nullable String title) {
        if (url != null && !UrlUtils.isSpecialUrl(url)) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            if (title != null) {
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            }
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            mActivity.startActivity(Intent.createChooser(shareIntent, mActivity.getString(R.string.dialog_title_share)));
        }
    }
}
