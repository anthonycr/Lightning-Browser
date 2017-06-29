package acr.browser.lightning.browser.bookmark;

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

    /**
     * Sets the current folder that is being shown.
     * Use null as the root folder.
     *
     * @param folder the current folder, null for root.
     */
    public void setCurrentFolder(@Nullable String folder) {
        mCurrentFolder = folder;
    }

    /**
     * Determines if the current folder is
     * the root folder.
     *
     * @return true if the current folder is
     * the root, false otherwise.
     */
    public boolean isRootFolder() {
        return mCurrentFolder == null;
    }

    /**
     * Gets the current folder that is being shown.
     *
     * @return the current folder, null for root.
     */
    @Nullable
    public String getCurrentFolder() {
        return mCurrentFolder;
    }

}
