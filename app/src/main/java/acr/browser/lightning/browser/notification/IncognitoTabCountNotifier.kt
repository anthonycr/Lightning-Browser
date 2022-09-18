package acr.browser.lightning.browser.notification

import javax.inject.Inject

/**
 * Shows a notification about the number of incognito tabs currently open.
 */
class IncognitoTabCountNotifier @Inject constructor(
    private val incognitoNotification: IncognitoNotification
) : TabCountNotifier {
    override fun notifyTabCountChange(total: Int) {
        if (total > 0) {
            incognitoNotification.show(total)
        } else {
            incognitoNotification.hide()
        }
    }
}
