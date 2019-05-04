package acr.browser.lightning.adblock.source

import acr.browser.lightning.preference.UserPreferences
import okhttp3.HttpUrl
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest


/**
 * The source from which hosts should be loaded.
 */
sealed class HostsSourceType {

    /**
     * The default source, included in the app assets.
     */
    object Default : HostsSourceType()

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
fun UserPreferences.selectedHostsSource(): HostsSourceType {
    val localFile: File? = hostsLocalFile?.let(::File)?.takeIf(File::exists)?.takeIf(File::canRead)

    val remoteUrl: HttpUrl? = hostsRemoteFile?.let { HttpUrl.parse(it) }

    val source = hostsSource

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

/**
 * The identity of the source. Will be unique for all source types.
 */
fun HostsSourceType.identity(): String = when (this) {
    HostsSourceType.Default -> "assets"
    is HostsSourceType.Local -> getMd5OfFile(file.path)
    is HostsSourceType.Remote -> httpUrl.toString()
}

/**
 * Compute and return the MD5 hash of the file at the provided [filePath].
 */
private fun getMd5OfFile(filePath: String): String {
    var returnVal = ""
    try {
        val input = FileInputStream(filePath)
        val buffer = ByteArray(1024)
        val md5Hash = MessageDigest.getInstance("MD5")
        var numRead = 0
        while (numRead != -1) {
            numRead = input.read(buffer)
            if (numRead > 0) {
                md5Hash.update(buffer, 0, numRead)
            }
        }
        input.close()

        val md5Bytes = md5Hash.digest()
        for (i in md5Bytes.indices) {
            returnVal += Integer.toString((md5Bytes[i].toInt() and 0xff) + 0x100, 16).substring(1)
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }

    return returnVal.toUpperCase()
}
