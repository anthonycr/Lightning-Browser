package acr.browser.lightning.activity;

import android.support.annotation.Nullable;

import acr.browser.lightning.browser.BookmarksView;

/**
 * The UI model representing the current folder shown
 * by the {@link BookmarksView}.
 * <p>
 * Created by anthonycr on 5/7/17.
 */
public class BookmarkUiModel {

    @Nullable private String mCurrentFolder;

    public void setCurrentFolder(@Nullable String folder) {
        mCurrentFolder = folder;
    }

    public boolean isRootFolder() {
        return mCurrentFolder == null;
    }

    @Nullable
    public String getCurrentFolder() {
        return mCurrentFolder;
    }

}
