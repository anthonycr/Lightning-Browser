package acr.browser.lightning.browser.activity

import android.text.Editable
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.ParagraphStyle

/**
 * A [TextWatcher] That removes text styling when text is pasted into the view.
 */
class StyleRemovingTextWatcher : TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

    override fun afterTextChanged(e: Editable) {
        e.getSpans(0, e.length, CharacterStyle::class.java).forEach(e::removeSpan)
        e.getSpans(0, e.length, ParagraphStyle::class.java).forEach(e::removeSpan)
    }
}
