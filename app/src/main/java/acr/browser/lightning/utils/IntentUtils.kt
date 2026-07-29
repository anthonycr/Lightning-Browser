package acr.browser.lightning.utils

import acr.browser.lightning.R
import acr.browser.lightning.constant.INTENT_ORIGIN
import acr.browser.lightning.log.Logger
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import java.net.URISyntaxException
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

class IntentUtils @Inject constructor(
    private val activity: Activity,
    private val logger: Logger,
) {

    fun startActivityForUrl(tab: WebView?, url: String): Boolean {
        if (url.isSpecialUrl()) {
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivityForUrlInternalApi30(tab, url)
        } else {
            startActivityForUrlInternalApiLegacy(tab, url)
        }
    }

    private fun startActivityForUrlInternalApiLegacy(tab: WebView?, url: String): Boolean {
        var intent: Intent = try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = null
                selector = null
            }
        } catch (ex: URISyntaxException) {
            logger.log(TAG, "Bad URI $url: ${ex.message}")
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startActivityForUrlInternalApi30(tab: WebView?, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER or
                Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT
            if (tab != null) {
                putExtra(INTENT_ORIGIN, tab.hashCode())
            }
        }

        return try {
            val started = activity.startActivityIfNeeded(intent, -1)
            if (started) {
                logger.log(TAG, "Started activity for URL: $url")
            }
            started
        } catch (_: ActivityNotFoundException) {
            false
        }
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
                type = "text/plain"
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
        private const val TAG = "IntentUtils"
        private val ACCEPTED_URI_SCHEMA: Pattern = Pattern.compile(
            "(?i)((?:http|https|file)://|(?:inline|data|about|javascript):|(?:.*:.*@))(.*)"
        )
    }
}
