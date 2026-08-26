package acr.browser.lightning.settings.licenses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Information about the source control for a project.
 */
@Serializable
data class SourceControl(
    @SerialName("url")
    val url: String
)
