package acr.browser.lightning.favicon

import acr.browser.lightning.R
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.extensions.pad
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.DrawableUtils
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactive model that can fetch favicons from URLs and also cache them to disk.
 */
@Singleton
class FaviconModel @Inject constructor(
    private val application: Application,
    private val logger: Logger,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    private val loaderOptions = BitmapFactory.Options()
    private val bookmarkIconSize =
        application.resources.getDimensionPixelSize(R.dimen.material_grid_small_icon)

    /**
     * Create the default favicon for a bookmark with the provided [title].
     */
    fun createDefaultBitmapForTitle(title: String?): Bitmap {
        val firstTitleCharacter = title?.takeIf(String::isNotBlank)?.let { it[0] } ?: '?'

        @ColorInt val defaultFaviconColor =
            DrawableUtils.characterToColorHash(firstTitleCharacter, application)

        return DrawableUtils.createRoundedLetterImage(
            firstTitleCharacter,
            bookmarkIconSize,
            bookmarkIconSize,
            defaultFaviconColor
        )
    }

    /**
     * Retrieves the favicon for a URL, may be from network or cache.
     *
     * @param url   The URL that we should retrieve the favicon for.
     * @param title The title for the web page.
     */
    suspend fun faviconForUrl(
        url: String,
        title: String
    ): Bitmap = withContext(coroutineDispatchers.io) {
        val uri = url.toUri().toValidUri()
            ?: return@withContext createDefaultBitmapForTitle(title).pad()

        val faviconCacheFile = getFaviconCacheFile(application, uri)

        if (faviconCacheFile.exists()) {
            val storedFavicon = BitmapFactory.decodeFile(faviconCacheFile.path, loaderOptions)

            if (storedFavicon != null) {
                return@withContext storedFavicon.pad()
            }
        }

        return@withContext createDefaultBitmapForTitle(title).pad()
    }

    /**
     * Caches a favicon for a particular URL.
     *
     * @param favicon the favicon to cache.
     * @param url     the URL to cache the favicon for.
     * @return an observable that notifies the consumer when it is complete.
     */
    suspend fun cacheFaviconForUrl(
        favicon: Bitmap,
        url: String
    ): Unit = withContext(coroutineDispatchers.io) {
        val uri = url.toUri().toValidUri() ?: return@withContext

        logger.log(TAG, "Caching icon for ${uri.host}")
        FileOutputStream(getFaviconCacheFile(application, uri)).safeUse {
            favicon.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
    }

    companion object {

        /**
         * The folder where favicons are cached.
         */
        fun faviconCacheFolder(application: Application): File =
            File(application.cacheDir, "favicon-cache")

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

            val faviconCache = faviconCacheFolder(app)
            faviconCache.mkdirs()
            return File(faviconCache, "$hash.png")
        }
    }

}
