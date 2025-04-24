package acr.browser.lightning.favicon

import acr.browser.lightning.migration.Cleanup
import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FaviconCleanup @Inject constructor(
    private val application: Application
) : Cleanup.Action {
    override val versionCode: Int = 101

    override suspend fun execute() {
        withContext(Dispatchers.IO) {
            application.cacheDir.listFiles()
                ?.filter { it.extension == "png" }
                ?.forEach(File::delete)
        }
    }
}
