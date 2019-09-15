package acr.browser.lightning.ssl

import android.net.http.SslError

/**
 * Representing the SSL state of the browser.
 */
sealed class SslState {

    /**
     * No SSL.
     */
    object None : SslState()

    /**
     * Valid SSL connection.
     */
    object Valid : SslState()

    /**
     * Broken SSL connection.
     *
     * @param sslError The error that is causing the invalid SSL state.
     */
    class Invalid(val sslError: SslError) : SslState()

}
