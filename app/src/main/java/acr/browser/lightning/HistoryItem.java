/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.graphics.Bitmap;

public class HistoryItem implements Comparable<HistoryItem> {

	// private variables
	private int mId = 0;
	private String mUrl = "";
	private String mTitle = "";
	private String mFolder = "";
	private Bitmap mBitmap = null;
	private int mImageId = 0;
	private int mOrder = 0;

	// Empty constructor
	public HistoryItem() {

	}

	// constructor
	public HistoryItem(int id, String url, String title) {
		this.mId = id;
		this.mUrl = url;
		this.mTitle = title;
		this.mBitmap = null;
	}

	// constructor
	public HistoryItem(String url, String title) {
		this.mUrl = url;
		this.mTitle = title;
		this.mBitmap = null;
	}

	// constructor
	public HistoryItem(String url, String title, int imageId) {
		this.mUrl = url;
		this.mTitle = title;
		this.mBitmap = null;
		this.mImageId = imageId;
	}

	// getting ID
	public int getId() {
		return this.mId;
	}

	public int getImageId() {
		return this.mImageId;
	}

	// setting id
	public void setID(int id) {
		this.mId = id;
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

	@Override
	public String toString() {
		return mTitle;
	}

	@Override
	public int compareTo(HistoryItem another) {
		return mTitle.compareTo(another.mTitle);
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (o == null || ((Object) this).getClass() != o.getClass()) {
			return false;
		}

		HistoryItem that = (HistoryItem) o;

		if (mId != that.mId) {
			return false;
		}
		if (mImageId != that.mImageId) {
			return false;
		}
		if (mBitmap != null ? !mBitmap.equals(that.mBitmap) : that.mBitmap != null) {
			return false;
		}
		if (!mTitle.equals(that.mTitle)) {
			return false;
		}
		return mUrl.equals(that.mUrl);
	}

	@Override
	public int hashCode() {

		int result = mId;
		result = 31 * result + mUrl.hashCode();
		result = 31 * result + mTitle.hashCode();
		result = 31 * result + (mBitmap != null ? mBitmap.hashCode() : 0);
		result = 31 * result + mImageId;

		return result;
	}
}
