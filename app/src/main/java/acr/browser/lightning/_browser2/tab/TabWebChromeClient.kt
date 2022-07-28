package acr.browser.lightning._browser2.tab

import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.utils.Option
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabWebChromeClient(
    private val faviconModel: FaviconModel,
    private val diskScheduler: Scheduler
) : WebChromeClient() {

    val progressObservable: PublishSubject<Int> = PublishSubject.create()
    val titleObservable: PublishSubject<String> = PublishSubject.create()
    val faviconObservable: BehaviorSubject<Option<Bitmap>> = BehaviorSubject.create()

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressObservable.onNext(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        titleObservable.onNext(title)
        faviconObservable.onNext(Option.None)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
        faviconObservable.onNext(Option.Some(icon))
        val url = view.url ?: return
        faviconModel.cacheFaviconForUrl(icon, url)
            .subscribeOn(diskScheduler)
            .subscribe()
    }
}
