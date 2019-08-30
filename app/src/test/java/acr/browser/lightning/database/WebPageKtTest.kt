package acr.browser.lightning.database

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests for [WebPage].
 */
class WebPageKtTest {

    @Test
    fun `asFolder returns root folder for null string`() {
        assertThat((null as String?).asFolder()).isEqualTo(Bookmark.Folder.Root)
    }

    @Test
    fun `asFolder returns root for blank string`() {
        assertThat("  ".asFolder()).isEqualTo(Bookmark.Folder.Root)
    }

    @Test
    fun `asFolder returns correct folder entry for non blank string`() {
        assertThat("test".asFolder()).isEqualTo(Bookmark.Folder.Entry(
            url = "folder://test",
            title = "test"
        ))
    }
}
