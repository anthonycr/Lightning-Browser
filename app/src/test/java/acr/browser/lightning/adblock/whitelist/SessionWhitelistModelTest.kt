package acr.browser.lightning.adblock.whitelist

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.database.whitelist.AdBlockWhitelistModel
import acr.browser.lightning.database.whitelist.WhitelistItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SessionWhitelistModel]
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class SessionWhitelistModelTest {

    private val adBlockWhitelistModel = mock<AdBlockWhitelistModel>()

    @Test
    fun `isUrlWhitelisted checks domain`() {
        whenever(adBlockWhitelistModel.allWhitelistItems()).thenReturn(Single.just(listOf(WhitelistItem("test.com", 0))))
        val sessionWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(sessionWhitelistModel.isUrlWhitelisted("http://test.com/12345")).isTrue()
        assertThat(sessionWhitelistModel.isUrlWhitelisted("https://test.com")).isTrue()
        assertThat(sessionWhitelistModel.isUrlWhitelisted("https://tests.com")).isFalse()
    }

    @Test
    fun `addUrlToWhitelist updates immediately`() {
        whenever(adBlockWhitelistModel.allWhitelistItems()).thenReturn(Single.just(listOf()))
        whenever(adBlockWhitelistModel.whitelistItemForUrl(any())).thenReturn(Maybe.empty())
        whenever(adBlockWhitelistModel.addWhitelistItem(any())).thenReturn(Completable.complete())
        val sessionWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(sessionWhitelistModel.isUrlWhitelisted("http://test.com")).isFalse()

        sessionWhitelistModel.addUrlToWhitelist("https://test.com/12345")

        assertThat(sessionWhitelistModel.isUrlWhitelisted("http://test.com")).isTrue()
    }

    @Test
    fun `removeUrlFromWhitelist updates immediately`() {
        whenever(adBlockWhitelistModel.allWhitelistItems()).thenReturn(Single.just(listOf(WhitelistItem("test.com", 0))))
        whenever(adBlockWhitelistModel.whitelistItemForUrl(any())).thenReturn(Maybe.empty())
        whenever(adBlockWhitelistModel.removeWhitelistItem(any())).thenReturn(Completable.complete())
        val sessionWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(sessionWhitelistModel.isUrlWhitelisted("http://test.com")).isTrue()

        sessionWhitelistModel.removeUrlFromWhitelist("https://test.com/12345")

        assertThat(sessionWhitelistModel.isUrlWhitelisted("http://test.com")).isFalse()
    }

    @Test
    fun `addUrlToWhitelist persists across instances`() {
        val mutableList = mutableListOf<WhitelistItem>()
        whenever(adBlockWhitelistModel.allWhitelistItems()).thenReturn(Single.just(mutableList))
        whenever(adBlockWhitelistModel.whitelistItemForUrl(any())).thenReturn(Maybe.empty())
        whenever(adBlockWhitelistModel.addWhitelistItem(any())).then { invocation ->
            return@then Completable.fromAction {
                mutableList.add(invocation.arguments[0] as WhitelistItem)
            }
        }

        val oldWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(oldWhitelistModel.isUrlWhitelisted("http://test.com")).isFalse()

        oldWhitelistModel.addUrlToWhitelist("https://test.com/12345")

        val newWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(newWhitelistModel.isUrlWhitelisted("http://test.com")).isTrue()
    }

    @Test
    fun `removeUrlFromWhitelist persists across instances`() {
        val mutableList = mutableListOf(WhitelistItem("test.com", 0))
        whenever(adBlockWhitelistModel.allWhitelistItems()).thenReturn(Single.just(mutableList))
        whenever(adBlockWhitelistModel.allWhitelistItems()).thenReturn(Single.just(mutableList))
        whenever(adBlockWhitelistModel.whitelistItemForUrl(any())).then { invocation ->
            return@then Maybe.fromCallable {
                return@fromCallable mutableList.find { it.url == (invocation.arguments[0] as String) }
            }
        }
        whenever(adBlockWhitelistModel.removeWhitelistItem(any())).then { invocation ->
            return@then Completable.fromAction {
                mutableList.remove(invocation.arguments[0] as WhitelistItem)
            }
        }

        val oldWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(oldWhitelistModel.isUrlWhitelisted("http://test.com")).isTrue()

        oldWhitelistModel.removeUrlFromWhitelist("https://test.com/12345")

        val newWhitelistModel = SessionWhitelistModel(adBlockWhitelistModel, Schedulers.trampoline())

        assertThat(newWhitelistModel.isUrlWhitelisted("http://test.com")).isFalse()
    }
}