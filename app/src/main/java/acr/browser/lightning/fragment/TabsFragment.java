package acr.browser.lightning.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
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
 * @author Stefano Pacifici based on Anthony C. Restaino's code
 * @date 2015/09/14
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
    private int mCurrentUiColor = Color.BLACK; // TODO Only temporary

    private RecyclerView mRecyclerView;
    private LightningViewAdapter mTabsAdapter;

    @Inject
    TabsManager tabsManager;

    @Inject
    Bus bus;

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
        final Bundle arguments = getArguments();
        final View view;
        final LayoutManager layoutManager;
        if (mShowInNavigationDrawer) {
            view = inflater.inflate(R.layout.tab_drawer, container, false);
            layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            // TODO Handle also long press
            setupFrameLayoutButton(view, R.id.new_tab_button, R.id.icon_plus);
            setupFrameLayoutButton(view, R.id.action_back, R.id.icon_back);
            setupFrameLayoutButton(view, R.id.action_forward, R.id.icon_forward);

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
        bus.register(this);
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
        bus.unregister(this);
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
                bus.post(new TabEvents.NewTab());
                break;
            case R.id.action_back:
                bus.post(new NavigationEvents.GoBack());
                break;
            case R.id.action_forward:
                bus.post(new NavigationEvents.GoForward());
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.action_new_tab:
                bus.post(new TabEvents.NewTabLongPress());
                break;
            default:
                break;
        }
        return true;
    }

    public class LightningViewAdapter extends RecyclerView.Adapter<LightningViewAdapter.LightningViewHolder> {

        private final int layoutResourceId;
        private final Drawable mBackgroundTabDrawable;
        private final Drawable mForegroundTabDrawable;
        private final Bitmap mForegroundTabBitmap;
        private ColorMatrix mColorMatrix;
        private Paint mPaint;
        private ColorFilter mFilter;
        private static final float DESATURATED = 0.5f;

        private final boolean vertical;

        public LightningViewAdapter(final boolean vertical) {
            this.layoutResourceId = vertical ? R.layout.tab_list_item : R.layout.tab_list_item_horizontal;
            this.vertical = vertical;

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
            View view = inflater.inflate(layoutResourceId, viewGroup, false);
            return new LightningViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final LightningViewHolder holder, int position) {
            holder.exitButton.setTag(position);

            ViewCompat.jumpDrawablesToCurrentState(holder.exitButton);

            LightningView web = tabsManager.getTabAtPosition(position);
            holder.txtTitle.setText(web.getTitle());

            final Bitmap favicon = web.getFavicon();
            if (web.isForegroundTab()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.txtTitle.setTextAppearance(R.style.boldText);
                } else {
                    holder.txtTitle.setTextAppearance(getContext(), R.style.boldText);
                }
                Drawable foregroundDrawable;
                if (!vertical) {
                    foregroundDrawable = new BitmapDrawable(getResources(), mForegroundTabBitmap);
                    if (!mIsIncognito && mColorMode) {
                        foregroundDrawable.setColorFilter(mCurrentUiColor, PorterDuff.Mode.SRC_IN);
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
                    changeToolbarBackground(favicon, foregroundDrawable);
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

        /**
         * Animates the color of the toolbar from one color to another. Optionally animates
         * the color of the tab background, for use when the tabs are displayed on the top
         * of the screen.
         *
         * @param favicon       the Bitmap to extract the color from
         * @param tabBackground the optional LinearLayout to color
         */
        private void changeToolbarBackground(@NonNull Bitmap favicon, @Nullable final Drawable tabBackground) {
            if (mShowInNavigationDrawer) {
                return;
            }

            final int defaultColor;
            final Resources resources = getResources();
            final ColorDrawable mBackground = new ColorDrawable();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defaultColor = resources.getColor(R.color.primary_color, null);
            } else {
                defaultColor = resources.getColor(R.color.primary_color);
            }
            if (mCurrentUiColor == Color.BLACK) {
                mCurrentUiColor = defaultColor;
            }
            Palette.from(favicon).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {

                    // OR with opaque black to remove transparency glitches
                    int color = 0xff000000 | palette.getVibrantColor(defaultColor);

                    int finalColor = Utils.mixTwoColors(defaultColor, color, 0.25f);

                    ValueAnimator anim = ValueAnimator.ofInt(mCurrentUiColor, finalColor);
                    anim.setEvaluator(new ArgbEvaluator());
                    // final Window window = getWindow();
                    // TODO Check this
                    // if (!mShowInNavigationDrawer) {
                    //     window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                    // }
                    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            final int color = (Integer) animation.getAnimatedValue();
                            if (tabBackground != null) {
                                tabBackground.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                            }
                            mCurrentUiColor = color;
                        }

                    });
                    anim.setDuration(300);
                    anim.start();
                }
            });
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
                    bus.post(new TabEvents.CloseTab(getAdapterPosition()));
                }
                if (v == layout) {
                    bus.post(new TabEvents.ShowTab(getAdapterPosition()));
                }
            }

            @Override
            public boolean onLongClick(View v) {
                // Show close dialog
                bus.post(new TabEvents.ShowCloseDialog(getAdapterPosition()));
                return true;
            }
        }
    }
}
