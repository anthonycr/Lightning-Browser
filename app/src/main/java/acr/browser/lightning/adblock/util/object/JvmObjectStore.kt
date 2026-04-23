package acr.browser.lightning.adblock.util.`object`

import acr.browser.lightning.adblock.util.hash.HashingAlgorithm
import acr.browser.lightning.extensions.safeUse
import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * An [ObjectStore] that serializes objects using the [ObjectInputStream].
 *
 * @param application Application used to construct files.
 * @param hashingAlgorithm The hashing algorithm used to construct cache file names.
 */
class JvmObjectStore<T>(
    private val application: Application,
    private val hashingAlgorithm: HashingAlgorithm<String>,
    private val key: String,
    private val objectStoreDispatcher: CoroutineDispatcher,
) : ObjectStore<T> where T : Any, T : Serializable {

    /**
     * Create the file in which to store the object, using the cache directory.
     */
    private fun createStorageFile() = File(
        application.cacheDir,
        "object-store-${hashingAlgorithm.hash(key)}"
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun retrieve(): T? = withContext(objectStoreDispatcher) {
        val storageFile = createStorageFile()
        if (storageFile.exists()) {
            val fileInputStream = FileInputStream(storageFile)
            val objectInputStream = ObjectInputStream(fileInputStream)
            return@withContext objectInputStream.safeUse {
                it.readObject() as T
            }
        }

        return@withContext null
    }

    override suspend fun store(value: T): Unit = withContext(objectStoreDispatcher) {
        val storageFile = createStorageFile()
        val fileOutputStream = FileOutputStream(storageFile, false)
        val objectOutputStream = ObjectOutputStream(fileOutputStream)

        objectOutputStream.safeUse {
            it.writeObject(value)
        }
    }

    override suspend fun clear(): Unit = withContext(objectStoreDispatcher) {
        val storageFile = createStorageFile()
        storageFile.delete()
    }
}
