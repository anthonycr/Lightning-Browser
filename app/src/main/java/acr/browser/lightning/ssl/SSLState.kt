package acr.browser.lightning.ssl

import android.net.http.SslError

/**
 * Representing the SSL state of the browser.
 */
sealed class SSLState {

    /**
     * No SSL.
     */
    class None : SSLState()

    /**
     * Valid SSL connection.
     */
    class Valid : SSLState()

    /**
     * Broken SSL connection.
     */
    class Invalid(val sslError: SslError) : SSLState()

}