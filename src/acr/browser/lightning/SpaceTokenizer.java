/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.widget.MultiAutoCompleteTextView.Tokenizer;

public class SpaceTokenizer implements Tokenizer {

	@Override
	public int findTokenEnd(CharSequence text, int cursor) {
		//int i = cursor;
		//int len = text.length();

		/*while (i < len) {
			if (text.charAt(i) == ' ') {
				return i;
			} else {
				i++;
			}
		}
*/
		return text.length();
	}

	@Override
	public int findTokenStart(CharSequence text, int cursor) {
		int i = cursor;

		while (i > 0 && text.charAt(i - 1) != ' ') {
			i--;
		}
		while (i < cursor && text.charAt(i) == ' ') {
			i++;
		}

		return i;
	}

	@Override
	public CharSequence terminateToken(CharSequence text) {
		//int i = text.length();
		if(text.charAt(text.length()-1) != ' '){
			text = text + " ";
		}
		return text;
		/*while (i > 0 && text.charAt(i - 1) == ' ') {
			i--;
		}

		if (i > 0 && text.charAt(i - 1) == ' ') {
			return text;
		} else {
			if (text instanceof Spanned) {
				SpannableString sp = new SpannableString(text + " ");
				TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
						Object.class, sp, 0);
				return sp;
			} else {
				return text + " ";
			}
		}*/
	}
}
