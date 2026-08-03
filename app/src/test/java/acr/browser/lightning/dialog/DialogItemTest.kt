package acr.browser.lightning.dialog

import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify
import org.junit.Test

/**
 * Unit tests for [DialogItem].
 */
class DialogItemTest {

    private val onClick = fake<() -> Unit>()

    @Test
    fun `onClick triggers onClick function reference`() {
        // fake
        val dialogItem = DialogItem(title = 0, isConditionMet = false, onClick = onClick)

        // train
        dialogItem.onClick()

        // verify
        verify(onClick) {
            onClick.invoke()
        }
    }
}
