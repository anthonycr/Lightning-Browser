package acr.browser.lightning.browser.history

import acr.browser.lightning.database.history.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The default history record that records the history in a permanent data store.
 */
class DefaultHistoryRecord @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val appCoroutineScope: CoroutineScope,
) : HistoryRecord {
    override fun visit(title: String, url: String) {
        appCoroutineScope.launch {
            historyRepository.visitHistoryEntry(url, title)
        }
    }
}
