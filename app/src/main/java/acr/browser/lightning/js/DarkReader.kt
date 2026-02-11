package acr.browser.lightning.js

import com.anthonycr.mezzanine.FileStream

/**
 * Dark Reader script for enabling dark mode on websites.
 */
@FileStream("src/main/js/DarkReader.js")
interface DarkReader {

    fun provideJs(): String

}
