package acr.browser.lightning.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.utils.DeviceUtils;
import acr.browser.lightning.utils.ResourceUtils;
import acr.browser.lightning.utils.Utils;

/**
 * Copyright 7/31/2016 Anthony Restaino
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class BrowserDialog {

    public interface Listener {
        void onClick();
    }

    public interface EditorListener {
        void onClick(String text);
    }

    public static abstract class Item {

        private int mTitle;
        private boolean mCondition = true;

        public Item(@StringRes int title, boolean condition) {
            this(title);
            mCondition = condition;
        }

        public Item(@StringRes int title) {
            mTitle = title;
        }

        @StringRes
        private int getTitle() {
            return mTitle;
        }

        private boolean isConditionMet() {
            return mCondition;
        }

        public abstract void onClick();
    }


    public static void show(@NonNull Activity activity, @NonNull Item item, @Nullable Item... items) {
        show(activity, null, item, items);
    }

    public static void show(@NonNull Activity activity, @StringRes int title, @NonNull Item item, @Nullable Item... items) {
        show(activity, activity.getString(title), item, items);
    }

    public static void show(@NonNull Activity activity, @Nullable String title, @NonNull Item item, @Nullable Item... items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View layout = LayoutInflater.from(activity).inflate(R.layout.list_dialog, null);

        TextView titleView = (TextView) layout.findViewById(R.id.dialog_title);
        ListView listView = (ListView) layout.findViewById(R.id.dialog_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
            android.R.layout.simple_list_item_1);

        final List<Item> itemList = new ArrayList<>(1);
        if (item.isConditionMet()) {
            itemList.add(item);
        }
        if (items != null) {
            for (Item it : items) {
                if (it.isConditionMet()) {
                    itemList.add(it);
                }
            }
        }

        for (Item it : itemList) {
            adapter.add(activity.getString(it.getTitle()));
        }

        if (!TextUtils.isEmpty(title)) {
            titleView.setText(title);
        }

        listView.setAdapter(adapter);

        listView.setDivider(null);
        builder.setView(layout);

        int maxWidth = ResourceUtils.dimen(activity, R.dimen.dialog_max_size);
        int padding = ResourceUtils.dimen(activity, R.dimen.dialog_padding);
        int screenSize = DeviceUtils.getScreenWidth(activity);
        if (maxWidth > screenSize - 2 * padding) {
            maxWidth = screenSize - 2 * padding;
        }

        final Dialog dialog = builder.show();
        dialog.getWindow().setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemList.get(position).onClick();
                dialog.dismiss();
            }
        });
    }

    public static void showEditText(@NonNull Activity activity, @StringRes int title,
                                    @StringRes int hint, @StringRes int action,
                                    @NonNull final EditorListener listener) {
        showEditText(activity, title, hint, null, action, listener);
    }

    public static void showEditText(@NonNull Activity activity, @StringRes int title,
                                    @StringRes int hint, @Nullable String currentText,
                                    @StringRes int action, @NonNull final EditorListener listener) {
        final AlertDialog.Builder editorDialog = new AlertDialog.Builder(activity);
        editorDialog.setTitle(title);
        final EditText editText = new EditText(activity);
        editText.setHint(hint);
        if (currentText != null) {
            editText.setText(currentText);
        }
        editText.setSingleLine();
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(10);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(editText);
        editorDialog.setView(layout);
        editorDialog.setPositiveButton(action,
            new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClick(editText.getText().toString());
                }
            });
        editorDialog.show();
    }

}
