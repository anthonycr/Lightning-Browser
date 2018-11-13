package acr.browser.lightning.database.allowlist

/**
 * A model object representing a domain on the allow list.
 */
data class AllowListEntry(
    val domain: String,
    val timeCreated: Long
)
