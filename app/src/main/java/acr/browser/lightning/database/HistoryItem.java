/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import acr.browser.lightning.utils.Preconditions;

public class HistoryItem implements Comparable<HistoryItem> {

    @NonNull private String mUrl = "";
    @NonNull private String mTitle = "";
    @NonNull private String mFolder = "";
    @Nullable private Bitmap mBitmap = null;

    private int mImageId = 0;

    private int mPosition = 0;

    private boolean mIsFolder = false;

    public HistoryItem() {}

    public HistoryItem(@NonNull String url, @NonNull String title) {
        Preconditions.checkNonNull(url);
        Preconditions.checkNonNull(title);
        this.mUrl = url;
        this.mTitle = title;
        this.mBitmap = null;
    }

    public HistoryItem(@NonNull String url, @NonNull String title, int imageId) {
        Preconditions.checkNonNull(url);
        Preconditions.checkNonNull(title);
        this.mUrl = url;
        this.mTitle = title;
        this.mBitmap = null;
        this.mImageId = imageId;
    }

    public int getImageId() {
        return this.mImageId;
    }

    public void setImageId(int id) {
        this.mImageId = id;
    }

    public void setBitmap(@Nullable Bitmap image) {
        mBitmap = image;
    }

    public void setFolder(@Nullable String folder) {
        mFolder = (folder == null) ? "" : folder;
    }

    public void setPosition(int order) {
        mPosition = order;
    }

    public int getPosition() {
        return mPosition;
    }

    @NonNull
    public String getFolder() {
        return mFolder;
    }

    @Nullable
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @NonNull
    public String getUrl() {
        return this.mUrl;
    }

    public void setUrl(@Nullable String url) {
        this.mUrl = (url == null) ? "" : url;
    }

    @NonNull
    public String getTitle() {
        return this.mTitle;
    }

    public void setTitle(@Nullable String title) {
        this.mTitle = (title == null) ? "" : title;
    }

    public void setIsFolder(boolean isFolder) {
        mIsFolder = isFolder;
    }

    public boolean isFolder() {
        return mIsFolder;
    }

    @NonNull
    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public int compareTo(@NonNull HistoryItem another) {
        int compare = this.mTitle.compareToIgnoreCase(another.mTitle);
        if (compare == 0) {
            return this.mUrl.compareTo(another.mUrl);
        }
        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryItem that = (HistoryItem) o;

        return mImageId == that.mImageId &&
            mPosition == that.mPosition &&
            mIsFolder == that.mIsFolder &&
            mUrl.equals(that.mUrl) &&
            mTitle.equals(that.mTitle) &&
            mFolder.equals(that.mFolder);

    }

    @Override
    public int hashCode() {
        int result = mUrl.hashCode();
        result = 31 * result + mTitle.hashCode();
        result = 31 * result + mFolder.hashCode();
        result = 31 * result + mImageId;
        result = 31 * result + mPosition;
        result = 31 * result + (mIsFolder ? 1 : 0);

        return result;
    }
}
