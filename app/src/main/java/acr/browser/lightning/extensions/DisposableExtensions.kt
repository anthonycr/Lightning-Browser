package acr.browser.lightning.extensions

import io.reactivex.disposables.Disposable

fun Disposable?.safeDispose() {
    if (this?.isDisposed == false) {
        dispose()
    }
}