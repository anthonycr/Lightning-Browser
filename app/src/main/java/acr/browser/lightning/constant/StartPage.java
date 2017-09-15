/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.inject.Inject;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.html.homepage.HomePageBuilder;
import acr.browser.lightning.search.SearchEngineProvider;
import acr.browser.lightning.utils.Utils;

public class StartPage {

    public static final String FILENAME = "homepage.html";

    @NonNull
    public static File getStartPageFile(@NonNull Application application) {
        return new File(application.getFilesDir(), FILENAME);
    }

    @Inject Application mApp;
    @Inject SearchEngineProvider mSearchEngineProvider;

    public StartPage() {
        BrowserApp.getAppComponent().inject(this);
    }

    @NonNull
    public Single<String> getHomepage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {

                HomePageBuilder homePageBuilder = new HomePageBuilder(mApp, mSearchEngineProvider);

                File homepage = getStartPageFile(mApp);
                FileWriter hWriter = null;
                try {
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    hWriter = new FileWriter(homepage, false);
                    hWriter.write(homePageBuilder.buildPage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Utils.close(hWriter);
                }

                subscriber.onItem(Constants.FILE + homepage);

            }
        });
    }

}
