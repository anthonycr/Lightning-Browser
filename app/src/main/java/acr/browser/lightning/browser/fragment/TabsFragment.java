package acr.browser.lightning.browser.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.browser.TabsManager;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.browser.TabsView;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.browser.fragment.anim.HorizontalItemAnimator;
import acr.browser.lightning.browser.fragment.anim.VerticalItemAnimator;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.DrawableUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.view.BackgroundDrawable;
import acr.browser.lightning.view.LightningView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A fragment that holds and manages the tabs and interaction with the tabs.
 * It is reliant on the BrowserController in order to get the current UI state
 * of the browser. It also uses the BrowserController to signal that the UI needs
 * to change. This class contains the adapter used by both the drawer tabs and
 * the desktop tabs. It delegates touch events for the tab UI appropriately.
 */
public class TabsFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener, TabsView {

    @NonNull
    public static TabsFragment createTabsFragment(boolean isIncognito, boolean showTabsInDrawer) {
        TabsFragment tabsFragment = new TabsFragment();
        final Bundle tabsFragmentArguments = new Bundle();
        tabsFragmentArguments.putBoolean(TabsFragment.IS_INCOGNITO, isIncognito);
        tabsFragmentArguments.putBoolean(TabsFragment.VERTICAL_MODE, showTabsInDrawer);
        tabsFragment.setArguments(tabsFragmentArguments);

        return tabsFragment;
    }

    private static final String TAG = "TabsFragment";

    /**
     * Arguments boolean to tell the fragment it is displayed in the drawner or on the tab strip
     * If true, the fragment is in the left drawner in the strip otherwise.
     */
    private static final String VERTICAL_MODE = TAG + ".VERTICAL_MODE";
    private static final String IS_INCOGNITO = TAG + ".IS_INCOGNITO";

    private boolean mIsIncognito, mDarkTheme;
    private int mIconColor;
    private boolean mColorMode = true;
    private boolean mShowInNavigationDrawer;

    @Nullable private LightningViewAdapter mTabsAdapter;
    private UIController mUiController;

    @BindView(R.id.tabs_list) RecyclerView mRecyclerView;
    private Unbinder mUnbinder;

    private TabsManager mTabsManager;
    @Inject PreferenceManager mPreferences;

    public TabsFragment() {
        BrowserApp.getAppComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        final Context context = getContext();
        mUiController = (UIController) getActivity();
        mTabsManager = mUiController.getTabModel();
        mIsIncognito = arguments.getBoolean(IS_INCOGNITO, false);
        mShowInNavigationDrawer = arguments.getBoolean(VERTICAL_MODE, true);
        mDarkTheme = mPreferences.getUseTheme() != 0 || mIsIncognito;
        mColorMode = mPreferences.getColorModeEnabled();
        mColorMode &= !mDarkTheme;
        mIconColor = mDarkTheme ?
            ThemeUtils.getIconDarkThemeColor(context) :
            ThemeUtils.getIconLightThemeColor(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view;
        final LayoutManager layoutManager;
        if (mShowInNavigationDrawer) {
            view = inflater.inflate(R.layout.tab_drawer, container, false);
            layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            setupFrameLayoutButton(view, R.id.tab_header_button, R.id.plusIcon);
            setupFrameLayoutButton(view, R.id.new_tab_button, R.id.icon_plus);
            setupFrameLayoutButton(view, R.id.action_back, R.id.icon_back);
            setupFrameLayoutButton(view, R.id.action_forward, R.id.icon_forward);
            setupFrameLayoutButton(view, R.id.action_home, R.id.icon_home);
        } else {
            view = inflater.inflate(R.layout.tab_strip, container, false);
            layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            ImageView newTab = view.findViewById(R.id.new_tab_button);
            newTab.setColorFilter(ThemeUtils.getIconDarkThemeColor(getActivity()));
            newTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mUiController.newTabButtonClicked();
                }
            });
        }

        mUnbinder = ButterKnife.bind(this, view);

        SimpleItemAnimator animator;
        if (mShowInNavigationDrawer) {
            animator = new VerticalItemAnimator();
        } else {
            animator = new HorizontalItemAnimator();
        }
        animator.setSupportsChangeAnimations(false);
        animator.setAddDuration(200);
        animator.setChangeDuration(0);
        animator.setRemoveDuration(200);
        animator.setMoveDuration(200);
        mRecyclerView.setLayerType(View.LAYER_TYPE_NONE, null);
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.setLayoutManager(layoutManager);
        mTabsAdapter = new LightningViewAdapter(mShowInNavigationDrawer);
        mRecyclerView.setAdapter(mTabsAdapter);
        mRecyclerView.setHasFixedSize(true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mUnbinder != null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
        mTabsAdapter = null;
    }

    private TabsManager getTabsManager() {
        if (mTabsManager == null) {
            mTabsManager = mUiController.getTabModel();
        }
        return mTabsManager;
    }

    private void setupFrameLayoutButton(@NonNull final View root, @IdRes final int buttonId,
                                        @IdRes final int imageId) {
        final View frameButton = root.findViewById(buttonId);
        final ImageView buttonImage = root.findViewById(imageId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Force adapter refresh
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void tabsInitialized() {
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyDataSetChanged();
        }
    }

    public void reinitializePreferences() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mDarkTheme = mPreferences.getUseTheme() != 0 || mIsIncognito;
        mColorMode = mPreferences.getColorModeEnabled();
        mColorMode &= !mDarkTheme;
        mIconColor = mDarkTheme ?
            ThemeUtils.getIconDarkThemeColor(activity) :
            ThemeUtils.getIconLightThemeColor(activity);
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()) {
            case R.id.tab_header_button:
                mUiController.showCloseDialog(getTabsManager().indexOfCurrentTab());
                break;
            case R.id.new_tab_button:
                mUiController.newTabButtonClicked();
                break;
            case R.id.action_back:
                mUiController.onBackButtonPressed();
                break;
            case R.id.action_forward:
                mUiController.onForwardButtonPressed();
                break;
            case R.id.action_home:
                mUiController.onHomeButtonPressed();
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(@NonNull View v) {
        switch (v.getId()) {
            case R.id.action_new_tab:
                mUiController.newTabButtonLongClicked();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void tabAdded() {
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyItemInserted(getTabsManager().last());
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.smoothScrollToPosition(mTabsAdapter.getItemCount() - 1);
                }
            }, 500);
        }
    }

    @Override
    public void tabRemoved(int position) {
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyItemRemoved(position);
        }
    }

    @Override
    public void tabChanged(int position) {
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyItemChanged(position);
        }
    }

    private class LightningViewAdapter extends RecyclerView.Adapter<LightningViewAdapter.LightningViewHolder> {

        private final int mLayoutResourceId;
        @Nullable private final Drawable mBackgroundTabDrawable;
        @Nullable private final Bitmap mForegroundTabBitmap;
        private ColorMatrix mColorMatrix;
        private Paint mPaint;
        private ColorFilter mFilter;
        private static final float DESATURATED = 0.5f;

        private final boolean mDrawerTabs;

        public LightningViewAdapter(final boolean vertical) {
            this.mLayoutResourceId = vertical ? R.layout.tab_list_item : R.layout.tab_list_item_horizontal;
            this.mDrawerTabs = vertical;

            if (vertical) {
                mBackgroundTabDrawable = null;
                mForegroundTabBitmap = null;
            } else {
                int backgroundColor = Utils.mixTwoColors(ThemeUtils.getPrimaryColor(getContext()), Color.BLACK, 0.75f);
                Bitmap backgroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
                Utils.drawTrapezoid(new Canvas(backgroundTabBitmap), backgroundColor, true);
                mBackgroundTabDrawable = new BitmapDrawable(getResources(), backgroundTabBitmap);

                int foregroundColor = ThemeUtils.getPrimaryColor(getContext());
                mForegroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
                Utils.drawTrapezoid(new Canvas(mForegroundTabBitmap), foregroundColor, false);
            }
        }

        @NonNull
        @Override
        public LightningViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View view = inflater.inflate(mLayoutResourceId, viewGroup, false);
            if (mDrawerTabs) {
                DrawableUtils.setBackground(view, new BackgroundDrawable(view.getContext()));
            }
            return new LightningViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final LightningViewHolder holder, int position) {
            holder.exitButton.setTag(position);

            ViewCompat.jumpDrawablesToCurrentState(holder.exitButton);

            LightningView web = getTabsManager().getTabAtPosition(position);
            if (web == null) {
                return;
            }
            holder.txtTitle.setText(web.getTitle());

            final Bitmap favicon = web.getFavicon();
            if (web.isForegroundTab()) {
                Drawable foregroundDrawable = null;
                if (!mDrawerTabs) {
                    foregroundDrawable = new BitmapDrawable(getResources(), mForegroundTabBitmap);
                    if (!mIsIncognito && mColorMode) {
                        foregroundDrawable.setColorFilter(mUiController.getUiColor(), PorterDuff.Mode.SRC_IN);
                    }
                }
                if (!mIsIncognito && mColorMode) {
                    mUiController.changeToolbarBackground(favicon, foregroundDrawable);
                }

                TextViewCompat.setTextAppearance(holder.txtTitle, R.style.boldText);
                if (!mDrawerTabs) {
                    DrawableUtils.setBackground(holder.layout, foregroundDrawable);
                }
                holder.favicon.setImageBitmap(favicon);
            } else {
                TextViewCompat.setTextAppearance(holder.txtTitle, R.style.normalText);
                if (!mDrawerTabs) {
                    DrawableUtils.setBackground(holder.layout, mBackgroundTabDrawable);
                }
                holder.favicon.setImageBitmap(getDesaturatedBitmap(favicon));
            }

            if (mDrawerTabs) {
                BackgroundDrawable verticalBackground = (BackgroundDrawable) holder.layout.getBackground();
                verticalBackground.setCrossFadeEnabled(false);
                if (web.isForegroundTab()) {
                    verticalBackground.startTransition(200);
                } else {
                    verticalBackground.reverseTransition(200);
                }
            }
        }

        @Override
        public int getItemCount() {
            return getTabsManager().size();
        }

        public Bitmap getDesaturatedBitmap(@NonNull Bitmap favicon) {
            Bitmap grayscaleBitmap = Bitmap.createBitmap(favicon.getWidth(),
                favicon.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(grayscaleBitmap);
            if (mColorMatrix == null || mFilter == null || mPaint == null) {
                mPaint = new Paint();
                mColorMatrix = new ColorMatrix();
                mColorMatrix.setSaturation(DESATURATED);
                mFilter = new ColorMatrixColorFilter(mColorMatrix);
                mPaint.setColorFilter(mFilter);
            }

            c.drawBitmap(favicon, 0, 0, mPaint);
            return grayscaleBitmap;
        }

        public class LightningViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

            public LightningViewHolder(@NonNull View view) {
                super(view);
                txtTitle = view.findViewById(R.id.textTab);
                favicon = view.findViewById(R.id.faviconTab);
                exit = view.findViewById(R.id.deleteButton);
                layout = view.findViewById(R.id.tab_item_background);
                exitButton = view.findViewById(R.id.deleteAction);
                exit.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);

                exitButton.setOnClickListener(this);
                layout.setOnClickListener(this);
                layout.setOnLongClickListener(this);
            }

            @NonNull final TextView txtTitle;
            @NonNull final ImageView favicon;
            @NonNull final ImageView exit;
            @NonNull final FrameLayout exitButton;
            @NonNull final LinearLayout layout;

            @Override
            public void onClick(View v) {
                if (v == exitButton) {
                    mUiController.tabCloseClicked(getAdapterPosition());
                }
                if (v == layout) {
                    mUiController.tabClicked(getAdapterPosition());
                }
            }

            @Override
            public boolean onLongClick(View v) {
                mUiController.showCloseDialog(getAdapterPosition());
                return true;
            }
        }
    }
}
