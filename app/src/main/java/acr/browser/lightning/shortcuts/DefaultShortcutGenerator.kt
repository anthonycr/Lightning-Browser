package acr.browser.lightning.shortcuts

import acr.browser.lightning.R
import acr.browser.lightning.resources.ResourceProvider
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

/**
 * Creates a shortcut using the [ShortcutManager] if supported by the launcher. For versions newer
 * than [Build.VERSION_CODES.O].
 */
@RequiresApi(Build.VERSION_CODES.O)
class DefaultShortcutGenerator @Inject constructor(
    private val activity: FragmentActivity,
    private val shortcutManager: ShortcutManager,
    private val resourceProvider: ResourceProvider,
) : ShortcutGenerator {

    override fun createShortcut(
        url: String,
        unsafeTitle: String,
        unsafeFavicon: Bitmap?
    ): Boolean {
        val shortcutIntent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
        }

        val title = unsafeTitle.takeIf { it.isNotEmpty() }
            ?: resourceProvider.stringResource(R.string.untitled)

        if (shortcutManager.isRequestPinShortcutSupported) {
            val pinShortcutInfo = ShortcutInfo.Builder(
                activity,
                "browser-shortcut-${url.hashCode()}"
            ).setIntent(shortcutIntent)
                .apply {
                    if (unsafeFavicon != null) {
                        setIcon(Icon.createWithBitmap(unsafeFavicon))
                    } else {
                        setIcon(Icon.createWithResource(activity, R.drawable.ic_webpage))
                    }
                }
                .setShortLabel(title)
                .build()

            return shortcutManager.requestPinShortcut(pinShortcutInfo, null)
        } else {
            return false
        }
    }
}
