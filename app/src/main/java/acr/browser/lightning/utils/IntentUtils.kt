package acr.browser.lightning.utils

import acr.browser.lightning.R
import acr.browser.lightning.constant.INTENT_ORIGIN
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.webkit.WebView
import androidx.core.net.toUri
import java.net.URISyntaxException
import java.util.regex.Matcher
import java.util.regex.Pattern

class IntentUtils(private val activity: Activity) {
    fun startActivityForUrl(tab: WebView?, url: String): Boolean {
        var intent: Intent = try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setComponent(null)
                setSelector(null)
            }
        } catch (ex: URISyntaxException) {
            Log.w("Browser", "Bad URI " + url + ": " + ex.message)
            return false
        }

        if (activity.packageManager.resolveActivity(intent, 0) == null) {
            val packageName = intent.getPackage()
            if (packageName != null) {
                intent = Intent(Intent.ACTION_VIEW, "market://search?q=pname:$packageName".toUri())
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                activity.startActivity(intent)
                return true
            } else {
                return false
            }
        }
        if (tab != null) {
            intent.putExtra(INTENT_ORIGIN, tab.hashCode())
        }

        val m: Matcher = ACCEPTED_URI_SCHEMA.matcher(url)
        if (m.matches() && !isSpecializedHandlerAvailable(intent)) {
            return false
        }
        try {
            if (activity.startActivityIfNeeded(intent, -1)) {
                return true
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return false
    }

    /**
     * Search for intent handlers that are specific to this URL aka, specialized
     * apps like google maps or youtube
     */
    private fun isSpecializedHandlerAvailable(intent: Intent): Boolean {
        val handlers = activity.packageManager.queryIntentActivities(
            intent,
            PackageManager.GET_RESOLVED_FILTER
        )
        if (handlers.isEmpty()) {
            return false
        }
        for (resolveInfo in handlers) {
            val filter = resolveInfo.filter ?: continue
            if (filter.countDataAuthorities() == 0) {
                // Generic handler, skip
                continue
            }
            return true
        }
        return false
    }

    /**
     * Shares a URL to the system.
     * 
     * @param url   the URL to share. If the URL is null
     * or a special URL, no sharing will occur.
     * @param title the title of the URL to share. This
     * is optional.
     */
    fun shareUrl(url: String?, title: String?) {
        if (url != null && !url.isSpecialUrl()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                setType("text/plain")
                if (title != null) {
                    putExtra(Intent.EXTRA_SUBJECT, title)
                }
                putExtra(Intent.EXTRA_TEXT, url)
            }

            activity.startActivity(
                Intent.createChooser(
                    shareIntent,
                    activity.getString(R.string.dialog_title_share)
                )
            )
        }
    }

    companion object {
        private val ACCEPTED_URI_SCHEMA: Pattern = Pattern.compile(
            "(?i)((?:http|https|file)://" + "|(?:inline|data|about|javascript):|(?:.*:.*@))(.*)"
        )
    }
}
