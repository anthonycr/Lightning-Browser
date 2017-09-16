/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.html.homepage

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.constant.Constants
import acr.browser.lightning.search.SearchEngineProvider
import android.app.Application
import com.anthonycr.bonsai.Single
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class StartPage {

    @Inject internal lateinit var app: Application
    @Inject internal lateinit var searchEngineProvider: SearchEngineProvider

    init {
        BrowserApp.getAppComponent().inject(this)
    }

    fun createHomePage(): Single<String> = Single.create { subscriber ->
        val homePageBuilder = HomePageBuilder(app, searchEngineProvider)

        val homepage = getStartPageFile(app)

        FileWriter(homepage, false).use {
            it.write(homePageBuilder.buildPage())
        }

        subscriber.onItem("${Constants.FILE}$homepage")
    }

    companion object {

        const val FILENAME = "homepage.html"

        @JvmStatic
        fun getStartPageFile(application: Application): File = File(application.filesDir, FILENAME)
    }

}
