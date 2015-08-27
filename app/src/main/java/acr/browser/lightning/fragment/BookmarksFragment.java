package acr.browser.lightning.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.BrowserActivity;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.bus.BusProvider;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.DownloadImageTask;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

import static android.support.v7.app.AlertDialog.Builder;

/**
 * Created by Stefano Pacifici on 25/08/15. Based on Anthony C. Restaino's code.
 */
public class BookmarksFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    // Managers
    private BookmarkManager mBookmarkManager;

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
            mBookmarkManager = BookmarkManager.getInstance(context.getApplicationContext());
            mBookmarkAdapter = new BookmarkViewAdapter(context, mBookmarks);
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
            mBookmarksListView.setAdapter(mBookmarkAdapter);
        }
    };

    // Handle bookmark click
    private final OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final HistoryItem item = mBookmarks.get(position);
            if (item.isFolder()) {
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(item.getTitle(), true),
                        true);
            } else {
                BusProvider.getInstance().post(new BookmarkEvents.Clicked(item));
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
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void addBookmark(final BrowserEvents.AddBookmark event) {
        final HistoryItem item = new HistoryItem(event.url, event.title);
        if (mBookmarkManager.addBookmark(item)) {
            mBookmarks.add(item);
            Collections.sort(mBookmarks, new BookmarkManager.SortIgnoreCase());
            mBookmarkAdapter.notifyDataSetChanged();
            BusProvider.getInstance()
                    .post(new BookmarkEvents.Added(item));
            updateBookmarkIndicator(event.url);
        }
    }

    @Subscribe
    public void currentPageInfo(final BrowserEvents.CurrentPageUrl event) {
        updateBookmarkIndicator(event.url);
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
            BusProvider.getInstance()
                    .post(new BookmarkEvents.CloseBookmarks());
        } else {
            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
        }
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
            longPressFolder(item, position);
            return;
        } else {
            final Bus bus = BusProvider.getInstance();
            final DialogInterface.OnClickListener dialogClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    bus.post(new BookmarkEvents.AsNewTab(item));
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    if (mBookmarkManager.deleteBookmark(item)) {
                                        mBookmarks.remove(position);
                                        mBookmarkAdapter.notifyDataSetChanged();
                                        bus.post(new BookmarkEvents.Deleted(item));
                                    }
                                    break;
                                case DialogInterface.BUTTON_NEUTRAL:
                                    editBookmark(item, position);
                                    break;
                            }
                        }
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.action_bookmarks)
                    .setMessage(R.string.dialog_bookmark)
                    .setCancelable(true)
                    .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                    .setNegativeButton(R.string.action_delete, dialogClickListener)
                    .setNeutralButton(R.string.action_edit, dialogClickListener)
                    .show();
        }
    }

    private void longPressFolder(final HistoryItem item, final int position) {
        final DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                renameFolder(item, position);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                mBookmarkManager.deleteFolder(item.getTitle());
                                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
                                BusProvider.getInstance().post(new BookmarkEvents.Deleted(item));
                                /* TODO Restore Bookmarkpage
                                if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                                        && mCurrentView.getUrl().endsWith(BookmarkPage.FILENAME)) {
                                    openBookmarkPage(mWebView);
                                }*/
                                break;
                        }
                    }
                };

        Builder builder = new Builder(getContext());
        builder.setTitle(R.string.action_folder)
                .setMessage(R.string.dialog_folder)
                .setCancelable(true)
                .setPositiveButton(R.string.action_rename, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .show();
    }

    /**
     * Takes in the id of which bookmark was selected and shows a dialog that
     * allows the user to rename and change the url of the bookmark
     *
     * @param item      the bookmark
     * @param position  the position inside the adapter
     */
    private synchronized void editBookmark(final HistoryItem item, final int position) {
        final Builder editBookmarkDialog = new Builder(getContext());
        editBookmarkDialog.setTitle(R.string.title_edit_bookmark);
        final View dialogLayout = View.inflate(getContext(), R.layout.dialog_edit_bookmark, null);
        final EditText getTitle = (EditText) dialogLayout.findViewById(R.id.bookmark_title);
        getTitle.setText(item.getTitle());
        final EditText getUrl = (EditText) dialogLayout.findViewById(R.id.bookmark_url);
        getUrl.setText(item.getUrl());
        final AutoCompleteTextView getFolder =
                (AutoCompleteTextView) dialogLayout.findViewById(R.id.bookmark_folder);
        getFolder.setHint(R.string.folder);
        getFolder.setText(item.getFolder());
        final List<String> folders = mBookmarkManager.getFolderTitles();
        final ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, folders);
        getFolder.setThreshold(1);
        getFolder.setAdapter(suggestionsAdapter);
        editBookmarkDialog.setView(dialogLayout);
        editBookmarkDialog.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HistoryItem editedItem = new HistoryItem();
                        String currentFolder = item.getFolder();
                        editedItem.setTitle(getTitle.getText().toString());
                        editedItem.setUrl(getUrl.getText().toString());
                        editedItem.setUrl(getUrl.getText().toString());
                        editedItem.setFolder(getFolder.getText().toString());
                        mBookmarkManager.editBookmark(item, editedItem);

                        List<HistoryItem> list = mBookmarkManager.getBookmarksFromFolder(currentFolder, true);
                        if (list.isEmpty()) {
                            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
                        } else {
                            setBookmarkDataSet(list, false);
                        }
                        BusProvider.getInstance()
                                .post(new BookmarkEvents.WantInfoAboutCurrentPage());
                        /* TODO Restore BookmarkPage
                        if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                                && mCurrentView.getUrl().endsWith(BookmarkPage.FILENAME)) {
                            openBookmarkPage(mWebView);
                        }*/
                    }
                });
        editBookmarkDialog.show();
    }

    /**
     * Show a dialog to rename a folder
     *
     * @param id the position of the HistoryItem (folder) in the bookmark list
     */
    private synchronized void renameFolder(final HistoryItem item, final int id) {
        final Context context = getContext();
        final Builder editFolderDialog = new Builder(context);
        editFolderDialog.setTitle(R.string.title_rename_folder);
        final EditText getTitle = new EditText(context);
        getTitle.setHint(R.string.hint_title);
        getTitle.setText(item.getTitle());
        getTitle.setSingleLine();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(10);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(getTitle);
        editFolderDialog.setView(layout);
        editFolderDialog.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String oldTitle = item.getTitle();
                        String newTitle = getTitle.getText().toString();

                        mBookmarkManager.renameFolder(oldTitle, newTitle);

                        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
                        /* TODO Restore Bookmarkpage
                        if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                                && mCurrentView.getUrl().endsWith(BookmarkPage.FILENAME)) {
                            openBookmarkPage(mWebView);
                        }*/
                    }
                });
        editFolderDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_add_bookmark:
                BusProvider.getInstance().post(new BookmarkEvents.WantToBookmarkCurrentPage());
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
