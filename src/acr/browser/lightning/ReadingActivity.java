package acr.browser.lightning;

import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.reading.HtmlFetcher;
import acr.browser.lightning.reading.JResult;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ReadingActivity extends ActionBarActivity {

	private TextView mTitle;
	private TextView mBody;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reading_view);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mTitle = (TextView) findViewById(R.id.textViewTitle);
		mBody = (TextView) findViewById(R.id.textViewBody);

		mTitle.setText(getString(R.string.untitled));
		mBody.setText(getString(R.string.loading));

		mTitle.setVisibility(View.INVISIBLE);
		mBody.setVisibility(View.INVISIBLE);

		Intent intent = getIntent();
		if (!loadPage(intent)) {
			setText(getString(R.string.untitled), getString(R.string.loading_failed));
		}
	}

	protected boolean loadPage(Intent intent) {
		if (intent == null) {
			return false;
		}
		String url = intent.getStringExtra(Constants.LOAD_READING_URL);
		if (url == null) {
			return false;
		}
		getSupportActionBar().setTitle(Utils.getDomainName(url));
		new PageLoader(this).execute(url);
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
		finish();
		return super.onOptionsItemSelected(item);
	}

}
