package acr.browser.lightning.browser.image

import acr.browser.lightning.R
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.extensions.themedDrawable
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * An image loader implementation that caches icons in memory after reading them from the disk
 * cache.
 */
class FaviconImageLoader @Inject constructor(
    private val faviconModel: FaviconModel,
    application: Application,
    themeProvider: ThemeProvider,
    coroutineDispatchers: CoroutineDispatchers,
) : ImageLoader {

    private val coroutineScope = CoroutineScope(coroutineDispatchers.main + SupervisorJob())

    private val lruCache: LruCache<String, Any> =
        object : LruCache<String, Any>(FileUtils.megabytesToBytes(5).toInt()) {
            override fun sizeOf(key: String, value: Any) = when (value) {
                is Bitmap -> value.allocationByteCount
                else -> 1
            }
        }
    private val folderIcon = application.themedDrawable(
        R.drawable.ic_folder,
        themeProvider.color(R.attr.autoCompleteTitleColor)
    )
    private val webPageIcon = application.themedDrawable(
        R.drawable.ic_webpage,
        themeProvider.color(R.attr.autoCompleteTitleColor)
    )

    override fun loadImage(imageView: ImageView, bookmark: Bookmark) {
        imageView.tag = bookmark.url
        lruCache[bookmark.url]?.let {
            if (it is Bitmap) {
                imageView.setImageBitmap(it)
            } else if (it is Drawable) {
                imageView.setImageDrawable(it)
            }
        } ?: run {
            when (bookmark) {
                is Bookmark.Folder -> {
                    lruCache.put(bookmark.url, folderIcon)
                    imageView.setImageDrawable(folderIcon)
                }

                is Bookmark.Entry -> {
                    lruCache.put(bookmark.url, webPageIcon)
                    imageView.setImageDrawable(webPageIcon)
                    coroutineScope.launch {
                        val bitmap = faviconModel.getFaviconForUrl(bookmark.url, bookmark.title)
                        lruCache.put(bookmark.url, bitmap)
                        if (imageView.tag == bookmark.url) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }

        fun cleanup() {
            coroutineScope.cancel()
        }
    }
}
