/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.database.downloads.DownloadItem;
import acr.browser.lightning.database.downloads.DownloadsModel;
import acr.browser.lightning.html.download.DownloadPageBuilder;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

public final class DownloadsPage {

    private static final String TAG = "DownloadsPage";

    public static final String FILENAME = "downloads.html";

    @NonNull
    private static File getDownloadsPageFile(@NonNull Application application) {
        return new File(application.getFilesDir(), FILENAME);
    }

    @Inject Application mApp;
    @Inject PreferenceManager mPreferenceManager;
    @Inject DownloadsModel mManager;

    public DownloadsPage() {
        BrowserApp.getAppComponent().inject(this);
    }

    @NonNull
    public Single<String> getDownloadsPage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                buildDownloadsPage();

                File downloadsWebPage = getDownloadsPageFile(mApp);

                subscriber.onItem(Constants.FILE + downloadsWebPage);
                subscriber.onComplete();
            }
        });
    }

    private void buildDownloadsPage() {
        mManager.getAllDownloads()
            .subscribe(new SingleOnSubscribe<List<DownloadItem>>() {
                @Override
                public void onItem(@Nullable List<DownloadItem> list) {
                    Preconditions.checkNonNull(list);
                    String directory = mPreferenceManager.getDownloadDirectory();

                    DownloadPageBuilder downloadPageBuilder = new DownloadPageBuilder(mApp, directory);


                    FileWriter bookWriter = null;
                    try {
                        //noinspection IOResourceOpenedButNotSafelyClosed
                        bookWriter = new FileWriter(getDownloadsPageFile(mApp), false);
                        bookWriter.write(downloadPageBuilder.buildPage(list));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to write download page to disk", e);
                    } finally {
                        Utils.close(bookWriter);
                    }
                }
            });
    }

}
