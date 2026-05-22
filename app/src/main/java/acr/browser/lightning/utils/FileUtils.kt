package acr.browser.lightning.utils

import android.app.Application
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
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
     * Writes a bundle to persistent storage in the files directory
     * using the specified file name. This method is a blocking
     * operation.
     * 
     * @param app    the application needed to obtain the file directory.
     * @param bundle the bundle to store in persistent storage.
     * @param name   the name of the file to store the bundle in.
     */
    suspend fun writeBundleToStorage(
        app: Application,
        bundle: Bundle?,
        name: String,
        coroutineDispatcher: CoroutineDispatcher,
    ) = withContext(coroutineDispatcher) {
        val outputFile = File(app.filesDir, name)
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(outputFile)
            val parcel = Parcel.obtain()
            parcel.writeBundle(bundle)
            outputStream.write(parcel.marshall())
            outputStream.flush()
            parcel.recycle()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to write bundle to storage")
        } finally {
            Utils.close(outputStream)
        }
    }

    /**
     * Use this method to delete the bundle with the specified name.
     * This is a blocking call and should be used within a worker
     * thread unless immediate deletion is necessary.
     * 
     * @param app  the application object needed to get the file.
     * @param name the name of the file.
     */
    fun deleteBundleInStorage(app: Application, name: String) {
        val outputFile = File(app.filesDir, name)
        if (outputFile.exists()) {
            outputFile.delete()
        }
    }

    /**
     * Reads a bundle from the file with the specified
     * name in the peristent storage files directory.
     * This method is a blocking operation.
     * 
     * @param app  the application needed to obtain the files directory.
     * @param name the name of the file to read from.
     * @return a valid Bundle loaded using the system class loader
     * or null if the method was unable to read the Bundle from storage.
     */
    fun readBundleFromStorage(app: Application, name: String): Bundle? {
        val inputFile = File(app.filesDir, name)
        var inputStream: FileInputStream? = null
        try {
            inputStream = FileInputStream(inputFile)
            val parcel = Parcel.obtain()
            val data = ByteArray(inputStream.channel.size().toInt())

            inputStream.read(data, 0, data.size)
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
            val out = parcel.readBundle(ClassLoader.getSystemClassLoader())
            out!!.putAll(out)
            parcel.recycle()
            return out
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Unable to read bundle from storage")
        } catch (e: IOException) {
            Log.e(TAG, "Unable to read bundle from storage", e)
        } finally {
            Utils.close(inputStream)
        }
        return null
    }

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
