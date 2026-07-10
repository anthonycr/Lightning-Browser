package acr.browser.lightning.utils

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

class ThreadSafeFileProvider @AssistedInject constructor(
    appCoroutineScope: AppCoroutineScope,
    private val coroutineDispatchers: CoroutineDispatchers,
    @Assisted private val fileProducer: () -> File
) {

    @AssistedFactory
    interface Factory {
        fun create(fileProducer: () -> File): ThreadSafeFileProvider
    }

    suspend fun file(): File = withContext(coroutineDispatchers.io) {
        file.await().apply {
            mkdirs()
        }
    }

    private val file: Deferred<File> = appCoroutineScope.async(coroutineDispatchers.io) {
        fileProducer()
    }
}
