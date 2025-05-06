package acr.browser.lightning.preview

import acr.browser.lightning.browser.di.Browser2Scope
import acr.browser.lightning.browser.di.CacheDir
import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
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
    @IncognitoMode private val incognitoMode: Boolean,
    @DiskScheduler private val diskScheduler: Scheduler,
    @CacheDir private val cacheDirThreadSafeFileProvider: ThreadSafeFileProvider,
) {

    private val eventPublisher = PublishSubject.create<Event>()

    private val cacheDir = cacheDirThreadSafeFileProvider.file()

    init {
        eventPublisher.subscribeOn(diskScheduler)
            .observeOn(diskScheduler)
            .flatMapCompletable {
                when (it) {
                    is Event.Prune -> pruneInternal(viewIdGenerator.takenIds).ignoreElement()
                    else -> Completable.complete()
                }
            }
            .subscribe()
    }

    /**
     * Retrieves the preview for an ID.
     */
    fun previewForId(id: Int): Single<String> = previewCacheFolder(incognitoMode)
        .map { cacheFolder -> File(cacheFolder, "$id.png").path }

    /**
     * Caches a preview for a specific ID.
     *
     * @return an observable that notifies the consumer when it is complete.
     */
    fun cachePreviewForId(id: Int, preview: Bitmap): Completable =
        previewCacheFolder(incognitoMode).map { cacheFolder ->
            logger.log(TAG, "Caching preview for tab: $id")
            FileOutputStream(getPreviewCacheFile(cacheFolder, id)).safeUse {
                preview.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
            Unit
        }.ignoreElement()

    /**
     * Prune the cache, releasing unused previews.
     */
    fun prune() {
        eventPublisher.onNext(Event.Prune)
    }

    private fun pruneInternal(keepIds: Set<Int>) = previewCacheFolder(incognitoMode)
        .doOnSuccess { cacheFolder ->
            cacheFolder.listFiles()
                ?.filter { !keepIds.contains(it.name.split(".")[0].toInt()) }
                ?.forEach(File::delete)
        }

    /**
     * The folder where favicons are cached.
     */
    private fun previewCacheFolder(incognitoMode: Boolean): Single<File> =
        cacheDir.map { cacheDir ->
            if (incognitoMode) {
                File(cacheDir, "preview-cache-incognito")
            } else {
                File(cacheDir, "preview-cache")
            }
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
