/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.html.download

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.preference.PreferenceManager
import android.app.Application
import io.reactivex.Single
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class DownloadsPage {

    @Inject internal lateinit var app: Application
    @Inject internal lateinit var preferenceManager: PreferenceManager
    @Inject internal lateinit var manager: DownloadsRepository

    init {
        BrowserApp.appComponent.inject(this)
    }

    fun getDownloadsPage(): Single<String> = manager
            .getAllDownloads()
            .map { list ->
                val directory = preferenceManager.downloadDirectory

                val downloadPageBuilder = DownloadPageBuilder(app, directory)

                val fileName = getDownloadsPageFile(app)
                FileWriter(fileName, false).use {
                    it.write(downloadPageBuilder.buildPage(requireNotNull(list)))
                }

                return@map fileName
            }
            .map { "$FILE$it" }

    companion object {

        const val FILENAME = "downloads.html"

        private fun getDownloadsPageFile(application: Application): File =
                File(application.filesDir, FILENAME)
    }

}
