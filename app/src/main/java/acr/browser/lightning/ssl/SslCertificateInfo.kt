package acr.browser.lightning.ssl

import java.util.Date

/**
 * Created by anthonycr on 11/19/20.
 */
data class SslCertificateInfo(
    val issuedByCommonName: String,
    val issuedToCommonName: String,
    val issuedToOrganizationName: String?,
    val issueDate: Date,
    val expireDate: Date,
    val sslState: SslState
)
