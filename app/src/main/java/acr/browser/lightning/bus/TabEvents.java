package acr.browser.lightning.bus;

/**
 * A collection of events been sent by {@link acr.browser.lightning.fragment.TabsFragment}
 *
 * @author Stefano Pacifici
 * @date 2015/09/14
 */
public final class TabEvents {

    private TabEvents() {
        // No instances
    }


    /**
     * Sended by {@link acr.browser.lightning.fragment.TabsFragment} when the user click on the
     * tab exit button
     */
    public static class CloseTab {
        public final int position;

        public CloseTab(int position) {
            this.position = position;
        }
    }

    /**
     * Sended by {@link acr.browser.lightning.fragment.TabsFragment} when the user click on the
     * tab itself.
     */
    public static class ShowTab {
        public final int position;

        public ShowTab(int position) {
            this.position = position;
        }
    }

    /**
     * Sended by {@link acr.browser.lightning.fragment.TabsFragment} when the user long presses on
     * new tab button.
     */
    public static class NewTabLongPress {
    }
}
