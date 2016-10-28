package acr.browser.lightning.bus;

public final class BrowserEvents {

    private BrowserEvents() {
        // No instances
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
