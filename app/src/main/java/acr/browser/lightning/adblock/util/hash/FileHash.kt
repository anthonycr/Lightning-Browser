package acr.browser.lightning.adblock.util.hash

import java.io.InputStream
import java.security.MessageDigest

/**
 * Compute and return the MD5 hash of the [InputStream].
 */
fun InputStream.computeMD5(): String {
    var returnVal = ""
    try {
        val buffer = ByteArray(1024)
        val md5Hash = MessageDigest.getInstance("MD5")
        var numRead = 0
        while (numRead != -1) {
            numRead = this.read(buffer)
            if (numRead > 0) {
                md5Hash.update(buffer, 0, numRead)
            }
        }
        this.close()

        val md5Bytes = md5Hash.digest()
        for (i in md5Bytes.indices) {
            returnVal += Integer.toString((md5Bytes[i].toInt() and 0xff) + 0x100, 16).substring(1)
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }

    return returnVal.toUpperCase()
}
