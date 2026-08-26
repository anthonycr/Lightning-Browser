package acr.browser.lightning.settings.licenses

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads the dependencies from the [DependenciesRepository] and constructs the UI as a list of
 * [LicensesScreenState] instances.
 */
class LicensesScreenPresenter(
    private val dependenciesRepository: DependenciesRepository,
    appCoroutineScope: AppCoroutineScope,
    coroutineDispatchers: CoroutineDispatchers,
) : ViewModel() {

    class Factory @Inject constructor(
        private val dependenciesRepository: DependenciesRepository,
        private val appCoroutineScope: AppCoroutineScope,
        private val coroutineDispatchers: CoroutineDispatchers,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == LicensesScreenPresenter::class.java)
            return LicensesScreenPresenter(
                dependenciesRepository,
                appCoroutineScope,
                coroutineDispatchers,
            ) as T
        }
    }

    private val _state = MutableStateFlow<LicensesScreenState>(LicensesScreenState.Loading)

    /**
     * The current state of the license screen.
     */
    val state: StateFlow<LicensesScreenState> = _state

    init {
        appCoroutineScope.launch(coroutineDispatchers.default) {
            val allDependencies = dependenciesRepository.readDependencies()

            val dependenciesByLicense = allDependencies.groupBy { dependency ->
                dependency.spdxLicenses.firstOrNull() ?: dependency.unknownLicenses.firstOrNull()
            }.mapValues { (_, dependencies) ->
                dependencies.groupBy { it.groupId }
            }

            val listItems = dependenciesByLicense.entries.flatMap { (license, dependencyGroups) ->
                val licenseText = license?.name ?: "Unknown"
                listOf(LicensesScreenState.Data.Items.Header(licenseText, license?.url.orEmpty())) +
                    dependencyGroups.flatMap { (groupId, dependencies) ->
                        listOf(LicensesScreenState.Data.Items.SubHeader(groupId)) +
                            dependencies.map {
                                LicensesScreenState.Data.Items.Entry(
                                    "${it.artifactId}:${it.version}",
                                    it.sourceControl?.url.orEmpty()
                                )
                            }
                    }
            }

            _state.value = LicensesScreenState.Data(listItems)
        }
    }
}

/**
 * The screen's states.
 */
sealed interface LicensesScreenState {
    /**
     * The loading state, shown while the [Data] state is being loaded.
     */
    data object Loading : LicensesScreenState

    /**
     * The data containing all dependencies.
     */
    data class Data(
        val list: List<Items>
    ) : LicensesScreenState {
        /**
         * Potential items in the list.
         */
        sealed interface Items {
            /**
             * The header, showing the license, not indented.
             */
            data class Header(val text: String, val url: String) : Items

            /**
             * The sub-header, showing the group, indented.
             */
            data class SubHeader(val text: String) : Items

            /**
             * The entry, showing the artifact, double indented.
             */
            data class Entry(val text: String, val url: String) : Items
        }
    }
}
