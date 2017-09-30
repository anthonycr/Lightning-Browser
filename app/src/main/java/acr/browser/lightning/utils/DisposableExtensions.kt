package acr.browser.lightning.utils

import io.reactivex.disposables.Disposable

fun Disposable?.safeDispose() {
    if (this?.isDisposed == false) {
        dispose()
    }
}