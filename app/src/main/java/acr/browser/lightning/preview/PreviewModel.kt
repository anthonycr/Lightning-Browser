package acr.browser.lightning.preview

import acr.browser.lightning.browser.di.Browser2Scope
import acr.browser.lightning.browser.di.PreviewCacheDir
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Reactive model that can store and retrieve previews from a disk cache.
 */
@Browser2Scope
class PreviewModel @Inject constructor(
    private val logger: Logger,
    private val viewIdGenerator: ViewIdGenerator,
    @PreviewCacheDir private val previewCacheDirThreadSafeFileProvider: ThreadSafeFileProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val appCoroutineScope: AppCoroutineScope,
) {

    private val eventSharedFlow = MutableSharedFlow<Event>()

    private val cacheDirDeferred = previewCacheDirThreadSafeFileProvider.file

    init {
        appCoroutineScope.launch(coroutineDispatchers.io) {
            eventSharedFlow.collectLatest {
                when (it) {
                    is Event.Prune -> pruneInternal(viewIdGenerator.takenIds)
                }
            }
        }
    }

    /**
     * Retrieves the preview for an ID.
     */
    suspend fun previewForId(id: Int): String = withContext(coroutineDispatchers.io) {
        val cacheFolder = cacheDirDeferred.await()
        File(cacheFolder, "$id.png").path
    }

    /**
     * Caches a preview for a specific ID.
     *
     * @return an observable that notifies the consumer when it is complete.
     */
    suspend fun cachePreviewForId(
        id: Int,
        preview: Bitmap
    ): Unit = withContext(coroutineDispatchers.io) {
        val cacheFolder = cacheDirDeferred.await()
        logger.log(TAG, "Caching preview for tab: $id")
        FileOutputStream(getPreviewCacheFile(cacheFolder, id)).safeUse {
            preview.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
    }

    /**
     * Prune the cache, releasing unused previews.
     */
    fun prune() {
        appCoroutineScope.launch {
            eventSharedFlow.emit(Event.Prune)
        }
    }

    private suspend fun pruneInternal(
        keepIds: Set<Int>
    ): Unit = withContext(coroutineDispatchers.io) {
        val cacheFolder = cacheDirDeferred.await()
        cacheFolder.listFiles()
            ?.filter { !keepIds.contains(it.name.split(".")[0].toInt()) }
            ?.forEach(File::delete)
    }

    /**
     * Creates the cache file for the preview image. File name will be in the form of "hash of URI host".png
     *
     * @param id The ID of the tab for which a preview will be cached.
     * @return a valid cache file.
     */
    @WorkerThread
    private fun getPreviewCacheFile(previewCache: File, id: Int): File {
        previewCache.mkdirs()
        return File(previewCache, "$id.png")
    }

    companion object {
        private const val TAG = "FaviconModel"
    }

    private sealed class Event {
        data object Prune : Event()
    }

}
