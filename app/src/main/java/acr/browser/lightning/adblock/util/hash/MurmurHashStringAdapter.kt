package acr.browser.lightning.adblock.util.hash

/**
 * A [HashingAlgorithm] of type [String] backed by the [MurmurHash].
 */
class MurmurHashStringAdapter : HashingAlgorithm<String> {

    override fun hash(item: String): Int = MurmurHash.hash32(item)

}
