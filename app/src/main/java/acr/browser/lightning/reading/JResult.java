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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


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
    private String authorName;
    private String authorDescription;
    private Date date;
    private Collection<String> keywords;
    private List<ImageResult> images = null;
    private final List<Map<String, String>> links = new ArrayList<>();
    private String type;
    private String sitename;
    private String language;

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

    public String getAuthorName() {
        if (authorName == null)
            return "";
        return authorName;
    }

    public JResult setAuthorName(String authorName) {
        this.authorName = authorName;
        return this;
    }

    public String getAuthorDescription() {
        if (authorDescription == null)
            return "";
        return authorDescription;
    }

    public JResult setAuthorDescription(String authorDescription) {
        this.authorDescription = authorDescription;
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

    public JResult setDate(Date date) {
        this.date = date;
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
    public Date getDate() {
        return date;
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

    public void addLink(String url, String text, Integer pos) {
        Map<String, String> link = new HashMap<>();
        link.put("url", url);
        link.put("text", text);
        link.put("offset", String.valueOf(pos));
        links.add(link);
    }

    public List<Map<String, String>> getLinks() {
        if (links == null)
            return Collections.emptyList();
        return links;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSitename() {
        return sitename;
    }

    public void setSitename(String sitename) {
        this.sitename = sitename;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "title:" + getTitle() + " imageUrl:" + getImageUrl() + " text:" + text;
    }
}