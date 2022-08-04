package acr.browser.lightning.browser.keys

import android.view.KeyEvent
import javax.inject.Inject

/**
 * Adapts [KeyEvents][KeyEvent] to [KeyCombos][KeyCombo].
 */
class KeyEventAdapter @Inject constructor() {

    /**
     * Adapt the [event] or return null if the key combo is unsupported.
     */
    fun adaptKeyEvent(event: KeyEvent): KeyCombo? {
        when {
            event.isCtrlPressed -> when (event.keyCode) {
                KeyEvent.KEYCODE_F -> {
                    // Search in page
                    return KeyCombo.CTRL_F
                }
                KeyEvent.KEYCODE_T -> {
                    // New tab
                    return KeyCombo.CTRL_T
                }
                KeyEvent.KEYCODE_W -> {
                    // Close current tab
                    return KeyCombo.CTRL_W
                }
                KeyEvent.KEYCODE_Q -> {
                    // Close browser
                    return KeyCombo.CTRL_Q
                }
                KeyEvent.KEYCODE_R -> {
                    // Refresh
                    return KeyCombo.CTRL_R
                }
                KeyEvent.KEYCODE_TAB -> {
                    return if (event.isShiftPressed) {
                        // Go back one tab
                        KeyCombo.CTRL_SHIFT_TAB
                    } else {
                        // Go forward one tab
                        KeyCombo.CTRL_TAB
                    }
                }
            }
            event.keyCode == KeyEvent.KEYCODE_SEARCH -> {
                // Highlight search field
                return KeyCombo.SEARCH
            }
            event.isAltPressed -> {
                // Alt + tab number
                if (event.keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
                    // Choose tab by number
                    return when (event.keyCode) {
                        KeyEvent.KEYCODE_0 -> KeyCombo.ALT_0
                        KeyEvent.KEYCODE_1 -> KeyCombo.ALT_1
                        KeyEvent.KEYCODE_2 -> KeyCombo.ALT_2
                        KeyEvent.KEYCODE_3 -> KeyCombo.ALT_3
                        KeyEvent.KEYCODE_4 -> KeyCombo.ALT_4
                        KeyEvent.KEYCODE_5 -> KeyCombo.ALT_5
                        KeyEvent.KEYCODE_6 -> KeyCombo.ALT_6
                        KeyEvent.KEYCODE_7 -> KeyCombo.ALT_8
                        KeyEvent.KEYCODE_8 -> KeyCombo.ALT_8
                        KeyEvent.KEYCODE_9 -> KeyCombo.ALT_9
                        else -> null
                    }
                }
            }
        }
        return null
    }

}
