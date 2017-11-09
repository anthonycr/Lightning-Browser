package acr.browser.lightning.dialog

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

/**
 * Unit tests for [DialogItem].
 */
class DialogItemTest {

    @Test
    fun `onClick triggers onClick function reference`() {
        // mock
        val onClick = mock<() -> Unit>()
        val dialogItem = DialogItem(0, false, onClick)

        // train
        dialogItem.onClick()

        // verify
        verify(onClick).invoke()
    }
}