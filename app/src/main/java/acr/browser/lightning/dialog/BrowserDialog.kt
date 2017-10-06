/*
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
package acr.browser.lightning.dialog

import acr.browser.lightning.R
import acr.browser.lightning.utils.DeviceUtils
import acr.browser.lightning.utils.ResourceUtils
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*

object BrowserDialog {

    // TODO convert to method reference
    interface EditorListener {
        fun onClick(text: String)
    }

    @JvmStatic
    fun show(activity: Activity, @StringRes title: Int, vararg items: DialogItem) =
            show(activity, activity.getString(title), *items)

    @JvmStatic
    fun show(activity: Activity, title: String?, vararg items: DialogItem) {
        val builder = AlertDialog.Builder(activity)

        val layout = LayoutInflater.from(activity).inflate(R.layout.list_dialog, null)

        val titleView = layout.findViewById<TextView>(R.id.dialog_title)
        val listView = layout.findViewById<ListView>(R.id.dialog_list)

        val adapter = ArrayAdapter<String>(activity,
                android.R.layout.simple_list_item_1)

        val itemList = items.filter(DialogItem::isConditionMet)

        adapter.addAll(itemList.map { activity.getString(it.title) })

        if (!TextUtils.isEmpty(title)) {
            titleView.text = title
        }

        listView.adapter = adapter

        listView.divider = null
        builder.setView(layout)

        val dialog = builder.show()

        setDialogSize(activity, dialog)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            itemList[position].onClick()
            dialog.dismiss()
        }
    }

    @JvmStatic
    fun showEditText(activity: Activity,
                     @StringRes title: Int,
                     @StringRes hint: Int,
                     @StringRes action: Int,
                     listener: EditorListener) =
            showEditText(activity, title, hint, null, action, listener)

    @JvmStatic
    fun showEditText(activity: Activity,
                     @StringRes title: Int,
                     @StringRes hint: Int,
                     currentText: String?,
                     @StringRes action: Int,
                     listener: EditorListener) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setHint(hint)
        if (currentText != null) {
            editText.setText(currentText)
        }

        val editorDialog = AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(action
                ) { _, _ -> listener.onClick(editText.text.toString()) }

        val dialog = editorDialog.show()
        setDialogSize(activity, dialog)
    }

    @JvmStatic
    fun setDialogSize(context: Context, dialog: Dialog) {
        var maxWidth = ResourceUtils.dimen(context, R.dimen.dialog_max_size)
        val padding = ResourceUtils.dimen(context, R.dimen.dialog_padding)
        val screenSize = DeviceUtils.getScreenWidth(context)
        if (maxWidth > screenSize - 2 * padding) {
            maxWidth = screenSize - 2 * padding
        }
        val window = dialog.window
        window?.setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

}
