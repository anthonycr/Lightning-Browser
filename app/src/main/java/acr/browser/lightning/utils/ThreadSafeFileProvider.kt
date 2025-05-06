package acr.browser.lightning.utils

import acr.browser.lightning.browser.di.DiskScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import java.io.File

class ThreadSafeFileProvider @AssistedInject constructor(
    @DiskScheduler private val diskScheduler: Scheduler,
    @Assisted private val fileProducer: () -> File
) {

    private val fileSingle = Single.fromCallable(fileProducer).cache()

    @AssistedFactory
    interface Factory {
        fun create(fileProducer: () -> File): ThreadSafeFileProvider
    }

    fun file(): Single<File> = fileSingle.subscribeOn(diskScheduler)

}
