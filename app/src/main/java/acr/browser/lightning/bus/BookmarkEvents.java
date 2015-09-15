package acr.browser.lightning.bus;

import acr.browser.lightning.database.HistoryItem;

/**
 * Created by Stefano Pacifici on 26/08/15.
 */
public final class BookmarkEvents {

    private BookmarkEvents() {
        // No instances
    }

    /**
     * A bookmark was clicked
     */
    public final static class Clicked {
        public final HistoryItem bookmark;

        public Clicked(final HistoryItem bookmark) {
            this.bookmark = bookmark;
        }
    }

    /**
     * The user ask to open the bookmark as new tab
     */
    public final static class AsNewTab {
        public final HistoryItem bookmark;

        public AsNewTab(final HistoryItem bookmark) {
            this.bookmark = bookmark;
        }
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
     * The user ask to bookmark the currently displayed page
     */
    public static class WantToBookmarkCurrentPage {
    }

    /**
     * The bookmark was added
     */
    public static class Added {
        public final HistoryItem item;

        public Added(final HistoryItem item) {
            this.item = item;
        }
    }

    /**
     * Sended by the {@link acr.browser.lightning.fragment.BookmarksFragment} when it wants to close
     * itself (generally in reply to a {@link acr.browser.lightning.bus.BrowserEvents.UserPressedBack}
     * event.
     */
    public static class CloseBookmarks {
    }

    /**
     * Sended when a bookmark is edited
     */
    public static class BookmarkChanged {

        public final HistoryItem oldBookmark;
        public final HistoryItem newBookmark;

        public BookmarkChanged(final HistoryItem oldItem, final HistoryItem newItem) {
            oldBookmark = oldItem;
            newBookmark = newItem;
        }
    }
}
