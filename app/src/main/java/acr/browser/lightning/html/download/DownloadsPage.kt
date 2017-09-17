/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.html.download

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.downloads.DownloadItem
import acr.browser.lightning.database.downloads.DownloadsModel
import acr.browser.lightning.preference.PreferenceManager
import android.app.Application
import com.anthonycr.bonsai.Single
import com.anthonycr.bonsai.SingleOnSubscribe
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class DownloadsPage {

    @Inject internal lateinit var app: Application
    @Inject internal lateinit var preferenceManager: PreferenceManager
    @Inject internal lateinit var manager: DownloadsModel

    init {
        BrowserApp.getAppComponent().inject(this)
    }

    fun getDownloadsPage(): Single<String> = Single.create { subscriber ->
        buildDownloadsPage()

        val downloadsWebPage = getDownloadsPageFile(app)

        subscriber.onItem("$FILE$downloadsWebPage")
        subscriber.onComplete()
    }

    private fun buildDownloadsPage() {
        manager.getAllDownloads()
                .subscribe(object : SingleOnSubscribe<List<DownloadItem>>() {
                    override fun onItem(list: List<DownloadItem>?) {
                        requireNotNull(list)
                        val directory = preferenceManager.downloadDirectory

                        val downloadPageBuilder = DownloadPageBuilder(app, directory)

                        FileWriter(getDownloadsPageFile(app), false).use {
                            it.write(downloadPageBuilder.buildPage(list!!))
                        }
                    }
                })
    }

    companion object {

        const val FILENAME = "downloads.html"

        private fun getDownloadsPageFile(application: Application): File =
                File(application.filesDir, FILENAME)
    }

}
