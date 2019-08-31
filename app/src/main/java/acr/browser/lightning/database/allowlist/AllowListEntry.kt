package acr.browser.lightning.database.allowlist

/**
 * A model object representing a domain on the allow list.
 *
 * @param domain The domain name for which ads should be allowed.
 * @param timeCreated The time this entry was created in milliseconds.
 */
data class AllowListEntry(
    val domain: String,
    val timeCreated: Long
)
