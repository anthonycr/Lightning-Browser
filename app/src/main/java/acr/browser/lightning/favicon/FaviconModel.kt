package acr.browser.lightning.favicon

import acr.browser.lightning.R
import acr.browser.lightning.extensions.pad
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.ColorInt
import android.support.annotation.WorkerThread
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import io.reactivex.Completable
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactive model that can fetch favicons
 * from URLs and also cache them.
 */
@Singleton
class FaviconModel @Inject constructor(private val application: Application) {

    private val loaderOptions = BitmapFactory.Options()
    private val bookmarkIconSize = application.resources.getDimensionPixelSize(R.dimen.bookmark_item_icon_size)
    private val faviconCache = object : LruCache<String, Bitmap>(FileUtils.megabytesToBytes(1).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /**
     * Retrieves a favicon from the memory cache.Bitmap may not be present if no bitmap has been
     * added for the URL or if it has been evicted from the memory cache.
     *
     * @param url the URL to retrieve the bitmap for.
     * @return the bitmap associated with the URL, may be null.
     */
    private fun getFaviconFromMemCache(url: String): Bitmap? {
        requireNotNull(url)
        synchronized(faviconCache) {
            return faviconCache.get(url)
        }
    }

    fun getDefaultBitmapForString(title: String?): Bitmap {
        val firstTitleCharacter = if (!TextUtils.isEmpty(title)) title!![0] else '?'

        @ColorInt val defaultFaviconColor = DrawableUtils.characterToColorHash(firstTitleCharacter, application)

        return DrawableUtils.getRoundedLetterImage(firstTitleCharacter,
            bookmarkIconSize,
            bookmarkIconSize,
            defaultFaviconColor)
    }

    /**
     * Adds a bitmap to the memory cache for the given URL.
     *
     * @param url    the URL to map the bitmap to.
     * @param bitmap the bitmap to store.
     */
    private fun addFaviconToMemCache(url: String, bitmap: Bitmap) {
        requireNotNull(url)
        requireNotNull(bitmap)
        synchronized(faviconCache) {
            faviconCache.put(url, bitmap)
        }
    }

    /**
     * Retrieves the favicon for a URL, may be from network or cache.
     *
     * @param url   The URL that we should retrieve the favicon for.
     * @param title The title for the web page.
     */
    fun faviconForUrl(url: String, title: String): Single<Bitmap> = Single.create {
        val uri = url.toValidUri()

        if (uri == null) {
            it.onSuccess(getDefaultBitmapForString(title).pad())
            return@create
        }

        val cachedFavicon = getFaviconFromMemCache(url)

        if (cachedFavicon != null) {
            it.onSuccess(cachedFavicon.pad())
            return@create
        }

        val faviconCacheFile = getFaviconCacheFile(application, uri)

        if (faviconCacheFile.exists()) {
            val storedFavicon = BitmapFactory.decodeFile(faviconCacheFile.path, loaderOptions)

            if (storedFavicon != null) {
                addFaviconToMemCache(url, storedFavicon)
                it.onSuccess(storedFavicon.pad())
                return@create
            }
        }

        it.onSuccess(getDefaultBitmapForString(title).pad())
    }

    /**
     * Caches a favicon for a particular URL.
     *
     * @param favicon the favicon to cache.
     * @param url     the URL to cache the favicon for.
     * @return an observable that notifies the consumer when it is complete.
     */
    fun cacheFaviconForUrl(favicon: Bitmap, url: String): Completable = Completable.create { emitter ->
        val uri = url.toValidUri()

        if (uri == null) {
            emitter.onComplete()
            return@create
        }

        Log.d(TAG, "Caching icon for ${uri.host}")
        FileOutputStream(getFaviconCacheFile(application, uri)).safeUse {
            favicon.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
            emitter.onComplete()
        }
    }

    companion object {

        private const val TAG = "FaviconModel"

        /**
         * Creates the cache file for the favicon image. File name will be in the form of "hash of URI host".png
         *
         * @param app the context needed to retrieve the cache directory.
         * @param validUri the URI to use as a unique identifier.
         * @return a valid cache file.
         */
        @WorkerThread
        fun getFaviconCacheFile(app: Application, validUri: ValidUri): File {
            val hash = validUri.host.hashCode().toString()

            return File(app.cacheDir, "$hash.png")
        }
    }

}
