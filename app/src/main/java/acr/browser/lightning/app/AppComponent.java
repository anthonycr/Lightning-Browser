package acr.browser.lightning.app;

import javax.inject.Singleton;

import acr.browser.lightning.activity.BrowserActivity;
import acr.browser.lightning.activity.ReadingActivity;
import acr.browser.lightning.activity.ThemableBrowserActivity;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.fragment.BookmarkSettingsFragment;
import acr.browser.lightning.fragment.BookmarksFragment;
import acr.browser.lightning.fragment.LightningPreferenceFragment;
import acr.browser.lightning.fragment.TabsFragment;
import acr.browser.lightning.object.SearchAdapter;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.view.LightningView;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(BrowserActivity activity);

    void inject(BookmarksFragment fragment);

    void inject(BookmarkSettingsFragment fragment);

    void inject(SearchAdapter adapter);

    void inject(LightningDialogBuilder builder);

    void inject(BookmarkPage bookmarkPage);

    void inject(TabsFragment fragment);

    void inject(LightningView lightningView);

    void inject(ThemableBrowserActivity activity);

    void inject(LightningPreferenceFragment fragment);

    void inject(BrowserApp app);

    void inject(ProxyUtils proxyUtils);

    void inject(ReadingActivity activity);

}
