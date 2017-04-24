package acr.browser.lightning.model;

import java.util.List;

/**
 * Created by Maksim Makeychik on 21.04.2017.
 */

public class Article {

    public String sessionId;
    public List<Document> documents;

    public String getTitle() {
        return getDocument().title;
    }

    public String getText() {
        String author = "" + getDocument().author;
        if ("null".equalsIgnoreCase(author)){
            author = "";
        }
        return author + (getDocument().promoted ? (" " + getDocument().promotedText) : "");
    }

    public Document getDocument() {
        return documents.get(0);
    }

    public String getUrl() {
        return getDocument().clickUrl;
    }

    public String getImageUrl() {
        if(getDocument().thumbnails == null || getDocument().thumbnails.isEmpty()) {
            return "";
        }
        return getDocument().thumbnails.get(0);
    }

    public static class Document {
        public String id;
        public String title;
        public String description;
        public String clickUrl;
        public String visibleUrl;
        public String originalUrl;
        public String publishedTime;
        public String language;
        public boolean promoted;
        public String promotedText;
        public String author;
        public String country;
        public List<String> thumbnails;
        public List<String> categories;
    }
}
