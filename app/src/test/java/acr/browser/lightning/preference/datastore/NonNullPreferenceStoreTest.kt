package acr.browser.lightning.preference.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.turbine.turbineScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class NonNullPreferenceStoreTest {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        File.createTempFile("test", ".preferences_pb")
    }

    @Test
    fun `no stored value returns default`() = runTest {
        val nonNullPreferenceStore = NonNullPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = 1
        )

        assertThat(nonNullPreferenceStore.get()).isEqualTo(1)
    }

    @Test
    fun `stored value returns value`() = runTest {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                set(intPreferencesKey("test"), 2)
            }
        }
        val nonNullPreferenceStore = NonNullPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = 1
        )

        assertThat(nonNullPreferenceStore.get()).isEqualTo(2)
    }

    @Test
    fun `set stores value`() = runTest {
        val nonNullPreferenceStore = NonNullPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = 1
        )

        nonNullPreferenceStore.set(3)

        assertThat(nonNullPreferenceStore.get()).isEqualTo(3)
    }

    @Test
    fun `consecutive sets emit to values`() = runTest {
        val nonNullPreferenceStore = NonNullPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = 1
        )

        turbineScope {
            val values = nonNullPreferenceStore.values().testIn(backgroundScope)

            nonNullPreferenceStore.set(1)
            nonNullPreferenceStore.set(2)
            nonNullPreferenceStore.set(3)
            nonNullPreferenceStore.set(4)

            assertThat(values.awaitItem()).isEqualTo(1)
            assertThat(values.awaitItem()).isEqualTo(2)
            assertThat(values.awaitItem()).isEqualTo(3)
            assertThat(values.awaitItem()).isEqualTo(4)
            values.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `duplicate sets emit only once to values`() = runTest {
        val nonNullPreferenceStore = NonNullPreferenceStore(
            key = intPreferencesKey("test"),
            dataStore = dataStore,
            defaultValue = 1
        )

        turbineScope {
            val values = nonNullPreferenceStore.values().testIn(backgroundScope)

            nonNullPreferenceStore.set(1)
            nonNullPreferenceStore.set(1)
            nonNullPreferenceStore.set(1)
            nonNullPreferenceStore.set(1)

            assertThat(values.awaitItem()).isEqualTo(1)
            values.ensureAllEventsConsumed()
        }
    }
}
