package acr.browser.lightning.adblock.allowlist

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.FakeCoroutineDispatchers
import acr.browser.lightning.database.allowlist.AllowListEntry
import acr.browser.lightning.log.NoOpLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SessionAllowListModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [SDK_VERSION])
class SessionAllowListModelTest {

    private val adBlockAllowListModel = FakeAdBlockAllowListRepository()

    @Test
    fun `isUrlAllowListed checks domain`() = runTest {
        adBlockAllowListModel.allowList = mutableListOf(AllowListEntry("test.com", 0))
        val sessionAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com/12345")).isTrue()
        assertThat(sessionAllowListModel.isUrlAllowedAds("https://test.com")).isTrue()
        assertThat(sessionAllowListModel.isUrlAllowedAds("https://tests.com")).isFalse()
    }

    @Test
    fun `addUrlToAllowList updates immediately`() = runTest {
        val sessionAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()

        sessionAllowListModel.addUrlToAllowList("https://test.com/12345")

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()
    }

    @Test
    fun `removeUrlFromAllowList updates immediately`() = runTest {
        adBlockAllowListModel.allowList.add(AllowListEntry("test.com", 0))
        val sessionAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()

        sessionAllowListModel.removeUrlFromAllowList("https://test.com/12345")

        assertThat(sessionAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()
    }

    @Test
    fun `addUrlToAllowList persists across instances`() = runTest {
        val oldAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(oldAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()

        oldAllowListModel.addUrlToAllowList("https://test.com/12345")

        val newAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(newAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()
    }

    @Test
    fun `removeUrlFromAllowList persists across instances`() = runTest {
        adBlockAllowListModel.allowList.add(AllowListEntry("test.com", 0))
        val oldAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(oldAllowListModel.isUrlAllowedAds("http://test.com")).isTrue()

        oldAllowListModel.removeUrlFromAllowList("https://test.com/12345")

        val newAllowListModel = SessionAllowListModel(
            adBlockAllowListModel = adBlockAllowListModel,
            logger = NoOpLogger(),
            appCoroutineScope = AppCoroutineScope(this),
            coroutineDispatchers = FakeCoroutineDispatchers(testScheduler)
        )

        advanceUntilIdle()

        assertThat(newAllowListModel.isUrlAllowedAds("http://test.com")).isFalse()
    }
}
