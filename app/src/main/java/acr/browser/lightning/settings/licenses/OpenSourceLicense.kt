package acr.browser.lightning.settings.licenses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An open source license as identified by licensee.
 */
@Serializable
data class OpenSourceLicense(
    @SerialName("identifier")
    val identifier: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: String,
)
