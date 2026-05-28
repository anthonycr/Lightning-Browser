package acr.browser.lightning.adblock.source

import acr.browser.lightning.preference.IntEnum
import acr.browser.lightning.preference.UserPreferencesDataStore
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
suspend fun UserPreferencesDataStore.selectedHostsSource(): HostsSourceType {
    val localFile: File? =
        hostsLocalFile.get()?.let(::File)?.takeIf(File::exists)?.takeIf(File::canRead)

    val remoteUrl: HttpUrl? = hostsRemoteFile.get()?.toHttpUrlOrNull()

    val source = hostsSource.get()

    return when (source) {
        HostsSourcePreference.LOCAL if localFile != null -> {
            HostsSourceType.Local(localFile)
        }

        HostsSourcePreference.REMOTE if remoteUrl != null -> {
            HostsSourceType.Remote(remoteUrl)
        }

        else -> {
            HostsSourceType.Default
        }
    }
}

enum class HostsSourcePreference(override val value: Int) : IntEnum {
    DEFAULT(0),
    LOCAL(1),
    REMOTE(2)
}
