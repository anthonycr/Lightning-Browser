/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.database.downloads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import acr.browser.lightning.utils.Preconditions;

public class DownloadItem implements Comparable<DownloadItem> {

    @NonNull private String mUrl = "";
    @NonNull private String mTitle = "";
    @NonNull private String mContentSize = "";

    public DownloadItem() {}

    public DownloadItem(@NonNull String url, @NonNull String title, @NonNull String size) {
        Preconditions.checkNonNull(url);
        Preconditions.checkNonNull(title);
        Preconditions.checkNonNull(size);
        this.mUrl = url;
        this.mTitle = title;
        this.mContentSize = size;
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

    @NonNull
    public String getContentSize() {
        return this.mContentSize;
    }

    public void setContentSize(@Nullable String size) {
        this.mContentSize = (size == null) ? "" : size;
    }

    @NonNull
    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public int compareTo(@NonNull DownloadItem another) {
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

        DownloadItem that = (DownloadItem) o;

        return mUrl.equals(that.mUrl) &&
            mTitle.equals(that.mTitle) &&
            mContentSize.equals(that.mContentSize);

    }

    @Override
    public int hashCode() {
        int result = mUrl.hashCode();
        result = 31 * result + mTitle.hashCode();
        result = 31 * result + mContentSize.hashCode();

        return result;
    }
}
