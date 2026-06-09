package acr.browser.lightning.favicon

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.FaviconCacheDir
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.extensions.pad
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
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
    @FaviconCacheDir private val faviconCacheDir: ThreadSafeFileProvider
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
    suspend fun getFaviconForUrl(
        url: String,
        title: String
    ): Bitmap = withContext(coroutineDispatchers.io) {
        val faviconPath = getFaviconPathForUrl(url)
            ?: return@withContext createDefaultBitmapForTitle(title).pad()

        val faviconCacheFile = File(faviconPath)

        if (faviconCacheFile.exists()) {
            val storedFavicon = BitmapFactory.decodeFile(faviconCacheFile.path, loaderOptions)

            if (storedFavicon != null) {
                return@withContext storedFavicon.pad()
            }
        }

        return@withContext createDefaultBitmapForTitle(title).pad()
    }

    /**
     * Creates the cache file path for the favicon image. Path will be in the form of
     * `[hash of URI host].png`
     *
     * @param url The URI to use as a unique identifier.
     * @return The path to the cache file or null if the [url] was invalid.
     */
    suspend fun getFaviconPathForUrl(url: String): String? = withContext(coroutineDispatchers.io) {
        val validUri = url.toUri().toValidUri() ?: return@withContext null

        val hash = validUri.host.hashCode().toString()

        "${faviconCacheDir.file.await().path}/$hash.png"
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
        val faviconPath = getFaviconPathForUrl(url) ?: return@withContext

        logger.log(TAG, "Caching icon: $faviconPath")
        FileOutputStream(File(faviconPath)).safeUse {
            favicon.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
    }

    companion object {
        private const val TAG = "FaviconModel"
    }

}
