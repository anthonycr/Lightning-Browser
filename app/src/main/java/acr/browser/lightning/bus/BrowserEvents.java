package acr.browser.lightning.bus;

import android.net.Uri;
import android.os.Message;
import android.support.annotation.StringRes;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import acr.browser.lightning.view.LightningView;

/**
 * Created by Stefano Pacifici on 26/08/15.
 */
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
     * Notify that the user closed or opened a tab
     */
    public static class TabsChanged {
    }

    /**
     *
     */

    /**
     * Notify the Browser to display a SnackBar in the main activity
     */
    public static class ShowSnackBarMessage {
        public final String message;
        @StringRes
        public final int stringRes;

        public ShowSnackBarMessage(final String message) {
            this.message = message;
            this.stringRes = -1;
        }

        public ShowSnackBarMessage(@StringRes final int stringRes) {
            this.message = null;
            this.stringRes = stringRes;
        }
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

    /**
     * Notify the browser to show the Action Bar
     */
    public static class ShowActionBar {
    }

    /**
     * Notify the browser to hide the Action Bar
     */
    public static class HideActionBar {
    }

    /**
     * Notify the browser to update the URL in the URL bar
     */
    public static class UpdateUrl {
        public final String url;
        public final Boolean isShortUrl;

        public UpdateUrl(final String url, final Boolean isShortUrl) {
            this.url = url;
            this.isShortUrl = isShortUrl;
        }

    }

    /**
     * Update the current progress of loading a page.
     */
    public static class UpdateProgress {
        public final int progress;

        public UpdateProgress(final int progress) {
            this.progress = progress;
        }
    }

    /**
     * Request the browser to create a new window
     */
    public static class CreateWindow {
        public final Message resultMsg;

        public CreateWindow(final Message resultMsg) {
            this.resultMsg = resultMsg;
        }
    }

    /**
     * Request the browser to close the given WebView and remove it
     * from the view system
     */
    public static class CloseWindow {
        public final LightningView lightningView;

        public CloseWindow(LightningView lightningView) {
            this.lightningView = lightningView;
        }
    }

    /**
     * Tell the browser to open a file chooser.
     */
    public static class OpenFileChooser {
        public final ValueCallback<Uri> uploadMsg;

        public OpenFileChooser(ValueCallback<Uri> uploadMsg) {
            this.uploadMsg = uploadMsg;
        }
    }

    /**
     * Tell the browser to show a file chooser.
     *
     * This is called to handle HTML forms with 'file' input type, in response to the
     * user pressing the "Select File" button.
     */
    public static class ShowFileChooser {
        public final ValueCallback<Uri[]> filePathCallBack;

        public ShowFileChooser(ValueCallback<Uri[]> filePathCallBack) {
            this.filePathCallBack = filePathCallBack;
        }
    }

    /**
     * Notify the browser that the current page has exited full
     * screen mode and to hide the custom View
     */
    public static class HideCustomView {
    }

    /**
     * Notify the browser that the current page has entered full screen mode. The browser must show
     * the custom View which contains the web contents: video or other HTML content
     * in full screen mode or in a particular orientation.
    */
    public static class ShowCustomView {
        public final View view;
        public final WebChromeClient.CustomViewCallback callback;
        public final Integer requestedOrientation;

        public ShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
            this.view = view;
            this.callback = callback;
            this.requestedOrientation = null;
        }

        public ShowCustomView(View view, WebChromeClient.CustomViewCallback callback,
                              int requestedOrientation) {
            this.view = view;
            this.callback = callback;
            this.requestedOrientation = requestedOrientation;
        }
    }
}
