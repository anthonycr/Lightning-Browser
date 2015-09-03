package acr.browser.lightning.fragment;

import android.content.Context;
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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.BrowserActivity;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.dialog.BookmarksDialogBuilder;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.DownloadImageTask;
import acr.browser.lightning.utils.ThemeUtils;

/**
 * Created by Stefano Pacifici on 25/08/15. Based on Anthony C. Restaino's code.
 */
public class BookmarksFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    // Managers
    @Inject
    BookmarkManager mBookmarkManager;

    // Event bus
    @Inject
    Bus eventBus;

    // Dialog builder
    @Inject
    BookmarksDialogBuilder bookmarksDialogBuilder;

    // Adapter
    private BookmarkViewAdapter mBookmarkAdapter;

    // Preloaded images
    private Bitmap mWebpageBitmap, mFolderBitmap;

    // Bookmarks
    private List<HistoryItem> mBookmarks = new ArrayList<>();

    // Views
    private ListView mBookmarksListView;
    private ImageView mBookmarkTitleImage, mBookmarkImage;

    // Colors
    private int mIconColor;

    // Init asynchronously the bookmark manager
    private final Runnable initBookmarkManager = new Runnable() {
        @Override
        public void run() {
            final Context context = getContext();
            mBookmarkAdapter = new BookmarkViewAdapter(context, mBookmarks);
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
            mBookmarksListView.setAdapter(mBookmarkAdapter);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
    }

    // Handle bookmark click
    private final OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final HistoryItem item = mBookmarks.get(position);
            if (item.isFolder()) {
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(item.getTitle(), true),
                        true);
            } else {
                eventBus.post(new BookmarkEvents.Clicked(item));
            }
        }
    };

    private final OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final HistoryItem item = mBookmarks.get(position);
            handleLongPress(item, position);
            return true;
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.bookmark_drawer, container, false);
        mBookmarksListView = (ListView) view.findViewById(R.id.right_drawer_list);
        mBookmarksListView.setOnItemClickListener(itemClickListener);
        mBookmarksListView.setOnItemLongClickListener(itemLongClickListener);
        mBookmarkTitleImage = (ImageView) view.findViewById(R.id.starIcon);
        mBookmarkImage = (ImageView) view.findViewById(R.id.icon_star);
        final View backView = view.findViewById(R.id.bookmark_back_button);
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBookmarkManager == null)
                    return;
                if (!mBookmarkManager.isRootFolder()) {
                    setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
                }
            }
        });

        // Must be called here, only here we have a reference to the ListView
        new Thread(initBookmarkManager).run();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // TODO this code depend way too much on BrowserActivity
        super.onActivityCreated(savedInstanceState);
        final BrowserActivity activity = (BrowserActivity) getActivity();
        final PreferenceManager preferenceManager =PreferenceManager.getInstance();
        boolean darkTheme = preferenceManager.getUseTheme() != 0 || activity.isIncognito();
        mWebpageBitmap = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_webpage, darkTheme);
        mFolderBitmap = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_folder, darkTheme);
        mIconColor = darkTheme ? ThemeUtils.getIconDarkThemeColor(activity) :
                ThemeUtils.getIconLightThemeColor(activity);
        setupFrameLayoutButton(getView(), R.id.action_add_bookmark, R.id.icon_star);
        mBookmarkTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    @Subscribe
    public void addBookmark(final BrowserEvents.AddBookmark event) {
        final HistoryItem item = new HistoryItem(event.url, event.title);
        if (mBookmarkManager.addBookmark(item)) {
            mBookmarks.add(item);
            Collections.sort(mBookmarks, new BookmarkManager.SortIgnoreCase());
            mBookmarkAdapter.notifyDataSetChanged();
            eventBus
                    .post(new BookmarkEvents.Added(item));
            updateBookmarkIndicator(event.url);
        }
    }

    @Subscribe
    public void currentPageInfo(final BrowserEvents.CurrentPageUrl event) {
        updateBookmarkIndicator(event.url);
    }

    @Subscribe
    public void bookmarkChanged(BookmarkEvents.BookmarkChanged event) {
        final int size = mBookmarks.size();
        mBookmarks.remove(event.oldBookmark);
        assert mBookmarks.size() < size;
        mBookmarks.add(event.newBookmark);
        mBookmarkAdapter.notifyDataSetChanged();
        Collections.sort(mBookmarks, new BookmarkManager.SortIgnoreCase());
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
            eventBus
                    .post(new BookmarkEvents.CloseBookmarks());
        } else {
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
        }
    }

    @Subscribe
    public void bookmarkDeleted(final BookmarkEvents.Deleted event) {
        final HistoryItem item = event.item;
        final int size = mBookmarks.size();
        mBookmarks.remove(event);
        assert mBookmarks.size() < size;
        mBookmarkAdapter.notifyDataSetChanged();
    }

    private void setBookmarkDataSet(List<HistoryItem> items, boolean animate) {
        mBookmarks.clear();
        mBookmarks.addAll(items);
        mBookmarkAdapter.notifyDataSetChanged();
        final int resource;
        if (mBookmarkManager.isRootFolder())
            resource = R.drawable.ic_action_star;
        else
            resource = R.drawable.ic_action_back;

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

    // TODO this is basically a copy/paste from BrowserActivity, should be changed
    private void setupFrameLayoutButton(@NonNull View view, @IdRes int buttonId, @IdRes int imageId) {
        FrameLayout frameButton = (FrameLayout) view.findViewById(buttonId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        ImageView buttonImage = (ImageView) view.findViewById(imageId);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    private void handleLongPress(final HistoryItem item, final int position) {
        if (item.isFolder()) {
            bookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(getContext(), item);
        } else {
            bookmarksDialogBuilder.showLongPressedDialogForUrl(getContext(), item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_add_bookmark:
                eventBus.post(new BookmarkEvents.WantToBookmarkCurrentPage());
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

        public BookmarkViewAdapter(Context context, List<HistoryItem> data) {
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
            holder.favicon.setImageBitmap(mWebpageBitmap);
            if (web.isFolder()) {
                holder.favicon.setImageBitmap(mFolderBitmap);
            } else if (web.getBitmap() == null) {
                new DownloadImageTask(holder.favicon, web, mWebpageBitmap).execute();
            } else {
                holder.favicon.setImageBitmap(web.getBitmap());
            }
            return row;
        }

        class BookmarkViewHolder {
            TextView txtTitle;
            ImageView favicon;
        }
    }

}
