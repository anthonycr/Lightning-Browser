package acr.browser.lightning.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.SingleSubscriber;
import com.anthonycr.bonsai.Subscription;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.ReadingActivity;
import acr.browser.lightning.activity.TabsManager;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.browser.BookmarksView;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.favicon.FaviconModel;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.view.LightningView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class BookmarksFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener, BookmarksView {

    @NonNull
    public static BookmarksFragment createFragment(boolean isIncognito) {
        BookmarksFragment bookmarksFragment = new BookmarksFragment();
        final Bundle bookmarksFragmentArguments = new Bundle();
        bookmarksFragmentArguments.putBoolean(BookmarksFragment.INCOGNITO_MODE, isIncognito);
        bookmarksFragment.setArguments(bookmarksFragmentArguments);

        return bookmarksFragment;
    }

    private static final String TAG = "BookmarksFragment";

    public final static String INCOGNITO_MODE = TAG + ".INCOGNITO_MODE";

    // Managers
    @Inject BookmarkManager mBookmarkManager;

    // Dialog builder
    @Inject LightningDialogBuilder mBookmarksDialogBuilder;

    @Inject PreferenceManager mPreferenceManager;

    @Inject FaviconModel mFaviconModel;

    private TabsManager mTabsManager;

    private UIController mUiController;

    // Adapter
    private BookmarkListAdapter mBookmarkAdapter;

    // Preloaded images
    private Bitmap mWebpageBitmap, mFolderBitmap;

    // Bookmarks
    private final List<HistoryItem> mBookmarks = new ArrayList<>();

    // Views
    @BindView(R.id.right_drawer_list) RecyclerView mBookmarksListView;
    @BindView(R.id.starIcon) ImageView mBookmarkTitleImage;
    @BindView(R.id.icon_star) ImageView mBookmarkImage;

    @Nullable
    private Unbinder mUnbinder;

    // Colors
    private int mIconColor, mScrollIndex;

    private boolean mIsIncognito;

    @Nullable
    private Subscription mBookmarksSubscription;

    private static Single<List<HistoryItem>> initBookmarks(@NonNull final BookmarkManager bookmarkManager) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                subscriber.onItem(bookmarkManager.getBookmarksFromFolder(null, true));
                subscriber.onComplete();

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        final Bundle arguments = getArguments();
        final Context context = getContext();
        mUiController = (UIController) context;
        mTabsManager = mUiController.getTabModel();
        mIsIncognito = arguments.getBoolean(INCOGNITO_MODE, false);
        boolean darkTheme = mPreferenceManager.getUseTheme() != 0 || mIsIncognito;
        mWebpageBitmap = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, darkTheme);
        mFolderBitmap = ThemeUtils.getThemedBitmap(context, R.drawable.ic_folder, darkTheme);
        mIconColor = darkTheme ? ThemeUtils.getIconDarkThemeColor(context) :
            ThemeUtils.getIconLightThemeColor(context);
    }

    private TabsManager getTabsManager() {
        if (mTabsManager == null) {
            mTabsManager = mUiController.getTabModel();
        }
        return mTabsManager;
    }

    // Handle bookmark click
    private final OnItemClickListener mItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(int position) {
            final HistoryItem item = mBookmarks.get(position);
            if (item.isFolder()) {
                mScrollIndex = ((LinearLayoutManager) mBookmarksListView.getLayoutManager()).findFirstVisibleItemPosition();
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(item.getTitle(), true), true);
            } else {
                mUiController.bookmarkItemClicked(item);
            }
        }
    };

    private final OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(int position) {
            final HistoryItem item = mBookmarks.get(position);
            handleLongPress(item);
            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (mBookmarkAdapter != null) {
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.bookmark_drawer, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mBookmarkTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        final View backView = view.findViewById(R.id.bookmark_back_button);
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBookmarkManager == null) return;
                if (!mBookmarkManager.isRootFolder()) {
                    setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
                    mBookmarksListView.getLayoutManager().scrollToPosition(mScrollIndex);
                }
            }
        });
        setupNavigationButton(view, R.id.action_add_bookmark, R.id.icon_star);
        setupNavigationButton(view, R.id.action_reading, R.id.icon_reading);
        setupNavigationButton(view, R.id.action_toggle_desktop, R.id.icon_desktop);

        mBookmarkAdapter = new BookmarkListAdapter(mBookmarks, mFaviconModel, mFolderBitmap, mWebpageBitmap);
        mBookmarkAdapter.setOnItemClickListener(mItemClickListener);
        mBookmarkAdapter.setOnItemLongClickListener(mItemLongClickListener);
        mBookmarksListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBookmarksListView.setAdapter(mBookmarkAdapter);

        mBookmarksSubscription = initBookmarks(mBookmarkManager)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                @Override
                public void onItem(@Nullable List<HistoryItem> item) {
                    mBookmarksSubscription = null;
                    Preconditions.checkNonNull(item);
                    setBookmarkDataSet(item, true);
                }
            });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBookmarksSubscription != null) {
            mBookmarksSubscription.unsubscribe();
            mBookmarksSubscription = null;
        }
        if (mUnbinder != null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBookmarksSubscription != null) {
            mBookmarksSubscription.unsubscribe();
            mBookmarksSubscription = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void reinitializePreferences() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        boolean darkTheme = mPreferenceManager.getUseTheme() != 0 || mIsIncognito;
        mWebpageBitmap = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_webpage, darkTheme);
        mFolderBitmap = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_folder, darkTheme);
        mIconColor = darkTheme ? ThemeUtils.getIconDarkThemeColor(activity) :
            ThemeUtils.getIconLightThemeColor(activity);
    }

    private void updateBookmarkIndicator(final String url) {
        if (!mBookmarkManager.isBookmark(url)) {
            mBookmarkImage.setImageResource(R.drawable.ic_action_star);
            mBookmarkImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        } else {
            mBookmarkImage.setImageResource(R.drawable.ic_bookmark);
            mBookmarkImage.setColorFilter(ThemeUtils.getAccentColor(getContext()), PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void handleBookmarkDeleted(@NonNull HistoryItem item) {
        mBookmarks.remove(item);
        if (item.isFolder()) {
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
        } else {
            mBookmarkAdapter.notifyDataSetChanged();
        }
    }

    private void setBookmarkDataSet(@NonNull List<HistoryItem> items, boolean animate) {
        mBookmarks.clear();
        mBookmarks.addAll(items);
        mBookmarkAdapter.notifyDataSetChanged();
        final int resource;
        if (mBookmarkManager.isRootFolder()) {
            resource = R.drawable.ic_action_star;
        } else {
            resource = R.drawable.ic_action_back;
        }

        final Animation startRotation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mBookmarkTitleImage.setRotationY(90 * interpolatedTime);
            }
        };
        final Animation finishRotation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mBookmarkTitleImage.setRotationY((-90) + (90 * interpolatedTime));
            }
        };
        startRotation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBookmarkTitleImage.setImageResource(resource);
                mBookmarkTitleImage.startAnimation(finishRotation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startRotation.setInterpolator(new AccelerateInterpolator());
        finishRotation.setInterpolator(new DecelerateInterpolator());
        startRotation.setDuration(250);
        finishRotation.setDuration(250);

        if (animate) {
            mBookmarkTitleImage.startAnimation(startRotation);
        } else {
            mBookmarkTitleImage.setImageResource(resource);
        }
    }

    private void setupNavigationButton(@NonNull View view, @IdRes int buttonId, @IdRes int imageId) {
        FrameLayout frameButton = (FrameLayout) view.findViewById(buttonId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        ImageView buttonImage = (ImageView) view.findViewById(imageId);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    private void handleLongPress(@NonNull final HistoryItem item) {
        if (item.isFolder()) {
            mBookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(getActivity(), mUiController, item);
        } else {
            mBookmarksDialogBuilder.showLongPressedDialogForBookmarkUrl(getActivity(), mUiController, item);
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()) {
            case R.id.action_add_bookmark:
                mUiController.bookmarkButtonClicked();
                break;
            case R.id.action_reading:
                LightningView currentTab = getTabsManager().getCurrentTab();
                if (currentTab != null) {
                    Intent read = new Intent(getActivity(), ReadingActivity.class);
                    read.putExtra(Constants.LOAD_READING_URL, currentTab.getUrl());
                    startActivity(read);
                }
                break;
            case R.id.action_toggle_desktop:
                LightningView current = getTabsManager().getCurrentTab();
                if (current != null) {
                    current.toggleDesktopUA(getActivity());
                    current.reload();
                    // TODO add back drawer closing
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public void navigateBack() {
        if (mBookmarkManager.isRootFolder()) {
            mUiController.closeBookmarksDrawer();
        } else {
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
            mBookmarksListView.getLayoutManager().scrollToPosition(mScrollIndex);
        }
    }

    @Override
    public void handleUpdatedUrl(@NonNull String url) {
        updateBookmarkIndicator(url);
        String folder = mBookmarkManager.getCurrentFolder();
        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(folder, true), false);
    }

    static class BookmarkViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        @BindView(R.id.textBookmark) TextView txtTitle;
        @BindView(R.id.faviconBookmark) ImageView favicon;

        @Nullable private final OnItemLongClickListener onItemLongClickListener;
        @Nullable private final OnItemClickListener onItemClickListener;

        BookmarkViewHolder(@NonNull View itemView,
                           @Nullable OnItemLongClickListener onItemLongClickListener,
                           @Nullable OnItemClickListener onItemClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            this.onItemClickListener = onItemClickListener;
            this.onItemLongClickListener = onItemLongClickListener;

            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            return onItemLongClickListener != null &&
                onItemLongClickListener.onItemLongClick(getAdapterPosition());
        }
    }

    interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    interface OnItemClickListener {
        void onItemClick(int position);
    }

    private static class BookmarkListAdapter extends RecyclerView.Adapter<BookmarkViewHolder> {

        @NonNull private final List<HistoryItem> mBookmarks;
        @NonNull private final FaviconModel mFaviconModel;
        @NonNull private final Bitmap mFolderBitmap;
        @NonNull private final Bitmap mWebpageBitmap;

        @Nullable private OnItemLongClickListener mOnItemLongCLickListener;
        @Nullable private OnItemClickListener mOnItemClickListener;

        BookmarkListAdapter(@NonNull List<HistoryItem> bookmarks,
                            @NonNull FaviconModel faviconModel,
                            @NonNull Bitmap folderBitmap,
                            @NonNull Bitmap webpageBitmap) {
            mBookmarks = bookmarks;
            mFaviconModel = faviconModel;
            mFolderBitmap = folderBitmap;
            mWebpageBitmap = webpageBitmap;
        }

        void setOnItemLongClickListener(@Nullable OnItemLongClickListener listener) {
            mOnItemLongCLickListener = listener;
        }

        void setOnItemClickListener(@Nullable OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        @Override
        public BookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.bookmark_list_item, parent, false);

            return new BookmarkViewHolder(itemView, mOnItemLongCLickListener, mOnItemClickListener);
        }

        @Override
        public void onBindViewHolder(BookmarkViewHolder holder, int position) {
            ViewCompat.jumpDrawablesToCurrentState(holder.itemView);

            final HistoryItem web = mBookmarks.get(position);
            holder.txtTitle.setText(web.getTitle());
            if (web.isFolder()) {
                holder.favicon.setImageBitmap(mFolderBitmap);
            } else if (web.getBitmap() == null) {
                holder.favicon.setImageBitmap(mWebpageBitmap);
                holder.favicon.setTag(web.getUrl().hashCode());

                final String url = web.getUrl();
                final WeakReference<ImageView> imageViewReference = new WeakReference<>(holder.favicon);

                mFaviconModel.faviconForUrl(url, mWebpageBitmap, true)
                    .subscribeOn(Schedulers.worker())
                    .observeOn(Schedulers.main())
                    .subscribe(new SingleOnSubscribe<Bitmap>() {
                        @Override
                        public void onItem(@Nullable Bitmap item) {
                            ImageView imageView = imageViewReference.get();
                            Object tag = imageView != null ? imageView.getTag() : null;
                            if (tag != null && tag.equals(url.hashCode())) {
                                imageView.setImageBitmap(item);
                            }

                            web.setBitmap(item);
                        }
                    });
            } else {
                holder.favicon.setImageBitmap(web.getBitmap());
            }

        }

        @Override
        public int getItemCount() {
            return mBookmarks.size();
        }
    }

}
