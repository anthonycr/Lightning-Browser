package acr.browser.lightning.browser;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;

import acr.browser.lightning.activity.TabsManager;
import acr.browser.lightning.activity.TabsManager.TabChangeListener;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.view.LightningView;

/**
 * Presenter in charge of keeping track of
 * the current tab and setting the current tab
 * of the
 */
public class BrowserPresenter {

    private static final String TAG = BrowserPresenter.class.getSimpleName();

    @Inject TabsManager mTabsModel;

    @NonNull private BrowserView mView;
    @Nullable private LightningView mCurrentTab;

    private static class TabListener implements TabChangeListener {

        private BrowserPresenter mPresenter;

        public TabListener(@NonNull BrowserPresenter presenter) {
            mPresenter = presenter;
        }

        @Override
        public void tabChanged(@Nullable LightningView newTab) {
            mPresenter.onTabChanged(newTab);
        }
    }

    public BrowserPresenter(@NonNull BrowserView view) {
        BrowserApp.getAppComponent().inject(this);
        mView = view;
        mTabsModel.setTabChangeListener(new TabListener(this));
    }

    private void onTabChanged(@Nullable LightningView newTab) {
        Log.d(TAG, "On tab changed");
        if (newTab == null) {
            mView.removeTabView();
            if (mCurrentTab != null) {
                mCurrentTab.pauseTimers();
                mCurrentTab.onDestroy();
            }
        } else {
            if (newTab.getWebView() == null) {
                mView.removeTabView();
                if (mCurrentTab != null) {
                    mCurrentTab.pauseTimers();
                    mCurrentTab.onDestroy();
                }
            } else {
                if (mCurrentTab != null) {
                    mCurrentTab.onPause();
                    mCurrentTab.setForegroundTab(false);
                }
                newTab.onResume();
                newTab.resumeTimers();
                newTab.setForegroundTab(true);
                mView.updateProgress(newTab.getProgress());
                mView.updateUrl(newTab.getUrl(), true);
                mView.setTabView(newTab.getWebView());
            }
        }

        mCurrentTab = newTab;
    }

    public void destroy() {
        mTabsModel.setTabChangeListener(null);
    }

}
