package acr.browser.lightning.device

/**
 * A representation of the info for the current build.
 *
 * @param packageName The package name of the browser.
 * @param buildType The type of build currently running.
 * @param versionCode The version code of the app currently running.
 * @param isPlus True if this is the plus variant, false if it is the lite variant.
 */
data class BuildInfo(
    val packageName: String,
    val buildType: BuildType,
    val versionCode: Int,
    val isPlus: Boolean,
)

/**
 * The types of builds that this instance of the app could be.
 */
enum class BuildType {
    DEBUG,
    RELEASE
}
