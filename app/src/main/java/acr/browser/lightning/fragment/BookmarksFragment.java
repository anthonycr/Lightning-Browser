package acr.browser.lightning.fragment;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.ReadingActivity;
import acr.browser.lightning.activity.TabsManager;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.async.AsyncExecutor;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.async.ImageDownloadTask;
import acr.browser.lightning.react.Action;
import acr.browser.lightning.react.Observable;
import acr.browser.lightning.react.OnSubscribe;
import acr.browser.lightning.react.Schedulers;
import acr.browser.lightning.react.Subscriber;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.view.LightningView;

public class BookmarksFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    private final static String TAG = BookmarksFragment.class.getSimpleName();

    public final static String INCOGNITO_MODE = TAG + ".INCOGNITO_MODE";

    // Managers
    @Inject BookmarkManager mBookmarkManager;

    // Event bus
    @Inject Bus mEventBus;

    // Dialog builder
    @Inject LightningDialogBuilder mBookmarksDialogBuilder;

    @Inject PreferenceManager mPreferenceManager;

    @Inject TabsManager mTabsManager;

    // Adapter
    private BookmarkViewAdapter mBookmarkAdapter;

    // Preloaded images
    private Bitmap mWebpageBitmap, mFolderBitmap;

    // Bookmarks
    private final List<HistoryItem> mBookmarks = new ArrayList<>();

    // Views
    private ListView mBookmarksListView;
    private ImageView mBookmarkTitleImage, mBookmarkImage;

    // Colors
    private int mIconColor, mScrollIndex;

    private Observable<BookmarkViewAdapter> initBookmarkManager() {
        return Observable.create(new Action<BookmarkViewAdapter>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<BookmarkViewAdapter> subscriber) {
                mBookmarkAdapter = new BookmarkViewAdapter(getContext(), mBookmarks);
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
                subscriber.onNext(mBookmarkAdapter);
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        final Bundle arguments = getArguments();
        final Context context = getContext();
        boolean isIncognito = arguments.getBoolean(INCOGNITO_MODE, false);
        boolean darkTheme = mPreferenceManager.getUseTheme() != 0 || isIncognito;
        mWebpageBitmap = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, darkTheme);
        mFolderBitmap = ThemeUtils.getThemedBitmap(context, R.drawable.ic_folder, darkTheme);
        mIconColor = darkTheme ? ThemeUtils.getIconDarkThemeColor(context) :
                ThemeUtils.getIconLightThemeColor(context);
    }

    // Handle bookmark click
    private final OnItemClickListener mItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final HistoryItem item = mBookmarks.get(position);
            if (item.isFolder()) {
                mScrollIndex = mBookmarksListView.getFirstVisiblePosition();
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(item.getTitle(), true), true);
            } else {
                mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(item.getUrl()));
            }
        }
    };

    private final OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
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
        mBookmarksListView = (ListView) view.findViewById(R.id.right_drawer_list);
        mBookmarksListView.setOnItemClickListener(mItemClickListener);
        mBookmarksListView.setOnItemLongClickListener(mItemLongClickListener);
        mBookmarkTitleImage = (ImageView) view.findViewById(R.id.starIcon);
        mBookmarkTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        mBookmarkImage = (ImageView) view.findViewById(R.id.icon_star);
        final View backView = view.findViewById(R.id.bookmark_back_button);
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBookmarkManager == null) return;
                if (!mBookmarkManager.isRootFolder()) {
                    setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
                    mBookmarksListView.setSelection(mScrollIndex);
                }
            }
        });
        setupNavigationButton(view, R.id.action_add_bookmark, R.id.icon_star);
        setupNavigationButton(view, R.id.action_reading, R.id.icon_reading);
        setupNavigationButton(view, R.id.action_toggle_desktop, R.id.icon_desktop);

        initBookmarkManager().subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.main())
                .subscribe(new OnSubscribe<BookmarkViewAdapter>() {
                    @Override
                    public void onNext(@Nullable BookmarkViewAdapter item) {
                        mBookmarksListView.setAdapter(mBookmarkAdapter);
                    }
                });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    @Subscribe
    public void addBookmark(@NonNull final BrowserEvents.BookmarkAdded event) {
        updateBookmarkIndicator(event.url);
        String folder = mBookmarkManager.getCurrentFolder();
        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(folder, true), false);
    }

    @Subscribe
    public void currentPageInfo(@NonNull final BrowserEvents.CurrentPageUrl event) {
        updateBookmarkIndicator(event.url);
        String folder = mBookmarkManager.getCurrentFolder();
        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(folder, true), false);
    }

    @Subscribe
    public void bookmarkChanged(BookmarkEvents.BookmarkChanged event) {
        String folder = mBookmarkManager.getCurrentFolder();
        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(folder, true), false);
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

    @Subscribe
    public void userPressedBack(final BrowserEvents.UserPressedBack event) {
        if (mBookmarkManager.isRootFolder()) {
            mEventBus.post(new BookmarkEvents.CloseBookmarks());
        } else {
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
            mBookmarksListView.setSelection(mScrollIndex);
        }
    }

    @Subscribe
    public void bookmarkDeleted(@NonNull final BookmarkEvents.Deleted event) {
        mBookmarks.remove(event.item);
        if (event.item.isFolder()) {
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
            mBookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(getContext(), item);
        } else {
            mBookmarksDialogBuilder.showLongPressedDialogForBookmarkUrl(getContext(), item);
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()) {
            case R.id.action_add_bookmark:
                mEventBus.post(new BookmarkEvents.ToggleBookmarkForCurrentPage());
                break;
            case R.id.action_reading:
                LightningView currentTab = mTabsManager.getCurrentTab();
                if (currentTab != null) {
                    Intent read = new Intent(getActivity(), ReadingActivity.class);
                    read.putExtra(Constants.LOAD_READING_URL, currentTab.getUrl());
                    startActivity(read);
                }
                break;
            case R.id.action_toggle_desktop:
                LightningView current = mTabsManager.getCurrentTab();
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

    private class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

        final Context context;

        public BookmarkViewAdapter(Context context, @NonNull List<HistoryItem> data) {
            super(context, R.layout.bookmark_list_item, data);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            BookmarkViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(R.layout.bookmark_list_item, parent, false);

                holder = new BookmarkViewHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.textBookmark);
                holder.favicon = (ImageView) row.findViewById(R.id.faviconBookmark);
                row.setTag(holder);
            } else {
                holder = (BookmarkViewHolder) row.getTag();
            }

            ViewCompat.jumpDrawablesToCurrentState(row);

            HistoryItem web = mBookmarks.get(position);
            holder.txtTitle.setText(web.getTitle());
            if (web.isFolder()) {
                holder.favicon.setImageBitmap(mFolderBitmap);
            } else if (web.getBitmap() == null) {
                holder.favicon.setImageBitmap(mWebpageBitmap);
                new ImageDownloadTask(holder.favicon, web, mWebpageBitmap, context)
                        .executeOnExecutor(AsyncExecutor.getInstance());
            } else {
                holder.favicon.setImageBitmap(web.getBitmap());
            }
            return row;
        }

        private class BookmarkViewHolder {
            TextView txtTitle;
            ImageView favicon;
        }
    }

}
