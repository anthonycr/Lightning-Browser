package acr.browser.lightning.browser.fragment;

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
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.browser.bookmark.BookmarkUiModel;
import acr.browser.lightning.reading.activity.ReadingActivity;
import acr.browser.lightning.browser.TabsManager;
import acr.browser.lightning.animation.AnimationUtils;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.browser.BookmarksView;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkModel;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.favicon.FaviconModel;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.SubscriptionUtils;
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

    private final static String INCOGNITO_MODE = TAG + ".INCOGNITO_MODE";

    // Managers
    @Inject BookmarkModel mBookmarkManager;

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

    // Views
    @BindView(R.id.right_drawer_list) RecyclerView mBookmarksListView;
    @BindView(R.id.starIcon) ImageView mBookmarkTitleImage;
    @BindView(R.id.icon_star) ImageView mBookmarkImage;

    @Nullable private Unbinder mUnbinder;

    // Colors
    private int mIconColor, mScrollIndex;

    private boolean mIsIncognito;

    @Nullable private Subscription mBookmarksSubscription;
    @Nullable private Subscription mFoldersSubscription;
    @Nullable private Subscription mBookmarkUpdateSubscription;

    @NonNull private final BookmarkUiModel mUiModel = new BookmarkUiModel();

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
        public void onItemClick(@NonNull HistoryItem item) {
            if (item.isFolder()) {
                mScrollIndex = ((LinearLayoutManager) mBookmarksListView.getLayoutManager()).findFirstVisibleItemPosition();
                setBookmarksShown(item.getTitle(), true);
            } else {
                mUiController.bookmarkItemClicked(item);
            }
        }
    };

    private final OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(@NonNull HistoryItem item) {
            handleLongPress(item);
            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (mBookmarkAdapter != null) {
            setBookmarksShown(null, false);
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
                if (!mUiModel.isRootFolder()) {
                    setBookmarksShown(null, true);
                    mBookmarksListView.getLayoutManager().scrollToPosition(mScrollIndex);
                }
            }
        });
        setupNavigationButton(view, R.id.action_add_bookmark, R.id.icon_star);
        setupNavigationButton(view, R.id.action_reading, R.id.icon_reading);
        setupNavigationButton(view, R.id.action_toggle_desktop, R.id.icon_desktop);

        mBookmarkAdapter = new BookmarkListAdapter(mFaviconModel, mFolderBitmap, mWebpageBitmap);
        mBookmarkAdapter.setOnItemClickListener(mItemClickListener);
        mBookmarkAdapter.setOnItemLongClickListener(mItemLongClickListener);
        mBookmarksListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBookmarksListView.setAdapter(mBookmarkAdapter);

        setBookmarksShown(null, true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        SubscriptionUtils.safeUnsubscribe(mBookmarksSubscription);
        SubscriptionUtils.safeUnsubscribe(mFoldersSubscription);
        SubscriptionUtils.safeUnsubscribe(mBookmarkUpdateSubscription);

        if (mBookmarkAdapter != null) {
            mBookmarkAdapter.cleanupSubscriptions();
        }

        if (mUnbinder != null) {
            mUnbinder.unbind();
            mUnbinder = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SubscriptionUtils.safeUnsubscribe(mBookmarksSubscription);
        SubscriptionUtils.safeUnsubscribe(mFoldersSubscription);
        SubscriptionUtils.safeUnsubscribe(mBookmarkUpdateSubscription);

        if (mBookmarkAdapter != null) {
            mBookmarkAdapter.cleanupSubscriptions();
        }
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
        SubscriptionUtils.safeUnsubscribe(mBookmarkUpdateSubscription);
        mBookmarkUpdateSubscription = mBookmarkManager.isBookmark(url)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<Boolean>() {
                @Override
                public void onItem(@Nullable Boolean item) {
                    mBookmarkUpdateSubscription = null;
                    Preconditions.checkNonNull(item);
                    Activity activity = getActivity();
                    if (mBookmarkImage == null || activity == null) {
                        return;
                    }
                    if (!item) {
                        mBookmarkImage.setImageResource(R.drawable.ic_action_star);
                        mBookmarkImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
                    } else {
                        mBookmarkImage.setImageResource(R.drawable.ic_bookmark);
                        mBookmarkImage.setColorFilter(ThemeUtils.getAccentColor(activity), PorterDuff.Mode.SRC_IN);
                    }
                }
            });
    }

    @Override
    public void handleBookmarkDeleted(@NonNull HistoryItem item) {
        if (item.isFolder()) {
            setBookmarksShown(null, false);
        } else {
            mBookmarkAdapter.deleteItem(item);
        }
    }

    private void setBookmarksShown(@Nullable final String folder, final boolean animate) {
        SubscriptionUtils.safeUnsubscribe(mBookmarksSubscription);
        mBookmarksSubscription = mBookmarkManager.getBookmarksFromFolderSorted(folder)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                @Override
                public void onItem(@Nullable final List<HistoryItem> item) {
                    mBookmarksSubscription = null;
                    Preconditions.checkNonNull(item);

                    mUiModel.setCurrentFolder(folder);
                    if (folder == null) {
                        SubscriptionUtils.safeUnsubscribe(mFoldersSubscription);
                        mFoldersSubscription = mBookmarkManager.getFoldersSorted()
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.main())
                            .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                                @Override
                                public void onItem(@Nullable List<HistoryItem> folders) {
                                    mFoldersSubscription = null;
                                    Preconditions.checkNonNull(folders);
                                    item.addAll(folders);
                                    setBookmarkDataSet(item, animate);
                                }
                            });
                    } else {
                        setBookmarkDataSet(item, animate);
                    }
                }
            });
    }

    private void setBookmarkDataSet(@NonNull List<HistoryItem> items, boolean animate) {
        mBookmarkAdapter.updateItems(items);
        final int resource;
        if (mUiModel.isRootFolder()) {
            resource = R.drawable.ic_action_star;
        } else {
            resource = R.drawable.ic_action_back;
        }

        if (animate) {
            Animation transition = AnimationUtils.createRotationTransitionAnimation(mBookmarkTitleImage, resource);
            mBookmarkTitleImage.startAnimation(transition);
        } else {
            mBookmarkTitleImage.setImageResource(resource);
        }
    }

    private void setupNavigationButton(@NonNull View view, @IdRes int buttonId, @IdRes int imageId) {
        FrameLayout frameButton = view.findViewById(buttonId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        ImageView buttonImage = view.findViewById(imageId);
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
        if (mUiModel.isRootFolder()) {
            mUiController.closeBookmarksDrawer();
        } else {
            setBookmarksShown(null, true);
            mBookmarksListView.getLayoutManager().scrollToPosition(mScrollIndex);
        }
    }

    @Override
    public void handleUpdatedUrl(@NonNull String url) {
        updateBookmarkIndicator(url);
        String folder = mUiModel.getCurrentFolder();
        setBookmarksShown(folder, false);
    }

    static class BookmarkViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        @BindView(R.id.textBookmark) TextView txtTitle;
        @BindView(R.id.faviconBookmark) ImageView favicon;

        @NonNull private final BookmarkListAdapter adapter;

        @Nullable private final OnItemLongClickListener onItemLongClickListener;
        @Nullable private final OnItemClickListener onItemClickListener;

        BookmarkViewHolder(@NonNull View itemView,
                           @NonNull BookmarkListAdapter adapter,
                           @Nullable OnItemLongClickListener onItemLongClickListener,
                           @Nullable OnItemClickListener onItemClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            this.adapter = adapter;

            this.onItemClickListener = onItemClickListener;
            this.onItemLongClickListener = onItemLongClickListener;

            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int index = getAdapterPosition();
            if (onItemClickListener != null && index != RecyclerView.NO_ID) {
                onItemClickListener.onItemClick(adapter.itemAt(index));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int index = getAdapterPosition();
            return index != RecyclerView.NO_POSITION && onItemLongClickListener != null &&
                onItemLongClickListener.onItemLongClick(adapter.itemAt(index));
        }
    }

    interface OnItemLongClickListener {
        boolean onItemLongClick(@NonNull HistoryItem item);
    }

    interface OnItemClickListener {
        void onItemClick(@NonNull HistoryItem item);
    }

    private static class BookmarkListAdapter extends RecyclerView.Adapter<BookmarkViewHolder> {

        @NonNull private List<HistoryItem> mBookmarks = new ArrayList<>();
        @NonNull private final FaviconModel mFaviconModel;
        @NonNull private final Bitmap mFolderBitmap;
        @NonNull private final Bitmap mWebpageBitmap;
        @NonNull private final Map<String, Subscription> mFaviconFetchSubscriptions = new ConcurrentHashMap<>();

        @Nullable private OnItemLongClickListener mOnItemLongCLickListener;
        @Nullable private OnItemClickListener mOnItemClickListener;

        BookmarkListAdapter(@NonNull FaviconModel faviconModel,
                            @NonNull Bitmap folderBitmap,
                            @NonNull Bitmap webpageBitmap) {
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

        @NonNull
        HistoryItem itemAt(int position) {
            return mBookmarks.get(position);
        }

        void deleteItem(@NonNull HistoryItem item) {
            List<HistoryItem> newList = new ArrayList<>(mBookmarks);
            newList.remove(item);
            updateItems(newList);
        }

        void updateItems(@NonNull List<HistoryItem> newList) {
            final List<HistoryItem> oldList = mBookmarks;
            mBookmarks = newList;

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldList.size();
                }

                @Override
                public int getNewListSize() {
                    return mBookmarks.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldList.get(oldItemPosition).equals(mBookmarks.get(newItemPosition));
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldList.get(oldItemPosition).equals(mBookmarks.get(newItemPosition));
                }
            });

            diffResult.dispatchUpdatesTo(this);
        }

        void cleanupSubscriptions() {
            for (Subscription subscription : mFaviconFetchSubscriptions.values()) {
                subscription.unsubscribe();
            }
            mFaviconFetchSubscriptions.clear();
        }

        @Override
        public BookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.bookmark_list_item, parent, false);

            return new BookmarkViewHolder(itemView, this, mOnItemLongCLickListener, mOnItemClickListener);
        }

        @Override
        public void onViewRecycled(BookmarkViewHolder holder) {
            super.onViewRecycled(holder);
        }

        @Override
        public void onBindViewHolder(final BookmarkViewHolder holder, int position) {
            ViewCompat.jumpDrawablesToCurrentState(holder.itemView);

            final HistoryItem web = mBookmarks.get(position);
            holder.txtTitle.setText(web.getTitle());
            if (web.isFolder()) {
                holder.favicon.setImageBitmap(mFolderBitmap);
            } else if (web.getBitmap() == null) {
                holder.favicon.setImageBitmap(mWebpageBitmap);
                holder.favicon.setTag(web.getUrl().hashCode());

                final String url = web.getUrl();

                Subscription oldSubscription = mFaviconFetchSubscriptions.get(url);
                SubscriptionUtils.safeUnsubscribe(oldSubscription);

                final Subscription faviconSubscription = mFaviconModel.faviconForUrl(url, web.getTitle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.main())
                    .subscribe(new SingleOnSubscribe<Bitmap>() {
                        @Override
                        public void onItem(@Nullable Bitmap item) {
                            mFaviconFetchSubscriptions.remove(url);
                            Object tag = holder.favicon.getTag();
                            if (tag != null && tag.equals(url.hashCode())) {
                                holder.favicon.setImageBitmap(item);
                            }

                            web.setBitmap(item);
                        }
                    });

                mFaviconFetchSubscriptions.put(url, faviconSubscription);
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
