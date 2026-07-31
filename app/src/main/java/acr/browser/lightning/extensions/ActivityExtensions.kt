package acr.browser.lightning.extensions

import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

/**
 * Displays a snackbar to the user with a [StringRes] message.
 *
 * NOTE: If there is an accessibility manager enabled on
 * the device, such as LastPass, then the snackbar animations
 * will not work.
 *
 * @param resource the string resource to display to the user.
 */
fun Activity.snackbar(@StringRes resource: Int) {
    val view = findViewById<View>(android.R.id.content)
    Snackbar.make(view, resource, Snackbar.LENGTH_SHORT).show()
}
