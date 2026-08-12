package acr.browser.lightning.device

/**
 * A representation of the info for the current build.
 *
 * @param packageName The package name of the app.
 * @param buildType The type of build currently running.
 * @param versionCode The version code of the app currently running.
 */
data class BuildInfo(
    val packageName: String,
    val buildType: BuildType,
    val versionCode: Int,
)

/**
 * The types of builds that this instance of the app could be.
 */
enum class BuildType {
    DEBUG,
    RELEASE
}
