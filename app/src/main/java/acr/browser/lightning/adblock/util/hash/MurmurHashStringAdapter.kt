package acr.browser.lightning.adblock.util.hash

import java.io.Serializable

/**
 * A [HashingAlgorithm] of type [String] backed by the [MurmurHash].
 */
class MurmurHashStringAdapter : HashingAlgorithm<String>, Serializable {

    override fun hash(item: String): Int = MurmurHash.hash32(item)

}
