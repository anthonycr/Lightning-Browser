package acr.browser.lightning.extensions

import android.view.View
import android.view.ViewGroup
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test

/**
 * Unit tests for ViewExtensions.kt
 */
class ViewExtensionsTest {

    private val mockView = mock<View>()

    @Test
    fun `removeFromParent no-ops without parent`() {
        // mock
        val mockParent: ViewGroup? = null
        whenever(mockView.parent).then { mockParent }

        // train
        mockView.removeFromParent()
    }

    @Test
    fun `removeFromParent removes view with valid parent`() {
        // mock
        val mockParent = mock<ViewGroup>()
        whenever(mockView.parent).then { mockParent }

        // train
        mockView.removeFromParent()

        // verify
        verify(mockParent).removeView(mockView)
    }

    @Test
    fun `removeFromParent allows null views`() {
        val nullView: View? = null

        nullView.removeFromParent()
    }

}