package acr.browser.lightning.settings.licenses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A dependency of this project.
 */
@Serializable
data class OpenSourceDependency(
    @SerialName("groupId")
    val groupId: String,
    @SerialName("artifactId")
    val artifactId: String,
    @SerialName("version")
    val version: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("unknownLicenses")
    val unknownLicenses: List<OpenSourceLicense> = emptyList(),
    @SerialName("spdxLicenses")
    val spdxLicenses: List<OpenSourceLicense> = emptyList(),
    @SerialName("scm")
    val sourceControl: SourceControl? = null,
)
