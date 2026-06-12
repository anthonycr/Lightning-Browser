package acr.browser.lightning.utils

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * A utility class containing helpful methods pertaining to file storage.
 */
object FileUtils {
    private const val TAG = "FileUtils"

    val DEFAULT_DOWNLOAD_PATH: String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

    /**
     * Writes a stacktrace to the downloads folder with
     * the following filename: [EXCEPTION]_[TIME OF CRASH IN MILLIS].txt
     * 
     * @param throwable the Throwable to log to external storage
     */
    fun writeCrashToStorage(throwable: Throwable) {
        val fileName =
            throwable.javaClass.simpleName + '_' + System.currentTimeMillis() + ".txt"
        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(outputFile)
            throwable.printStackTrace(PrintStream(outputStream))
            outputStream.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to write bundle to storage")
        } finally {
            Utils.close(outputStream)
        }
    }

    /**
     * Converts megabytes to bytes.
     * 
     * @param megaBytes the number of megabytes.
     * @return the converted bytes.
     */
    fun megabytesToBytes(megaBytes: Long): Long {
        return megaBytes * 1024 * 1024
    }

    @JvmStatic
    fun addNecessarySlashes(originalPath: String?): String {
        var originalPath = originalPath
        if (originalPath.isNullOrEmpty()) {
            return "/"
        }
        if (originalPath[originalPath.length - 1] != '/') {
            originalPath = "$originalPath/"
        }
        if (originalPath[0] != '/') {
            originalPath = "/$originalPath"
        }
        return originalPath
    }
}
