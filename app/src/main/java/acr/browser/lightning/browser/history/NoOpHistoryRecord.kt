package acr.browser.lightning.browser.history

/**
 * A non functional history record that ignores all attempts to record a visit.
 */
object NoOpHistoryRecord : HistoryRecord {
    override fun recordVisit(title: String, url: String) = Unit
}
