package acr.browser.lightning.activity;

import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.R;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.reading.HtmlFetcher;
import acr.browser.lightning.reading.JResult;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ReadingActivity extends ActionBarActivity {

	private TextView mTitle;
	private TextView mBody;
	private boolean mInvert;
	private String mUrl = null;
	private PreferenceManager mPreferences;
	private int mTextSize;
	private static final float XXLARGE = 30.0f;
	private static final float XLARGE = 26.0f;
	private static final float LARGE = 22.0f;
	private static final float MEDIUM = 18.0f;
	private static final float SMALL = 14.0f;
	private static final float XSMALL = 10.0f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mPreferences = PreferenceManager.getInstance();
		mInvert = mPreferences.getInvertColors();
		if (mInvert) {
			this.setTheme(R.style.Theme_SettingsTheme_Dark);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reading_view);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mTitle = (TextView) findViewById(R.id.textViewTitle);
		mBody = (TextView) findViewById(R.id.textViewBody);

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

	private float getTextSize(int size) {
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
		return super.onCreateOptionsMenu(menu);
	}

	protected boolean loadPage(Intent intent) {
		if (intent == null) {
			return false;
		}
		mUrl = intent.getStringExtra(Constants.LOAD_READING_URL);
		if (mUrl == null) {
			return false;
		}
		getSupportActionBar().setTitle(Utils.getDomainName(mUrl));
		new PageLoader(this).execute(mUrl);
		return true;
	}

	private class PageLoader extends AsyncTask<String, Void, Void> {

		private Context mContext;
		private ProgressDialog mProgressDialog;
		private String mTitleText;
		private List<String> mBodyText;

		public PageLoader(Context context) {
			mContext = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMessage(mContext.getString(R.string.loading));
			mProgressDialog.show();
		}

		@Override
		protected Void doInBackground(String... params) {

			HtmlFetcher fetcher = new HtmlFetcher();
			try {
				JResult result = fetcher.fetchAndExtract(params[0], 5000, true);
				mTitleText = result.getTitle();
				mBodyText = result.getTextList();
			} catch (Exception e) {
				mTitleText = "";
				mBodyText = new ArrayList<>();
				e.printStackTrace();
			} catch (OutOfMemoryError e) {
				System.gc();
				mTitleText = "";
				mBodyText = new ArrayList<>();
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
			if (mTitleText.isEmpty() || mBodyText.isEmpty()) {
				setText(getString(R.string.untitled), getString(R.string.loading_failed));
			} else {
				StringBuilder builder = new StringBuilder();
				for (String text : mBodyText) {
					builder.append(text).append("\n\n");
				}
				setText(mTitleText, builder.toString());
			}
			super.onPostExecute(result);
		}

	}

	private void setText(String title, String body) {
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
