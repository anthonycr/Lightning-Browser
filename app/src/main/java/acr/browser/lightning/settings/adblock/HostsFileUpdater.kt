package acr.browser.lightning.settings.adblock

import acr.browser.lightning.concurrency.CoroutineDispatchers
import android.app.Application
import android.net.Uri
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject

interface HostsFileUpdater {

    suspend fun readTextFromUri(uri: Uri): File?

}

class DefaultHostsFileUpdater @Inject constructor(
    private val application: Application,
    private val coroutineDispatchers: CoroutineDispatchers,
) : HostsFileUpdater {
    override suspend fun readTextFromUri(uri: Uri): File? = withContext(coroutineDispatchers.io) {
        val externalFilesDir = application.getExternalFilesDir("")
            ?: return@withContext null
        val inputStream = application.contentResolver?.openInputStream(uri)
            ?: return@withContext null

        try {
            val outputFile = File(externalFilesDir, AD_HOSTS_FILE)

            val input = inputStream.source()
            val output = outputFile.sink().buffer()
            output.writeAll(input)
            return@withContext outputFile
        } catch (exception: IOException) {
            return@withContext null
        }
    }

    companion object {
        private const val AD_HOSTS_FILE = "local_hosts.txt"
    }
}
