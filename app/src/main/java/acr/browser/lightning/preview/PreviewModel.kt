package acr.browser.lightning.preview

import acr.browser.lightning.browser.di.Browser2Scope
import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.ids.ViewIdGenerator
import acr.browser.lightning.log.Logger
import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Reactive model that can store and retrieve previews from a disk cache.
 */
@Browser2Scope
class PreviewModel @Inject constructor(
    private val application: Application,
    private val logger: Logger,
    private val viewIdGenerator: ViewIdGenerator,
    @IncognitoMode private val incognitoMode: Boolean,
    @DiskScheduler private val diskScheduler: Scheduler
) {

    private val eventPublisher = PublishSubject.create<Event>()

    init {
        eventPublisher.subscribeOn(diskScheduler)
            .observeOn(diskScheduler)
            .subscribe {
                when (it) {
                    is Event.Prune -> pruneInternal(viewIdGenerator.takenIds)
                }
            }
    }

    /**
     * Retrieves the preview for an ID.
     */
    fun previewForId(id: Int): String =
        File(previewCacheFolder(application, incognitoMode), "$id.png").path

    /**
     * Caches a preview for a specific ID.
     *
     * @return an observable that notifies the consumer when it is complete.
     */
    fun cachePreviewForId(id: Int, preview: Bitmap) {
        logger.log(TAG, "Caching preview for tab: $id")
        FileOutputStream(getPreviewCacheFile(application, incognitoMode, id)).safeUse {
            preview.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
    }

    /**
     * Prune the cache, releasing unused previews.
     */
    fun prune() {
        eventPublisher.onNext(Event.Prune)
    }

    private fun pruneInternal(keepIds: Set<Int>) {
        previewCacheFolder(application, incognitoMode).listFiles()
            ?.filter { !keepIds.contains(it.name.split(".")[0].toInt()) }
            ?.forEach(File::delete)
    }

    companion object {

        /**
         * The folder where favicons are cached.
         */
        fun previewCacheFolder(application: Application, incognitoMode: Boolean): File =
            if (incognitoMode) {
                File(application.cacheDir, "preview-cache-incognito")
            } else {
                File(application.cacheDir, "preview-cache")
            }


        private const val TAG = "FaviconModel"

        /**
         * Creates the cache file for the preview image. File name will be in the form of "hash of URI host".png
         *
         * @param app the context needed to retrieve the cache directory.
         * @param id The ID of the tab for which a preview will be cached.
         * @return a valid cache file.
         */
        @WorkerThread
        fun getPreviewCacheFile(app: Application, incognitoMode: Boolean, id: Int): File {
            val previewCache = previewCacheFolder(app, incognitoMode)
            previewCache.mkdirs()
            return File(previewCache, "$id.png")
        }
    }

    private sealed class Event {
        data object Prune : Event()
    }

}
