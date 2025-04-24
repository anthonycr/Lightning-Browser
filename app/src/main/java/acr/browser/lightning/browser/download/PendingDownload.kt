package acr.browser.lightning.browser.download

/**
 * Represents a file that will be downloaded.
 *
 * @param url The URL of the file.
 * @param userAgent The user agent we will use when making the download request.
 * @param contentDisposition The description of content we are downloading.
 * @param mimeType The type of content we are downloading.
 * @param contentLength The size of the file we are downloading.
 */
data class PendingDownload(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
    val contentLength: Long
)
