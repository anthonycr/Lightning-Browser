package acr.browser.lightning.adblock.util.`object`

import java.io.Serializable

/**
 * A store of objects matched to keys.
 */
interface ObjectStore<T> where T : Any, T : Serializable {

    /**
     * Retrieve the object held for [key] or `null` if it is absent.
     */
    fun retrieve(key: String): T?

    /**
     * Stores the [value] matched to the provided [key].
     */
    fun store(key: String, value: T)

}
