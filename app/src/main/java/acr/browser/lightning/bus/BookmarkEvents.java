package acr.browser.lightning.bus;

import acr.browser.lightning.database.HistoryItem;

public final class BookmarkEvents {

    private BookmarkEvents() {
        // No instances
    }

    /**
     * The user ask to delete the selected bookmark
     */
    public static class Deleted {
        public final HistoryItem item;

        public Deleted(final HistoryItem item) {
            this.item = item;
        }
    }

    /**
     * Sended when a bookmark is edited
     */
    public static class BookmarkChanged {

        public BookmarkChanged() {
        }
    }
}
