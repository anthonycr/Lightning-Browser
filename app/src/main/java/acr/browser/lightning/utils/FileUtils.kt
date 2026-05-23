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

    /**
     * Determine whether there is write access in the given directory. Returns false if a
     * file cannot be created in the directory or if the directory does not exist.
     * 
     * @param directory the directory to check for write access
     * @return returns true if the directory can be written to or is in a directory that can
     * be written to. false if there is no write access.
     */
    fun isWriteAccessAvailable(directory: String?): Boolean {
        if (directory.isNullOrEmpty()) {
            return false
        }

        val sFileName = "test"
        val sFileExtension = ".txt"
        var dir = addNecessarySlashes(directory)
        dir = getFirstRealParentDirectory(dir)
        var file = File(dir + sFileName + sFileExtension)
        for (n in 0..99) {
            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        file.delete()
                    }
                    return true
                } catch (ignored: IOException) {
                    return false
                }
            } else {
                file = File("$dir$sFileName-$n$sFileExtension")
            }
        }
        return file.canWrite()
    }

    /**
     * Returns the first parent directory of a directory that exists. This is useful
     * for subdirectories that do not exist but their parents do.
     * 
     * @param directory the directory to find the first existent parent
     * @return the first existent parent
     */
    private fun getFirstRealParentDirectory(directory: String?): String {
        var directory = directory
        while (true) {
            if (directory.isNullOrEmpty()) {
                return "/"
            }
            directory = addNecessarySlashes(directory)
            val file = File(directory)
            if (!file.isDirectory) {
                val indexSlash = directory.lastIndexOf('/')
                if (indexSlash > 0) {
                    val parent = directory.substring(0, indexSlash)
                    val previousIndex = parent.lastIndexOf('/')
                    if (previousIndex > 0) {
                        directory = parent.substring(0, previousIndex)
                    } else {
                        return "/"
                    }
                } else {
                    return "/"
                }
            } else {
                return directory
            }
        }
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
