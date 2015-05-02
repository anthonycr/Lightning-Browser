/*
 *  Copyright 2011 Peter Karich 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package acr.browser.lightning.reading;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Parsed result from web page containing important title, text and image.
 * 
 * @author Peter Karich
 */
public class JResult implements Serializable {

	private String title;
	private String url;
	private String originalUrl;
	private String canonicalUrl;
	private String imageUrl;
	private String videoUrl;
	private String rssUrl;
	private String text;
	private String faviconUrl;
	private String description;
	private String dateString;
	private List<String> textList;
	private Collection<String> keywords;
	private List<ImageResult> images = null;

	public JResult() {
	}

	public String getUrl() {
		if (url == null)
			return "";
		return url;
	}

	public JResult setUrl(String url) {
		this.url = url;
		return this;
	}

	public JResult setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
		return this;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public JResult setCanonicalUrl(String canonicalUrl) {
		this.canonicalUrl = canonicalUrl;
		return this;
	}

	public String getCanonicalUrl() {
		return canonicalUrl;
	}

	public String getFaviconUrl() {
		if (faviconUrl == null)
			return "";
		return faviconUrl;
	}

	public JResult setFaviconUrl(String faviconUrl) {
		this.faviconUrl = faviconUrl;
		return this;
	}

	public JResult setRssUrl(String rssUrl) {
		this.rssUrl = rssUrl;
		return this;
	}

	public String getRssUrl() {
		if (rssUrl == null)
			return "";
		return rssUrl;
	}

	public String getDescription() {
		if (description == null)
			return "";
		return description;
	}

	public JResult setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getImageUrl() {
		if (imageUrl == null)
			return "";
		return imageUrl;
	}

	public JResult setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
		return this;
	}

	public String getText() {
		if (text == null)
			return "";

		return text;
	}

	public JResult setText(String text) {
		this.text = text;
		return this;
	}

	public List<String> getTextList() {
		if (this.textList == null)
			return new ArrayList<String>();
		return this.textList;
	}

	public JResult setTextList(List<String> textList) {
		this.textList = textList;
		return this;
	}

	public String getTitle() {
		if (title == null)
			return "";
		return title;
	}

	public JResult setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getVideoUrl() {
		if (videoUrl == null)
			return "";
		return videoUrl;
	}

	public JResult setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
		return this;
	}

	public JResult setDate(String date) {
		this.dateString = date;
		return this;
	}

	public Collection<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Collection<String> keywords) {
		this.keywords = keywords;
	}

	/**
	 * @return get date from url or guessed from text
	 */
	public String getDate() {
		return dateString;
	}

	/**
	 * @return images list
	 */
	public List<ImageResult> getImages() {
		if (images == null)
			return Collections.emptyList();
		return images;
	}

	/**
	 * @return images count
	 */
	public int getImagesCount() {
		if (images == null)
			return 0;
		return images.size();
	}

	/**
	 * set images list
	 */
	public void setImages(List<ImageResult> images) {
		this.images = images;
	}

	@Override
	public String toString() {
		return "title:" + getTitle() + " imageUrl:" + getImageUrl() + " text:" + text;
	}
}
