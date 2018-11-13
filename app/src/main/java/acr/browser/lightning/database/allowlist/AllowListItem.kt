package acr.browser.lightning.database.allowlist

/**
 * A model object representing a domain on the allow list.
 */
data class AllowListItem(
    val url: String,
    val timeCreated: Long
)
