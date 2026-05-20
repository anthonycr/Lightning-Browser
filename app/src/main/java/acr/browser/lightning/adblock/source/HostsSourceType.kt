package acr.browser.lightning.adblock.source

import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File


/**
 * The source from which hosts should be loaded.
 */
sealed class HostsSourceType {

    /**
     * The default source, included in the app assets.
     */
    data object Default : HostsSourceType()

    /**
     * A local source, loaded from a local file.
     *
     * @param file The hosts file to use, must have access to it.
     */
    class Local(val file: File) : HostsSourceType()

    /**
     * A remote source, loaded from a URL.
     *
     * @param httpUrl The URL of the hosts file.
     */
    class Remote(val httpUrl: HttpUrl) : HostsSourceType()
}

/**
 * Extract the user's chosen [HostsSourceType] from the preferences. If either the file chosen is
 * invalid or the remote URL chosen is invalid, we will fall back to the [HostsSourceType.Default].
 */
fun UserPreferencesDataStore.selectedHostsSource(): HostsSourceType {
    val localFile: File? = hostsLocalFile.getUnsafe()?.let(::File)?.takeIf(File::exists)?.takeIf(File::canRead)

    val remoteUrl: HttpUrl? = hostsRemoteFile.getUnsafe()?.toHttpUrlOrNull()

    val source = hostsSource.getUnsafe()

    return if (source == 1 && localFile != null) {
        HostsSourceType.Local(localFile)
    } else if (source == 2 && remoteUrl != null) {
        HostsSourceType.Remote(remoteUrl)
    } else {
        HostsSourceType.Default
    }
}

/**
 * Convert the [HostsSourceType] to the index stored in preferences.
 */
fun HostsSourceType.toPreferenceIndex(): Int = when (this) {
    HostsSourceType.Default -> 0
    is HostsSourceType.Local -> 1
    is HostsSourceType.Remote -> 2
}
