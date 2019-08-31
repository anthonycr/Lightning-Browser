package acr.browser.lightning.device

/**
 * A representation of the info for the current build.
 */
data class BuildInfo(val buildType: BuildType)

/**
 * The types of builds that this instance of the app could be.
 */
enum class BuildType {
    DEBUG,
    RELEASE
}
