package acr.browser.lightning.ssl

import acr.browser.lightning.R
import acr.browser.lightning.extensions.inflater
import acr.browser.lightning.extensions.resizeAndShow
import android.content.Context
import android.net.http.SslCertificate
import android.text.format.DateFormat
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_ssl_info.view.*

/**
 * Shows an informative dialog with the provided [SslCertificate] information.
 */
fun Context.showSslDialog(sslCertificate: SslCertificate, sslState: SslState) {
    val by = sslCertificate.issuedBy
    val to = sslCertificate.issuedTo
    val issueDate = sslCertificate.validNotBeforeDate
    val expireDate = sslCertificate.validNotAfterDate

    val dateFormat = DateFormat.getDateFormat(applicationContext)

    val contentView = inflater.inflate(R.layout.dialog_ssl_info, null, false).apply {
        ssl_layout_issue_by.text = by.cName
        ssl_layout_issue_to.text = to.oName
        ssl_layout_issue_date.text = dateFormat.format(issueDate)
        ssl_layout_expire_date.text = dateFormat.format(expireDate)
    }

    val icon = createSslDrawableForState(sslState)

    AlertDialog.Builder(this)
        .setIcon(icon)
        .setTitle(to.cName)
        .setView(contentView)
        .setPositiveButton(R.string.action_ok, null)
        .resizeAndShow()
}
