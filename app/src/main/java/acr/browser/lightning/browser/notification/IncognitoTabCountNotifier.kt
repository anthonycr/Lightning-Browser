package acr.browser.lightning.browser.notification

import javax.inject.Inject

/**
 * Created by anthonycr on 7/27/22.
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
