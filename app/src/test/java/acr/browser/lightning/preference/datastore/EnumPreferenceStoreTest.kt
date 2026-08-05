package acr.browser.lightning.preference.datastore

import acr.browser.lightning.preference.IntEnum
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.turbine.turbineScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class EnumPreferenceStoreTest {

    private enum class TestEnum(override val value: Int) : IntEnum {
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
    }

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        File.createTempFile("test", ".preferences_pb")
    }

    @Test
    fun `no stored value returns default`() = runTest {
        val enumPreferenceStore = EnumPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = TestEnum.ONE
        )

        assertThat(enumPreferenceStore.get()).isEqualTo(TestEnum.ONE)
    }

    @Test
    fun `stored value returns value`() = runTest {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                set(intPreferencesKey("test"), 2)
            }
        }
        val enumPreferenceStore = EnumPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = TestEnum.ONE
        )

        assertThat(enumPreferenceStore.get()).isEqualTo(TestEnum.TWO)
    }

    @Test
    fun `set stores value`() = runTest {
        val enumPreferenceStore = EnumPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = TestEnum.ONE
        )

        enumPreferenceStore.set(TestEnum.THREE)

        assertThat(enumPreferenceStore.get()).isEqualTo(TestEnum.THREE)
    }

    @Test
    fun `consecutive sets emit to values`() = runTest {
        val enumPreferenceStore = EnumPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = TestEnum.ONE
        )

        turbineScope {
            val values = enumPreferenceStore.values().testIn(backgroundScope)

            enumPreferenceStore.set(TestEnum.ONE)
            enumPreferenceStore.set(TestEnum.TWO)
            enumPreferenceStore.set(TestEnum.THREE)
            enumPreferenceStore.set(TestEnum.FOUR)

            assertThat(values.awaitItem()).isEqualTo(TestEnum.ONE)
            assertThat(values.awaitItem()).isEqualTo(TestEnum.TWO)
            assertThat(values.awaitItem()).isEqualTo(TestEnum.THREE)
            assertThat(values.awaitItem()).isEqualTo(TestEnum.FOUR)
            values.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `duplicate sets emit only once to values`() = runTest {
        val enumPreferenceStore = EnumPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = TestEnum.ONE
        )

        turbineScope {
            val values = enumPreferenceStore.values().testIn(backgroundScope)

            enumPreferenceStore.set(TestEnum.ONE)
            enumPreferenceStore.set(TestEnum.ONE)
            enumPreferenceStore.set(TestEnum.ONE)
            enumPreferenceStore.set(TestEnum.ONE)

            assertThat(values.awaitItem()).isEqualTo(TestEnum.ONE)
            values.ensureAllEventsConsumed()
        }
    }
}
