package acr.browser.lightning.browser.history

import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.browser.di.DatabaseScheduler
import io.reactivex.rxjava3.core.Scheduler
import javax.inject.Inject

/**
 * The default history record that records the history in a permanent data store.
 */
class DefaultHistoryRecord @Inject constructor(
    private val historyRepository: HistoryRepository,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) : HistoryRecord {
    override fun visit(title: String, url: String) {
        historyRepository.visitHistoryEntry(url, title)
            .subscribeOn(databaseScheduler)
            .subscribe()
    }
}
