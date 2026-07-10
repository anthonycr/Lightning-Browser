@file:JvmName("Injector")

package acr.browser.lightning.browser.di

import acr.browser.lightning.BrowserApp
import android.content.Context

/**
 * The [AppComponent] attached to the application [Context].
 */
val Context.injector: AppComponent
    get() = (applicationContext as BrowserApp).applicationComponent

