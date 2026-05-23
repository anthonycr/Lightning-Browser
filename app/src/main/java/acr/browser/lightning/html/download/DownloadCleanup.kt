package acr.browser.lightning.html.download

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.migration.Cleanup
import android.app.Application
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DownloadCleanup @Inject constructor(
    private val application: Application,
    private val coroutineDispatchers: CoroutineDispatchers,
) : Cleanup.Action {
    override val versionCode: Int = 101

    override suspend fun execute() {
        withContext(coroutineDispatchers.io) {
            application.filesDir.listFiles()
                ?.filter { it.endsWith(DownloadPageFactory.FILENAME) }
                ?.forEach(File::delete)
        }
    }

}
