package acr.browser.lightning.settings.licenses

/**
 * Extracts the dependencies of the project.
 */
interface DependenciesRepository {

    /**
     * Read the dependencies from assets and return them.
     */
    suspend fun readDependencies(): List<OpenSourceDependency>
}

