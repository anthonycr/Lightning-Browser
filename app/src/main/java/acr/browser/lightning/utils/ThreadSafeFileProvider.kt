package acr.browser.lightning.utils

import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.concurrency.CoroutineDispatchers
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.File

class ThreadSafeFileProvider @AssistedInject constructor(
    @DiskScheduler private val diskScheduler: Scheduler,
    appCoroutineScope: CoroutineScope,
    coroutineDispatchers: CoroutineDispatchers,
    @Assisted private val fileProducer: () -> File
) {

    private val fileSingle = Single.fromCallable(fileProducer).cache()

    @AssistedFactory
    interface Factory {
        fun create(fileProducer: () -> File): ThreadSafeFileProvider
    }

    val file: Deferred<File> = appCoroutineScope.async(coroutineDispatchers.io) {
        fileProducer()
    }

    fun file(): Single<File> = fileSingle.subscribeOn(diskScheduler)

}
