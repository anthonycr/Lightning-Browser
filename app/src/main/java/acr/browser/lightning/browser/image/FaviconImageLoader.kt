package acr.browser.lightning.browser.image

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.browser.di.NetworkScheduler
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.extensions.themedDrawable
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

/**
 * An image loader implementation that caches icons in memory after reading them from the disk
 * cache.
 */
class FaviconImageLoader @Inject constructor(
    private val faviconModel: FaviconModel,
    application: Application,
    @NetworkScheduler private val networkScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler,
    themeProvider: ThemeProvider
) : ImageLoader {

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
    private val compositeDisposable = CompositeDisposable()

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
                    compositeDisposable += faviconModel
                        .faviconForUrl(bookmark.url, bookmark.title)
                        .subscribeOn(networkScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onSuccess = { bitmap ->
                                lruCache.put(bookmark.url, bitmap)
                                if (imageView.tag == bookmark.url) {
                                    imageView.setImageBitmap(bitmap)
                                }
                            }
                        )
                }
            }
        }

        fun cleanup() {
            compositeDisposable.clear()
        }
    }
}
