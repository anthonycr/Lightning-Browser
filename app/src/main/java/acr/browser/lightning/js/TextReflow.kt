package acr.browser.lightning.js

import com.anthonycr.mezzanine.FileStream

/**
 * Force the text to reflow.
 */
@FileStream("src/main/js/TextReflow.js")
interface TextReflow {

    fun provideJs(): String

}