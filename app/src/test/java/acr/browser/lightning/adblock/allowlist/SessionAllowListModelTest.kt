package acr.browser.lightning.adblock.allowlist

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import acr.browser.lightning.database.allowlist.AdBlockAllowListRepository
import acr.browser.lightning.database.allowlist.AllowListEntry
import acr.browser.lightning.log.NoOpLogger
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
 * Unit tests for [SessionAllowListModel].
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class, sdk = [SDK_VERSION])
class SessionAllowListModelTest {

    private val adBlockAllowListModel = mock<AdBlockAllowListRepository>()

    @Test
    fun `isUrlAllowListed checks domain`() {
        whenever(adBlockAllowListModel.allAllowListItems()).thenReturn(Single.just(listOf(AllowListEntry("test.com", 0))))
        val sessionAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com/12345")).isTrue()
        assertThat(sessionAllowListModel.isUrlAllowedAds("https://test.com")).isTrue()
        assertThat(sessionAllowListModel.isUrlAllowedAds("https://tests.com")).isFalse()
    }

    @Test
    fun `addUrlToAllowList updates immediately`() {
        whenever(adBlockAllowListModel.allAllowListItems()).thenReturn(Single.just(emptyList()))
        whenever(adBlockAllowListModel.allowListItemForUrl(any())).thenReturn(Maybe.empty())
        whenever(adBlockAllowListModel.addAllowListItem(any())).thenReturn(Completable.complete())
        val sessionAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()

        sessionAllowListModel.addUrlToAllowList("https://test.com/12345")

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()
    }

    @Test
    fun `removeUrlFromAllowList updates immediately`() {
        whenever(adBlockAllowListModel.allAllowListItems()).thenReturn(Single.just(listOf(AllowListEntry("test.com", 0))))
        whenever(adBlockAllowListModel.allowListItemForUrl(any())).thenReturn(Maybe.empty())
        whenever(adBlockAllowListModel.removeAllowListItem(any())).thenReturn(Completable.complete())
        val sessionAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()

        sessionAllowListModel.removeUrlFromAllowList("https://test.com/12345")

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()
    }

    @Test
    fun `addUrlToAllowList persists across instances`() {
        val mutableList = mutableListOf<AllowListEntry>()
        whenever(adBlockAllowListModel.allAllowListItems()).thenReturn(Single.just(mutableList))
        whenever(adBlockAllowListModel.allowListItemForUrl(any())).thenReturn(Maybe.empty())
        whenever(adBlockAllowListModel.addAllowListItem(any())).then { invocation ->
            return@then Completable.fromAction {
                mutableList.add(invocation.arguments[0] as AllowListEntry)
            }
        }

        val oldAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(oldAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()

        oldAllowListModel.addUrlToAllowList("https://test.com/12345")

        val newAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(newAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()
    }

    @Test
    fun `removeUrlFromAllowList persists across instances`() {
        val mutableList = mutableListOf(AllowListEntry("test.com", 0))
        whenever(adBlockAllowListModel.allAllowListItems()).thenReturn(Single.just(mutableList))
        whenever(adBlockAllowListModel.allAllowListItems()).thenReturn(Single.just(mutableList))
        whenever(adBlockAllowListModel.allowListItemForUrl(any())).then { invocation ->
            return@then Maybe.fromCallable {
                return@fromCallable mutableList.find { it.domain == (invocation.arguments[0] as String) }
            }
        }
        whenever(adBlockAllowListModel.removeAllowListItem(any())).then { invocation ->
            return@then Completable.fromAction {
                mutableList.remove(invocation.arguments[0] as AllowListEntry)
            }
        }

        val oldAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(oldAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()

        oldAllowListModel.removeUrlFromAllowList("https://test.com/12345")

        val newAllowListModel = SessionAllowListModel(adBlockAllowListModel, Schedulers.trampoline(), NoOpLogger())

        assertThat(newAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()
    }
}
