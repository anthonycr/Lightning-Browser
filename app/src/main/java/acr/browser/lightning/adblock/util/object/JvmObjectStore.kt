package acr.browser.lightning.adblock.util.`object`

import acr.browser.lightning.adblock.util.hash.HashingAlgorithm
import acr.browser.lightning.extensions.safeUse
import android.app.Application
import java.io.*

/**
 * An [ObjectStore] that serializes objects using the [ObjectInputStream].
 *
 * @param application Application used to construct files.
 * @param hashingAlgorithm The hashing algorithm used to construct cache file names.
 */
class JvmObjectStore<T>(
    private val application: Application,
    private val hashingAlgorithm: HashingAlgorithm<String>
) : ObjectStore<T> where T : Any, T : Serializable {

    /**
     * Create the file in which to store the object, using the cache directory.
     */
    private fun createStorageFile(key: String) = File(
        application.cacheDir,
        "object-store-${hashingAlgorithm.hash(key)}"
    )

    @Suppress("UNCHECKED_CAST")
    override fun retrieve(key: String): T? {
        val storageFile = createStorageFile(key)
        if (storageFile.exists()) {
            val fileInputStream = FileInputStream(storageFile)
            val objectInputStream = ObjectInputStream(fileInputStream)
            return objectInputStream.safeUse {
                it.readObject() as T
            }
        }

        return null
    }

    override fun store(key: String, value: T) {
        val storageFile = createStorageFile(key)
        val fileOutputStream = FileOutputStream(storageFile, false)
        val objectOutputStream = ObjectOutputStream(fileOutputStream)

        objectOutputStream.safeUse {
            it.writeObject(value)
        }
    }

    override fun clear(key: String) {
        val storageFile = createStorageFile(key)
        storageFile.delete()
    }
}
