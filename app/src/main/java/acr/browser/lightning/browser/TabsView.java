package acr.browser.lightning.browser;

/**
 * The interface for communicating to the tab list view.
 */
public interface TabsView {

    /**
     * Called when a tab has been added.
     */
    void tabAdded();

    /**
     * Called when a tab has been removed.
     *
     * @param position the position of the tab that has been removed.
     */
    void tabRemoved(int position);

    /**
     * Called when a tab's metadata has been changed.
     *
     * @param position the position of the tab that has been changed.
     */
    void tabChanged(int position);

    /**
     * Called when the tabs are completely initialized for the first time.
     */
    void tabsInitialized();
}
