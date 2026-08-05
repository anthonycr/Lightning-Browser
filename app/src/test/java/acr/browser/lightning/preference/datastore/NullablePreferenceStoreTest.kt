package acr.browser.lightning.preference.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.turbineScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class NullablePreferenceStoreTest {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        File.createTempFile("test", ".preferences_pb")
    }

    @Test
    fun `no stored value returns null`() = runTest {
        val nullablePreferenceStore = NullablePreferenceStore(
            key = stringPreferencesKey("test"),
            dataStore = dataStore
        )

        assertThat(nullablePreferenceStore.get()).isNull()
    }

    @Test
    fun `stored value returns value`() = runTest {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                set(stringPreferencesKey("test"), "one")
            }
        }
        val nullablePreferenceStore = NullablePreferenceStore(
            key = stringPreferencesKey("test"),
            dataStore = dataStore
        )

        assertThat(nullablePreferenceStore.get()).isEqualTo("one")
    }

    @Test
    fun `set stores value`() = runTest {
        val nullablePreferenceStore = NullablePreferenceStore(
            key = stringPreferencesKey("test"),
            dataStore = dataStore
        )

        nullablePreferenceStore.set("two")

        assertThat(nullablePreferenceStore.get()).isEqualTo("two")
    }

    @Test
    fun `consecutive sets emit to values`() = runTest {
        val nullablePreferenceStore = NullablePreferenceStore(
            key = stringPreferencesKey("test"),
            dataStore = dataStore,
        )

        turbineScope {
            val values = nullablePreferenceStore.values().testIn(backgroundScope)

            nullablePreferenceStore.set("one")
            nullablePreferenceStore.set("two")
            nullablePreferenceStore.set("three")
            nullablePreferenceStore.set("four")

            assertThat(values.awaitItem()).isNull()
            assertThat(values.awaitItem()).isEqualTo("one")
            assertThat(values.awaitItem()).isEqualTo("two")
            assertThat(values.awaitItem()).isEqualTo("three")
            assertThat(values.awaitItem()).isEqualTo("four")
            values.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `duplicate sets emit only once to values`() = runTest {
        val nullablePreferenceStore = NullablePreferenceStore(
            key = stringPreferencesKey("test"),
            dataStore = dataStore
        )

        turbineScope {
            val values = nullablePreferenceStore.values().testIn(backgroundScope)

            nullablePreferenceStore.set("one")
            nullablePreferenceStore.set("one")
            nullablePreferenceStore.set("one")
            nullablePreferenceStore.set("one")

            assertThat(values.awaitItem()).isNull()
            assertThat(values.awaitItem()).isEqualTo("one")
            values.ensureAllEventsConsumed()
        }
    }
}
