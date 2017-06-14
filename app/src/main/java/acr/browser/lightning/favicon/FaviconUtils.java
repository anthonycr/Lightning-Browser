package acr.browser.lightning.favicon;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Simple utils for favicon fetching.
 */
public class FaviconUtils {
    @Nullable
    public static Uri safeUri(@NonNull String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }

        Uri uri = Uri.parse(url);

        if (TextUtils.isEmpty(uri.getScheme()) || TextUtils.isEmpty(uri.getHost())) {
            return null;
        }

        return uri;
    }

    static void assertUriSafe(@Nullable Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.getScheme()) || TextUtils.isEmpty(uri.getHost())) {
            throw new RuntimeException("Unsafe uri provided");
        }
    }
}
