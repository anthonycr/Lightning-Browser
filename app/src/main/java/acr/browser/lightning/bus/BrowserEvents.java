package acr.browser.lightning.bus;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

public final class BrowserEvents {

    private BrowserEvents() {
        // No instances
    }

    /**
     * The {@link acr.browser.lightning.activity.BrowserActivity} signal a new bookmark was added
     * (mainly to the {@link acr.browser.lightning.fragment.BookmarksFragment}).
     */
    public static class BookmarkAdded {
        public final String title, url;

        public BookmarkAdded(final String title, final String url) {
            this.title = title;
            this.url = url;
        }
    }

    /**
     * Notify the current page has a new url. This is generally used to update the
     * {@link acr.browser.lightning.fragment.BookmarksFragment} interface.
     */
    public static class CurrentPageUrl {
        public final String url;

        public CurrentPageUrl(final String url) {
            this.url = url;
        }
    }

    /**
     * Notify the BookmarksFragment and TabsFragment that the user pressed the back button
     */
    public static class UserPressedBack {
    }

    /**
     *
     */

    /**
     * Notify the Browser to display a SnackBar in the main activity
     */
    public static class ShowSnackBarMessage {
        @Nullable public final String message;
        @StringRes
        public final int stringRes;

        public ShowSnackBarMessage(@Nullable final String message) {
            this.message = message;
            this.stringRes = -1;
        }

        public ShowSnackBarMessage(@StringRes final int stringRes) {
            this.message = null;
            this.stringRes = stringRes;
        }
    }

    public final static class OpenHistoryInCurrentTab {
    }

    /**
     * The user want to open the given url in the current tab
     */
    public final static class OpenUrlInCurrentTab {
        public final String url;

        public OpenUrlInCurrentTab(final String url) {
            this.url = url;
        }
    }

    /**
     * The user ask to open the given url as new tab
     */
    public final static class OpenUrlInNewTab {
        public final String url;

        public OpenUrlInNewTab(final String url) {
            this.url = url;
        }
    }
}
