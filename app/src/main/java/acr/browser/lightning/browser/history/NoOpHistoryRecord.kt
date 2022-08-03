package acr.browser.lightning.browser.history

/**
 * Created by anthonycr on 9/14/20.
 */
object NoOpHistoryRecord : HistoryRecord {
    override fun recordVisit(title: String, url: String) = Unit
}
