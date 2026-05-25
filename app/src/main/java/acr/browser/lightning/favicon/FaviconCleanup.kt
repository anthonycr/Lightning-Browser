package acr.browser.lightning.favicon

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.migration.Cleanup
import android.app.Application
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FaviconCleanup @Inject constructor(
    private val application: Application,
    private val coroutineDispatchers: CoroutineDispatchers,
) : Cleanup.Action {
    override val versionCode: Int = 101

    override suspend fun execute(): Unit = withContext(coroutineDispatchers.io) {
        application.cacheDir.listFiles()
            ?.filter { it.extension == "png" }
            ?.forEach(File::delete)
    }
}
