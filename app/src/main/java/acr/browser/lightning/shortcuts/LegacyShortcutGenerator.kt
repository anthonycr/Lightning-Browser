package acr.browser.lightning.shortcuts

import acr.browser.lightning.R
import acr.browser.lightning.resources.ResourceProvider
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

/**
 * Legacy implementation of [ShortcutGenerator] used for versions lower than
 * [android.os.Build.VERSION_CODES.O].
 */
@Suppress("DEPRECATION")
class LegacyShortcutGenerator @Inject constructor(
    private val activity: FragmentActivity,
    private val resourceProvider: ResourceProvider,
) : ShortcutGenerator {

    override fun createShortcut(
        url: String,
        unsafeTitle: String,
        unsafeFavicon: Bitmap?
    ): Boolean {
        val shortcutIntent = Intent(Intent.ACTION_VIEW).apply {
            setData(url.toUri())
        }

        val title = unsafeTitle.ifBlank {
            resourceProvider.stringResource(android.R.string.untitled)
        }
        val webPageDrawable = resourceProvider.drawableResource(R.drawable.ic_webpage)!!
        val webPageBitmap: Bitmap = webPageDrawable.toBitmap()
        val addIntent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
            putExtra(Intent.EXTRA_SHORTCUT_ICON, unsafeFavicon ?: webPageBitmap)
            setAction("com.android.launcher.action.INSTALL_SHORTCUT")
        }
        activity.sendBroadcast(addIntent)
        return true
    }
}
