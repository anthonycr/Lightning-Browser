package acr.browser.lightning.fragment;

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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.BrowserActivity;
import acr.browser.lightning.activity.TabsManager;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.bus.NavigationEvents;
import acr.browser.lightning.bus.TabEvents;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.view.LightningView;

/**
 * A fragment that holds and manages the tabs and interaction with the tabs.
 * It is reliant on the BrowserController in order to get the current UI state
 * of the browser. It also uses the BrowserController to signal that the UI needs
 * to change. This class contains the adapter used by both the drawer tabs and
 * the desktop tabs. It delegates touch events for the tab UI appropriately.
 */
public class TabsFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = TabsFragment.class.getSimpleName();

    /**
     * Arguments boolean to tell the fragment it is displayed in the drawner or on the tab strip
     * If true, the fragment is in the left drawner in the strip otherwise.
     */
    public static final String VERTICAL_MODE = TAG + ".VERTICAL_MODE";
    public static final String IS_INCOGNITO = TAG + ".IS_INCOGNITO";

    private boolean mIsIncognito, mDarkTheme;
    private int mIconColor;
    private boolean mColorMode = true;
    private boolean mShowInNavigationDrawer;

    private RecyclerView mRecyclerView;
    private LightningViewAdapter mTabsAdapter;

    @Inject
    TabsManager tabsManager;

    @Inject
    Bus mBus;

    @Inject
    PreferenceManager mPreferences;

    public TabsFragment() {
        BrowserApp.getAppComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        final Context context = getContext();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        }
        mRecyclerView = (RecyclerView) view.findViewById(R.id.tabs_list);
        mRecyclerView.setLayoutManager(layoutManager);
        mTabsAdapter = new LightningViewAdapter(mShowInNavigationDrawer);
        mRecyclerView.setAdapter(mTabsAdapter);
        mRecyclerView.setHasFixedSize(true);
        return view;
    }

    private void setupFrameLayoutButton(@NonNull final View root, @IdRes final int buttonId,
                                        @IdRes final int imageId) {
        final View frameButton = root.findViewById(buttonId);
        final ImageView buttonImage = (ImageView) root.findViewById(imageId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        mTabsAdapter = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mBus.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Force adapter refresh
        mTabsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        mBus.unregister(this);
    }

    @Subscribe
    public void tabsChanged(final BrowserEvents.TabsChanged event) {
        if (mTabsAdapter != null) {
            mTabsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_tab_button:
                mBus.post(new TabEvents.NewTab());
                break;
            case R.id.action_back:
                mBus.post(new NavigationEvents.GoBack());
                break;
            case R.id.action_forward:
                mBus.post(new NavigationEvents.GoForward());
                break;
            case R.id.action_home:
                mBus.post(new NavigationEvents.GoHome());
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.action_new_tab:
                mBus.post(new TabEvents.NewTabLongPress());
                break;
            default:
                break;
        }
        return true;
    }

    public class LightningViewAdapter extends RecyclerView.Adapter<LightningViewAdapter.LightningViewHolder> {

        private final int mLayoutResourceId;
        private final Drawable mBackgroundTabDrawable;
        private final Drawable mForegroundTabDrawable;
        private final Bitmap mForegroundTabBitmap;
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
                mForegroundTabDrawable = ThemeUtils.getSelectedBackground(getContext(), mDarkTheme);
            } else {
                int backgroundColor = Utils.mixTwoColors(ThemeUtils.getPrimaryColor(getContext()), Color.BLACK, 0.75f);
                Bitmap backgroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
                Utils.drawTrapezoid(new Canvas(backgroundTabBitmap), backgroundColor, true);
                mBackgroundTabDrawable = new BitmapDrawable(getResources(), backgroundTabBitmap);

                int foregroundColor = ThemeUtils.getPrimaryColor(getContext());
                mForegroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
                Utils.drawTrapezoid(new Canvas(mForegroundTabBitmap), foregroundColor, false);
                mForegroundTabDrawable = null;
            }
        }

        @Override
        public LightningViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View view = inflater.inflate(mLayoutResourceId, viewGroup, false);
            return new LightningViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final LightningViewHolder holder, int position) {
            holder.exitButton.setTag(position);

            ViewCompat.jumpDrawablesToCurrentState(holder.exitButton);

            LightningView web = tabsManager.getTabAtPosition(position);
            if (web == null) {
                return;
            }
            holder.txtTitle.setText(web.getTitle());

            final Bitmap favicon = web.getFavicon();
            if (web.isForegroundTab()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.txtTitle.setTextAppearance(R.style.boldText);
                } else {
                    holder.txtTitle.setTextAppearance(getContext(), R.style.boldText);
                }
                Drawable foregroundDrawable;
                if (!mDrawerTabs) {
                    foregroundDrawable = new BitmapDrawable(getResources(), mForegroundTabBitmap);
                    if (!mIsIncognito && mColorMode) {
                        foregroundDrawable.setColorFilter(((BrowserActivity)getActivity()).getUiColor(),
                                PorterDuff.Mode.SRC_IN);
                    }
                } else {
                    foregroundDrawable = mForegroundTabDrawable;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.layout.setBackground(foregroundDrawable);
                } else {
                    holder.layout.setBackgroundDrawable(foregroundDrawable);
                }
                if (!mIsIncognito && mColorMode) {
                    ((BrowserActivity)getActivity()).changeToolbarBackground(favicon, foregroundDrawable);
                }
                holder.favicon.setImageBitmap(favicon);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.txtTitle.setTextAppearance(R.style.normalText);
                } else {
                    holder.txtTitle.setTextAppearance(getContext(), R.style.normalText);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.layout.setBackground(mBackgroundTabDrawable);
                } else {
                    holder.layout.setBackgroundDrawable(mBackgroundTabDrawable);
                }
                holder.favicon.setImageBitmap(getDesaturatedBitmap(favicon));
            }
        }

        @Override
        public int getItemCount() {
            return tabsManager.size();
        }

        public Bitmap getDesaturatedBitmap(Bitmap favicon) {
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

            public LightningViewHolder(View view) {
                super(view);
                txtTitle = (TextView) view.findViewById(R.id.textTab);
                favicon = (ImageView) view.findViewById(R.id.faviconTab);
                exit = (ImageView) view.findViewById(R.id.deleteButton);
                layout = (LinearLayout) view.findViewById(R.id.tab_item_background);
                exitButton = (FrameLayout) view.findViewById(R.id.deleteAction);
                exit.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);

                exitButton.setOnClickListener(this);
                layout.setOnClickListener(this);
                layout.setOnLongClickListener(this);
            }

            final TextView txtTitle;
            final ImageView favicon;
            final ImageView exit;
            final FrameLayout exitButton;
            final LinearLayout layout;

            @Override
            public void onClick(View v) {
                if (v == exitButton) {
                    // Close tab
                    mBus.post(new TabEvents.CloseTab(getAdapterPosition()));
                }
                if (v == layout) {
                    mBus.post(new TabEvents.ShowTab(getAdapterPosition()));
                }
            }

            @Override
            public boolean onLongClick(View v) {
                // Show close dialog
                mBus.post(new TabEvents.ShowCloseDialog(getAdapterPosition()));
                return true;
            }
        }
    }
}
