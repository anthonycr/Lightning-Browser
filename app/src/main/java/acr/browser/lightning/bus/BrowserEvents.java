package acr.browser.lightning.bus;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

public final class BrowserEvents {

    private BrowserEvents() {
        // No instances
    }

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
     * The user ask to open the given url as new tab
     */
    public final static class OpenUrlInNewTab {

        public enum Location {
            NEW_TAB,
            BACKGROUND,
            INCOGNITO
        }

        public final String url;

        public final Location location;

        public OpenUrlInNewTab(final String url) {
            this.url = url;
            this.location = Location.NEW_TAB;
        }

        public OpenUrlInNewTab(final String url, Location location) {
            this.url = url;
            this.location = location;
        }
    }
}
