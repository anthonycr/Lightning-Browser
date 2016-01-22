package acr.browser.lightning.activity;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.reading.HtmlFetcher;
import acr.browser.lightning.reading.JResult;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;
import butterknife.Bind;
import butterknife.ButterKnife;

public class ReadingActivity extends AppCompatActivity {

    @Bind(R.id.textViewTitle)
    TextView mTitle;

    @Bind(R.id.textViewBody)
    TextView mBody;

    @Inject PreferenceManager mPreferences;

    private boolean mInvert;
    private String mUrl = null;
    private int mTextSize;
    private ProgressDialog mProgressDialog;
    private PageLoader mLoaderReference;

    private static final float XXLARGE = 30.0f;
    private static final float XLARGE = 26.0f;
    private static final float LARGE = 22.0f;
    private static final float MEDIUM = 18.0f;
    private static final float SMALL = 14.0f;
    private static final float XSMALL = 10.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BrowserApp.getAppComponent().inject(this);

        overridePendingTransition(R.anim.slide_in_from_right, R.anim.fade_out_scale);
        mInvert = mPreferences.getInvertColors();
        final int color;
        if (mInvert) {
            setTheme(R.style.Theme_SettingsTheme_Dark);
            color = ThemeUtils.getPrimaryColorDark(this);
            getWindow().setBackgroundDrawable(new ColorDrawable(color));
        } else {
            setTheme(R.style.Theme_SettingsTheme);
            color = ThemeUtils.getPrimaryColor(this);
            getWindow().setBackgroundDrawable(new ColorDrawable(color));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reading_view);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextSize = mPreferences.getReadingTextSize();
        mBody.setTextSize(getTextSize(mTextSize));
        mTitle.setText(getString(R.string.untitled));
        mBody.setText(getString(R.string.loading));

        mTitle.setVisibility(View.INVISIBLE);
        mBody.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        if (!loadPage(intent)) {
            setText(getString(R.string.untitled), getString(R.string.loading_failed));
        }
    }

    private static float getTextSize(int size) {
        switch (size) {
            case 0:
                return XSMALL;
            case 1:
                return SMALL;
            case 2:
                return MEDIUM;
            case 3:
                return LARGE;
            case 4:
                return XLARGE;
            case 5:
                return XXLARGE;
            default:
                return MEDIUM;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reading, menu);
        MenuItem invert = menu.findItem(R.id.invert_item);
        MenuItem textSize = menu.findItem(R.id.text_size_item);
        int iconColor = mInvert ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        if (invert != null && invert.getIcon() != null)
            invert.getIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        if (textSize != null && textSize.getIcon() != null)
            textSize.getIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean loadPage(Intent intent) {
        if (intent == null) {
            return false;
        }
        mUrl = intent.getStringExtra(Constants.LOAD_READING_URL);
        if (mUrl == null) {
            return false;
        }
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(Utils.getDomainName(mUrl));
        mLoaderReference = new PageLoader(this);
        mLoaderReference.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUrl);
        return true;
    }

    private class PageLoader extends AsyncTask<String, Void, Void> {

        private final WeakReference<Activity> mActivityReference;
        private String mTitleText;
        private String mBodyText;

        public PageLoader(Activity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Activity activity = mActivityReference.get();
            if (activity != null) {
                mProgressDialog = new ProgressDialog(activity);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setMessage(activity.getString(R.string.loading));
                mProgressDialog.show();
            }
        }

        @Override
        protected Void doInBackground(String... params) {

            HtmlFetcher fetcher = new HtmlFetcher();
            try {
                JResult result = fetcher.fetchAndExtract(params[0], 2500, true);
                mTitleText = result.getTitle();
                mBodyText = result.getText();
            } catch (Exception e) {
                mTitleText = "";
                mBodyText = "";
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                System.gc();
                mTitleText = "";
                mBodyText = "";
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            if (mTitleText.isEmpty() || mBodyText.isEmpty()) {
                setText(getString(R.string.untitled), getString(R.string.loading_failed));
            } else {
                setText(mTitleText, mBodyText);
            }
            super.onPostExecute(result);
        }

    }

    private void setText(String title, String body) {
        if (mTitle == null || mBody == null)
            return;
        if (mTitle.getVisibility() == View.INVISIBLE) {
            mTitle.setAlpha(0.0f);
            mTitle.setVisibility(View.VISIBLE);
            mTitle.setText(title);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mTitle, "alpha", 1.0f);
            animator.setDuration(300);
            animator.start();
        } else {
            mTitle.setText(title);
        }

        if (mBody.getVisibility() == View.INVISIBLE) {
            mBody.setAlpha(0.0f);
            mBody.setVisibility(View.VISIBLE);
            mBody.setText(body);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mBody, "alpha", 1.0f);
            animator.setDuration(300);
            animator.start();
        } else {
            mBody.setText(body);
        }
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        mLoaderReference.cancel(true);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_out_to_right);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_item:
                mPreferences.setInvertColors(!mInvert);
                Intent read = new Intent(this, ReadingActivity.class);
                read.putExtra(Constants.LOAD_READING_URL, mUrl);
                startActivity(read);
                finish();
                break;
            case R.id.text_size_item:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                View view = inflater.inflate(R.layout.seek_layout, null);
                final SeekBar bar = (SeekBar) view.findViewById(R.id.text_size_seekbar);
                bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar view, int size, boolean user) {
                        mBody.setTextSize(getTextSize(size));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar arg0) {
                    }

                });
                bar.setMax(5);
                bar.setProgress(mTextSize);
                builder.setView(view);
                builder.setTitle(R.string.size);
                builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mTextSize = bar.getProgress();
                        mBody.setTextSize(getTextSize(mTextSize));
                        mPreferences.setReadingTextSize(bar.getProgress());
                    }

                });
                builder.show();
                break;
            default:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
