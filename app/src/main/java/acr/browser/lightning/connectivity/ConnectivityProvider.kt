package acr.browser.lightning.connectivity

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.log.Logger
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the required connectivity status for the browser, notifying consumers when internet
 * access is gained or lost.
 */
@Singleton
class ConnectivityProvider @Inject constructor(
    private val appCoroutineScope: AppCoroutineScope,
    connectivityManager: ConnectivityManager,
    private val logger: Logger,
) {

    /**
     * Emits true when there is internet, false otherwise.
     */
    val hasInternetAccess: MutableStateFlow<Boolean> = MutableStateFlow(
        connectivityManager.hasInternet(connectivityManager.activeNetwork)
    )

    init {
        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    logger.log(TAG, "Lost network")
                    appCoroutineScope.launch {
                        hasInternetAccess.emit(false)
                    }
                }

                override fun onAvailable(network: Network) {
                    val hasInternet = connectivityManager.hasInternet(network)
                    logger.log(TAG, "Gained network, internet availability: $hasInternet")
                    appCoroutineScope.launch {
                        hasInternetAccess.emit(hasInternet)
                    }
                }
            }
        )
    }

    private fun ConnectivityManager.hasInternet(network: Network?): Boolean =
        getNetworkCapabilities(network)?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true

    companion object {
        private const val TAG = "ConnectivityProvider"
    }
}
