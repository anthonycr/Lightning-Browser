package acr.browser.lightning.reading.activity;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import javax.inject.Inject;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.R;
import acr.browser.lightning.di.MainScheduler;
import acr.browser.lightning.di.NetworkScheduler;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.preference.UserPreferences;
import acr.browser.lightning.reading.HtmlFetcher;
import acr.browser.lightning.reading.JResult;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class ReadingActivity extends AppCompatActivity {

    private static final String LOAD_READING_URL = "ReadingUrl";

    /**
     * Launches this activity with the necessary URL argument.
     *
     * @param context The context needed to launch the activity.
     * @param url     The URL that will be loaded into reading mode.
     */
    public static void launch(@NonNull Context context, @NonNull String url) {
        final Intent intent = new Intent(context, ReadingActivity.class);
        intent.putExtra(LOAD_READING_URL, url);
        context.startActivity(intent);
    }

    private static final String TAG = "ReadingActivity";

    @BindView(R.id.textViewTitle) TextView mTitle;
    @BindView(R.id.textViewBody) TextView mBody;

    @Inject UserPreferences mUserPreferences;
    @Inject @NetworkScheduler Scheduler mNetworkScheduler;
    @Inject @MainScheduler Scheduler mMainScheduler;

    private boolean mInvert;
    @Nullable private String mUrl = null;
    private int mTextSize;
    @Nullable private ProgressDialog mProgressDialog;
    private Disposable mPageLoaderSubscription;

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
        mInvert = mUserPreferences.getInvertColors();
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextSize = mUserPreferences.getReadingTextSize();
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
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.reading, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean loadPage(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }
        mUrl = intent.getStringExtra(LOAD_READING_URL);
        if (mUrl == null) {
            return false;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(Utils.getDomainName(mUrl));
        }

        mProgressDialog = new ProgressDialog(ReadingActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.show();
        BrowserDialog.setDialogSize(ReadingActivity.this, mProgressDialog);

        mPageLoaderSubscription = loadPage(mUrl)
            .subscribeOn(mNetworkScheduler)
            .observeOn(mMainScheduler)
            .subscribe(readerInfo -> {
                if (readerInfo.getTitle().isEmpty() || readerInfo.getBody().isEmpty()) {
                    setText(getString(R.string.untitled), getString(R.string.loading_failed));
                } else {
                    setText(readerInfo.getTitle(), readerInfo.getBody());
                }
                dismissProgressDialog();
            }, throwable -> {
                setText(getString(R.string.untitled), getString(R.string.loading_failed));
                dismissProgressDialog();
            });
        return true;
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @NonNull
    private static Single<ReaderInfo> loadPage(@NonNull final String url) {
        return Single.create(emitter -> {
            HtmlFetcher fetcher = new HtmlFetcher();
            try {
                JResult result = fetcher.fetchAndExtract(url, 2500, true);
                emitter.onSuccess(new ReaderInfo(result.getTitle(), result.getText()));
            } catch (Exception e) {
                emitter.onError(new Throwable("Encountered exception"));
                Log.e(TAG, "Error parsing page", e);
            } catch (OutOfMemoryError e) {
                System.gc();
                emitter.onError(new Throwable("Out of memory"));
                Log.e(TAG, "Out of memory", e);
            }
        });
    }

    private static class ReaderInfo {
        @NonNull private final String mTitleText;
        @NonNull private final String mBodyText;

        ReaderInfo(@NonNull String title, @NonNull String body) {
            mTitleText = title;
            mBodyText = body;
        }

        @NonNull
        public String getTitle() {
            return mTitleText;
        }

        @NonNull
        public String getBody() {
            return mBodyText;
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
        mPageLoaderSubscription.dispose();

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_item:
                mUserPreferences.setInvertColors(!mInvert);
                if (mUrl != null) {
                    ReadingActivity.launch(this, mUrl);
                    finish();
                }
                break;
            case R.id.text_size_item:

                View view = LayoutInflater.from(this).inflate(R.layout.dialog_seek_bar, null);
                final SeekBar bar = view.findViewById(R.id.text_size_seekbar);
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

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setView(view)
                    .setTitle(R.string.size)
                    .setPositiveButton(android.R.string.ok, (dialog, arg1) -> {
                        mTextSize = bar.getProgress();
                        mBody.setTextSize(getTextSize(mTextSize));
                        mUserPreferences.setReadingTextSize(bar.getProgress());
                    });
                Dialog dialog = builder.show();
                BrowserDialog.setDialogSize(this, dialog);
                break;
            default:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
