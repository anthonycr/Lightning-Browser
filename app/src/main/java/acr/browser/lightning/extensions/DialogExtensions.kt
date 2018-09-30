@file:Suppress("NOTHING_TO_INLINE")

package acr.browser.lightning.extensions

import acr.browser.lightning.dialog.BrowserDialog
import android.support.v7.app.AlertDialog

/**
 * Ensures that the dialog is appropriately sized and displays it.
 */
inline fun AlertDialog.Builder.resizeAndShow() = BrowserDialog.setDialogSize(context, this.show())
