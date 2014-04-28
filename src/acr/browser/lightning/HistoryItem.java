/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.graphics.Bitmap;

public class HistoryItem implements Comparable<HistoryItem> {

	// private variables
	private int mId;
	private String mUrl;
	private String mTitle;
	private Bitmap mBitmap;
	private int mImageId;

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
	
	public int getImageId(){
		return this.mImageId;
	}

	// setting id
	public void setID(int id) {
		this.mId = id;
	}
	
	public void setImageId(int id){
		this.mImageId = id;
	}

	public void setBitmap(Bitmap image) {
		mBitmap = image;
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
		this.mUrl = url;
	}

	// getting phone number
	public String getTitle() {
		return this.mTitle;
	}

	// setting phone number
	public void setTitle(String title) {
		this.mTitle = title;
	}

	@Override
	public String toString() {
		return mTitle;
	}

	@Override
	public int compareTo(HistoryItem another) {
		return mTitle.compareTo(another.mTitle);
	}

}