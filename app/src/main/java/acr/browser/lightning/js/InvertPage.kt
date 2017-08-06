package acr.browser.lightning.js

import com.anthonycr.mezzanine.FileStream

/**
 * Invert the color of the page.
 */
@FileStream("app/js/InvertPage.js")
interface InvertPage {

    fun provideJs(): String

}