package acr.browser.lightning.browser.image

import acr.browser.lightning.R
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.browser.di.NetworkScheduler
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.favicon.FaviconModel
import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

/**
 * An image loader implementation that caches icons in memory after reading them from the disk
 * cache.
 */
class FaviconImageLoader @Inject constructor(
    private val faviconModel: FaviconModel,
    application: Application,
    @NetworkScheduler private val networkScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler
) : ImageLoader {

    private val lruCache: LruCache<String, Any> = LruCache(1 * 1000 * 1000)
    private val folderIcon = application.drawable(R.drawable.ic_folder)
    private val webPageIcon = application.drawable(R.drawable.ic_webpage)
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
