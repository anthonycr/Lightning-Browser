package acr.browser.lightning.adblock.util.`object`

import java.io.Serializable

/**
 * A store of an object.
 */
interface ObjectStore<T> where T : Any, T : Serializable {

    /**
     * Retrieve the value held `null` if it is absent.
     */
    suspend fun retrieve(): T?

    /**
     * Stores the [value].
     */
    suspend fun store(value: T)

    /**
     * Clears the value held.
     */
    suspend fun clear()

}
