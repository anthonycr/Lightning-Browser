package acr.browser.lightning._browser2.tab

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabWebChromeClient : WebChromeClient() {

    val progressObservable: PublishSubject<Int> = PublishSubject.create()
    val titleObservable: PublishSubject<String> = PublishSubject.create()
    val faviconObservable: PublishSubject<Bitmap> = PublishSubject.create()

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressObservable.onNext(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        titleObservable.onNext(title)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
        faviconObservable.onNext(icon)
    }
}
