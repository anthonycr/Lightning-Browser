/*
 * Copyright 2015-present Pop Tech Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fillr.browsersdk.model;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fillr.browsersdk.R;

public class FillrBrowserProperties {

    private String mBrowserName = null;
    private String mBarBrowserName = null;

    /**
     *
     * @param browserName Name of the browser in the install dialog prompt
     * @param fillrBarBrowserName Name of the browser displayed
     *
     * @see com.fillr.browsersdk.FillrToolbarView
     *
     */
    public FillrBrowserProperties(String browserName,
                                  String fillrBarBrowserName){
        mBrowserName = browserName;
        mBarBrowserName = fillrBarBrowserName;
    }

    public String getShortBrowserName() {
        return mBrowserName;
    }

    public String getBarBrowserName() {
        return mBarBrowserName;
    }

    public void setDialogProps(View dialogView, Context context){

        if(dialogView!=null){
            TextView title   = (TextView) dialogView.findViewById(R.id.dialog_title_text);
            Button btnYes    = (Button) dialogView.findViewById(R.id.id_btn_yes);
            if(title!=null && mBrowserName!=null){
                title.setText(context.getResources().getString(R.string.install_fillr_title_bspec, mBrowserName));
            }
            if(btnYes!=null && mBrowserName!=null){
                btnYes.setText(context.getResources().getString(R.string.install_fillr_button_yes_bspec, mBrowserName));
            }
        }
    }

}
