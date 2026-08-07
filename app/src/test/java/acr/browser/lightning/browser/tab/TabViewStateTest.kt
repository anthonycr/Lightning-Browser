package acr.browser.lightning.browser.tab

import acr.browser.lightning.tab.TabModel
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TabViewStateTest {

    private fun createTabViewState(
        icon: TabModel.Favicon = TabModel.Favicon.None,
        preview: TabModel.Preview = TabModel.Preview.None
    ): TabViewState = TabViewState(
        id = 1,
        icon = icon,
        title = "test",
        isSelected = true,
        preview = preview,
    )

    @Test
    fun `verify equality`() {
        val faviconPermutations = mapOf(
            (TabModel.Favicon.None to TabModel.Favicon.None) to true,
            (TabModel.Favicon.None to TabModel.Favicon.Frozen) to false,
            (TabModel.Favicon.None to TabModel.Favicon.Icon(TestImageBitmap())) to false,
            (TabModel.Favicon.Frozen to TabModel.Favicon.Icon(TestImageBitmap())) to false,
            (TabModel.Favicon.Frozen to TabModel.Favicon.Frozen) to true,
            (TabModel.Favicon.Icon(TestImageBitmap()) to TabModel.Favicon.Icon(TestImageBitmap())) to false,
        )

        val previewPermutations = mapOf(
            (TabModel.Preview.None to TabModel.Preview.None) to true,
            (TabModel.Preview.None to TabModel.Preview.Image("test", 0L)) to false,
            (TabModel.Preview.Image("a", 0L) to TabModel.Preview.Image("a", 0L)) to true,
            (TabModel.Preview.Image("a", 0L) to TabModel.Preview.Image("b", 0L)) to false,
            (TabModel.Preview.Image("a", 0L) to TabModel.Preview.Image("a", 1L)) to false,
        )

        TabModel.Favicon::class.checkAllPermutations(faviconPermutations)
        TabModel.Preview::class.checkAllPermutations(previewPermutations)

        faviconPermutations.forEach { (one, another), result ->
            if (result) {
                assertThat(createTabViewState(icon = one)).isEqualTo(createTabViewState(icon = another))
            } else {
                assertThat(createTabViewState(icon = one)).isNotEqualTo(createTabViewState(icon = another))
            }
        }

        previewPermutations.forEach { (one, another), result ->
            if (result) {
                assertThat(createTabViewState(preview = one)).isEqualTo(createTabViewState(preview = another))
            } else {
                assertThat(createTabViewState(preview = one))
                    .isNotEqualTo(createTabViewState(preview = another))
            }
        }
    }

    class TestImageBitmap(
        override val width: Int = 0,
        override val height: Int = 0,
        override val colorSpace: ColorSpace = ColorSpaces.Srgb,
        override val hasAlpha: Boolean = false,
        override val config: ImageBitmapConfig = ImageBitmapConfig.Alpha8
    ) : ImageBitmap {
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int
        ) = Unit

        override fun prepareToDraw() = Unit
    }
}
