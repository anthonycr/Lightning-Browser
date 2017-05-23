package acr.browser.lightning.bus;

/**
 * Collections of navigation events, like go back or go forward
 *
 * @author Stefano Pacifici
 * @date 2015/09/15
 */
public class NavigationEvents {
    private NavigationEvents() {
        // No instances please
    }

    /**
     * Fired by {@link acr.browser.lightning.fragment.TabsFragment} when the user presses back
     * button.
     */
    public static class GoBack {
    }

    /**
     * Fired by {@link acr.browser.lightning.fragment.TabsFragment} when the user presses forward
     * button.
     */
    public static class GoForward {
    }

    /**
     * Fired by {@link acr.browser.lightning.fragment.TabsFragment} when the user presses the home
     * button.
     */
    public static class GoHome {
    }
}
