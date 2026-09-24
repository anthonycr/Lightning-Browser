package acr.browser.lightning.icon

import acr.browser.lightning.device.BuildInfo
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import javax.inject.Inject

/**
 * Used to select the app's icon.
 */
class BrowserIconSelector @Inject constructor(
    private val application: Application,
    buildInfo: BuildInfo,
) {

    private val orangeIconComponent = ComponentName(
        buildInfo.packageName,
        "acr.browser.lightning.DefaultBrowserActivityOrange"
    )

    private val blueIconComponent = ComponentName(
        buildInfo.packageName,
        "acr.browser.lightning.DefaultBrowserActivityBlue"
    )

    private val allIcons = listOf(blueIconComponent, orangeIconComponent)

    /**
     * Selects the chosen [browserIcon] as the app's icon.
     */
    fun selectIcon(browserIcon: BrowserIcon) {
        val enabled = when (browserIcon) {
            BrowserIcon.ORANGE -> orangeIconComponent
            BrowserIcon.BLUE -> blueIconComponent
        }

        val disabled = allIcons - enabled

        application.packageManager.setComponentEnabledSetting(
            enabled,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            0
        )

        disabled.forEach { component ->
            application.packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0
            )
        }
    }
}
