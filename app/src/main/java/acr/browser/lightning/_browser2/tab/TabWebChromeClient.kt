package acr.browser.lightning._browser2.tab

import acr.browser.lightning.R
import acr.browser.lightning.extensions.color
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.utils.Option
import acr.browser.lightning.utils.Utils
import acr.browser.lightning.view.ResultMessageInitializer
import acr.browser.lightning.view.TabInitializer
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabWebChromeClient(
    context: Context,
    private val faviconModel: FaviconModel,
    private val diskScheduler: Scheduler
) : WebChromeClient() {

    private val defaultColor = context.color(R.color.primary_color)

    val progressObservable: PublishSubject<Int> = PublishSubject.create()
    val titleObservable: PublishSubject<String> = PublishSubject.create()
    val faviconObservable: BehaviorSubject<Option<Bitmap>> = BehaviorSubject.create()
    val createWindowObservable: PublishSubject<TabInitializer> = PublishSubject.create()
    val closeWindowObservable: PublishSubject<Unit> = PublishSubject.create()
    val colorChangeObservable: BehaviorSubject<Int> = BehaviorSubject.createDefault(defaultColor)

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        createWindowObservable.onNext(ResultMessageInitializer(resultMsg))
        return true
    }

    override fun onCloseWindow(window: WebView) {
        closeWindowObservable.onNext(Unit)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressObservable.onNext(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        titleObservable.onNext(title)
        faviconObservable.onNext(Option.None)
        generateColorAndPropagate(null)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
        faviconObservable.onNext(Option.Some(icon))
        val url = view.url ?: return
        faviconModel.cacheFaviconForUrl(icon, url)
            .subscribeOn(diskScheduler)
            .subscribe()
        generateColorAndPropagate(icon)
    }

    fun generateColorAndPropagate(favicon: Bitmap?) {
        val icon = favicon ?: return run {
            colorChangeObservable.onNext(defaultColor)
        }
        Palette.from(icon).generate { palette ->
            // OR with opaque black to remove transparency glitches
            val color = Color.BLACK or (palette?.getDominantColor(defaultColor) ?: defaultColor)

            // Lighten up the dark color if it is too dark
            val finalColor = if (Utils.isColorTooDark(color)) {
                Utils.mixTwoColors(defaultColor, color, 0.25f)
            } else {
                color
            }
            colorChangeObservable.onNext(finalColor)
        }
    }
}
