package acr.browser.lightning._browser2.history

import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.di.DatabaseScheduler
import io.reactivex.Scheduler
import javax.inject.Inject

/**
 * Created by anthonycr on 9/14/20.
 */
class DefaultHistoryRecord @Inject constructor(
    private val historyRepository: HistoryRepository,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) : HistoryRecord {
    override fun recordVisit(title: String, url: String) {
        historyRepository.visitHistoryEntry(url, title)
            .subscribeOn(databaseScheduler)
            .subscribe()
    }
}
