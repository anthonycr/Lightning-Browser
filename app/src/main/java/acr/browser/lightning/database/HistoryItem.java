/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class HistoryItem implements Comparable<HistoryItem> {

    // private variables
    @NonNull
    private String mUrl = "";

    @NonNull
    private String mTitle = "";

    @NonNull
    private String mFolder = "";

    @Nullable
    private Bitmap mBitmap = null;

    private int mImageId = 0;
    private int mOrder = 0;
    private boolean mIsFolder = false;

    // Empty constructor
    public HistoryItem() {}

    public HistoryItem(HistoryItem item) {
        this.mUrl = item.mUrl;
        this.mTitle = item.mTitle;
        this.mFolder = item.mFolder;
        this.mOrder = item.mOrder;
        this.mIsFolder = item.mIsFolder;
    }

    // constructor
    public HistoryItem(@NonNull String url, @NonNull String title) {
        this.mUrl = url;
        this.mTitle = title;
        this.mBitmap = null;
    }

    // constructor
    public HistoryItem(@NonNull String url, @NonNull String title, int imageId) {
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

    public void setBitmap(Bitmap image) {
        mBitmap = image;
    }

    public void setFolder(String folder) {
        mFolder = (folder == null) ? "" : folder;
    }

    public void setOrder(int order) {
        mOrder = order;
    }

    public int getOrder() {
        return mOrder;
    }

    public String getFolder() {
        return mFolder;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    // getting name
    public String getUrl() {
        return this.mUrl;
    }

    // setting name
    public void setUrl(String url) {
        this.mUrl = (url == null) ? "" : url;
    }

    // getting phone number
    public String getTitle() {
        return this.mTitle;
    }

    // setting phone number
    public void setTitle(String title) {
        this.mTitle = (title == null) ? "" : title;
    }

    public void setIsFolder(boolean isFolder) {
        mIsFolder = isFolder;
    }

    public boolean isFolder() {
        return mIsFolder;
    }

    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public int compareTo(@NonNull HistoryItem another) {
        int compare = this.mTitle.compareTo(another.mTitle);
        if (compare == 0) {
            return this.mUrl.compareTo(another.mUrl);
        }
        return compare;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) return true;
        if (object == null) return false;
        if (!(object instanceof HistoryItem)) return false;

        HistoryItem that = (HistoryItem) object;

        return mImageId == that.mImageId &&
                this.mTitle.equals(that.mTitle) && this.mUrl.equals(that.mUrl) &&
                this.mFolder.equals(that.mFolder);
    }

    @Override
    public int hashCode() {

        int result = mUrl.hashCode();
        result = 31 * result + mImageId;
        result = 31 * result + mTitle.hashCode();
        result = 32 * result + mFolder.hashCode();
        result = 31 * result + mImageId;

        return result;
    }
}
