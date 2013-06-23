package acr.browser.barebones;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class Settings extends Activity {
	private static final String preferences = "settings";
	private static boolean allowLocation;
    private static boolean deleteHistory;
	private static int enableFlash;
	private static boolean savePasswords;
    private static boolean fullScreen;
    private static boolean java;
    private static boolean saveTabs;
	private static String userAgent;
    private static String homepage;
	private static EditText agent;
    private static EditText h;
	private static SharedPreferences.Editor edit;
	private static int agentPicker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		TextView it = (TextView) findViewById(R.id.textView1);
		
		agent = (EditText) findViewById(R.id.agent);
		agent.setSelectAllOnFocus(true);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		SharedPreferences settings = getSharedPreferences(preferences, 0);
		WebView test = new WebView(Settings.this);
		String user = test.getSettings().getUserAgentString();
		test.destroy();
		it.requestFocus();
		allowLocation = settings.getBoolean("location", false);
		saveTabs = settings.getBoolean("savetabs", true);
		savePasswords = settings.getBoolean("passwords", true);
		deleteHistory = settings.getBoolean("history", false);
		fullScreen = settings.getBoolean("fullscreen", false);
		enableFlash = settings.getInt("enableflash", 0);
		agentPicker = settings.getInt("agentchoose", 1);
		userAgent = settings.getString("agent", user);
		java = settings.getBoolean("java", true);
		homepage = settings.getString("home", FinalVars.HOMEPAGE);

		agent.setText(userAgent);
		edit = settings.edit();
		final RadioButton m = (RadioButton) findViewById(R.id.radioMobile);
		final RadioButton d = (RadioButton) findViewById(R.id.radioDesktop);
		final RadioButton c = (RadioButton) findViewById(R.id.radioCustom);
		m.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				d.setChecked(false);
				c.setChecked(false);
				edit.putInt("agentchoose", 1);
				edit.commit();
			}
			
		});
		d.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				m.setChecked(false);
				c.setChecked(false);
				edit.putInt("agentchoose", 2);
				edit.commit();
			}
			
		});
		c.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				m.setChecked(false);
				d.setChecked(false);
				edit.putInt("agentchoose", 3);
				edit.commit();
			}
			
		});
		switch (agentPicker) {
		case 1:
			m.setChecked(true);
			d.setChecked(false);
			c.setChecked(false);
			break;
		case 2:
			m.setChecked(false);
			d.setChecked(true);
			c.setChecked(false);
			break;
		case 3:
			
			m.setChecked(false);
			d.setChecked(false);
			c.setChecked(true);
			break;
		}
		back();
		location();
		passwords();
		clearHistory();
		saveTabs();
		getText();
		flash();
		getHome();
		full();
		java();
	}
	
	void saveTabs(){
		final CheckBox tab = (CheckBox)findViewById(R.id.saveTabs);
		if(saveTabs){
			tab.setChecked(true);
			
		}else{
			tab.setChecked(false);
		}
		tab.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if(arg1){
					edit.putBoolean("savetabs", true);
					edit.commit();
				}else{
					edit.putBoolean("savetabs", false);
					edit.commit();
				}
			}
			
		});
	}

	void getHome() {
		h = (EditText) findViewById(R.id.homePage);
		
		TextView t = (TextView) findViewById(R.id.textView3);
		t.setBackgroundResource(R.drawable.button);
		TextView a = (TextView) findViewById(R.id.textView2);
		a.setBackgroundResource(R.drawable.button);
		h.setSelectAllOnFocus(true);
		h.setText(homepage);
		h.setSingleLine(true);
		h.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				// TODO Auto-generated method stub
				switch(arg1){
				case KeyEvent.KEYCODE_ENTER:
					String home;
					home = h.getText().toString();
					if(!home.contains("about:blank")&&!home.contains("about:home")){
					if(!home.contains("http://") && !home.contains("https://")){
						home = "http://"+home;
					}}
					edit.putString("home", home);
					edit.commit();
					return true;
				default: break;
				}
				return false;
			}
			
		});
		h.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_GO
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT
						|| actionId == EditorInfo.IME_ACTION_SEND||actionId==EditorInfo.IME_ACTION_SEARCH||event.getAction()==KeyEvent.KEYCODE_ENTER) {
					String home = h.getText().toString();
					if(!h.getText().toString().contains("about:blank")&&!h.getText().toString().contains("about:home")){
					if(!h.getText().toString().contains("http://") && !h.getText().toString().contains("https://")){
						home = "http://"+h.getText().toString();
						}}
					edit.putString("home", home);
					edit.commit();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(agent.getWindowToken(), 0);
					return true;
				}
				return false;
			}

		});
	}

	public void radioAgent(View v) {

		boolean checked = ((RadioButton) v).isChecked();
		switch (v.getId()) {
		case (R.id.radioMobile):
			if (checked) {
				edit.putInt("agentchoose", 1);
				edit.commit();
				
			}
			break;
		case (R.id.radioDesktop):
			if (checked) {
				edit.putInt("agentchoose", 2);
				edit.commit();
				
			}
			break;
		case (R.id.radioCustom):
			if (checked) {
				edit.putInt("agentchoose", 3);
				edit.commit();
				
			}
			break;
		}

	}

	void back() {
		ImageView back = (ImageView) findViewById(R.id.back);
		back.setBackgroundResource(R.drawable.button);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (agentPicker == 3) {
					userAgent = agent.getText().toString();
					edit.putString("agent", userAgent);
					edit.commit();
				}
				String home = h.getText().toString();
				if(!h.getText().toString().contains("about:blank")&&!h.getText().toString().contains("about:home")){
				if(!h.getText().toString().contains("http://") && !h.getText().toString().contains("https://")){
					home = "http://"+h.getText().toString();
					}}
				edit.putString("home", home);
				edit.commit();
				finish();
			}

		});
	}
	void java(){
		final CheckBox full = (CheckBox)findViewById(R.id.java);
		if(java){
			full.setChecked(true);
			
		}else{
			full.setChecked(false);
		}
		full.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if(arg1){
					edit.putBoolean("java", true);
					edit.commit();
				}else{
					edit.putBoolean("java", false);
					edit.commit();
				}
			}
			
		});
	}
void full(){
	final CheckBox full = (CheckBox)findViewById(R.id.fullScreen);
	if(fullScreen){
		full.setChecked(true);
		
	}else{
		full.setChecked(false);
	}
	full.setOnCheckedChangeListener(new OnCheckedChangeListener(){

		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
			// TODO Auto-generated method stub
			if(arg1){
				edit.putBoolean("fullscreen", true);
				edit.commit();
			}else{
				edit.putBoolean("fullscreen", false);
				edit.commit();
			}
		}
		
	});
}
	void flash() {
		final CheckBox fla = (CheckBox) findViewById(R.id.flash);
		if (enableFlash == 1||enableFlash==2) {
			fla.setChecked(true);
		} else {
			fla.setChecked(false);
		}
		fla.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								edit.putInt("enableflash", 1);
								edit.commit();
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								edit.putInt("enableflash", 2);
								edit.commit();
								break;
							}
							default:
								fla.setChecked(false);
								edit.putInt("enableflash", 0);
								break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							Settings.this); // dialog
					builder.setMessage(
							"How do you want Flash enabled?")
							.setPositiveButton("Load on-demand",
									dialogClickListener)
							.setNegativeButton("Always load",
									dialogClickListener).show();
					builder.setOnCancelListener(new OnCancelListener(){

						@Override
						public void onCancel(DialogInterface arg0) {
							// TODO Auto-generated method stub
							edit.putInt("enableflash", 0);
							edit.commit();
							fla.setChecked(false);
						}
						
					});
					
					
				} else {
					edit.putInt("enableflash", 0);
					edit.commit();
				}
			}

		});
	}

	void location() {
		CheckBox loc = (CheckBox) findViewById(R.id.location);
		if (allowLocation) {
			loc.setChecked(true);
		} else {
			loc.setChecked(false);
		}
		loc.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					edit.putBoolean("location", true);
					edit.commit();
				} else {
					edit.putBoolean("location", false);
					edit.commit();
				}
			}

		});
	}


	void getText() {
		agent.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				// TODO Auto-generated method stub
				switch(arg1){
				case KeyEvent.KEYCODE_ENTER:
					userAgent = agent.getText().toString();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(agent.getWindowToken(), 0);
					edit.putString("agent", userAgent);
					edit.commit();
					return true;
				default: break;
				}
				return false;
			}
			
		});
		agent.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_GO
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT
						|| actionId == EditorInfo.IME_ACTION_SEND||actionId==EditorInfo.IME_ACTION_SEARCH||event.getAction()==KeyEvent.KEYCODE_ENTER) {
					userAgent = agent.getText().toString();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(agent.getWindowToken(), 0);
					edit.putString("agent", userAgent);
					edit.commit();
					return true;
				}
				return false;
			}

		});
	}

	void passwords() {
		CheckBox pass = (CheckBox) findViewById(R.id.password);
		if (savePasswords) {
			pass.setChecked(true);
		} else {
			pass.setChecked(false);
		}
		pass.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					edit.putBoolean("passwords", true);
					edit.commit();
				} else {
					edit.putBoolean("passwords", false);
					edit.commit();
				}
			}

		});
	}

	void clearHistory() {
		CheckBox clearHist = (CheckBox) findViewById(R.id.historyClear);
		if (deleteHistory) {
			clearHist.setChecked(true);
		} else {
			clearHist.setChecked(false);
		}
		clearHist.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					edit.putBoolean("history", true);
					edit.commit();
				} else {
					edit.putBoolean("history", false);
					edit.commit();
				}
			}

		});
	}


	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (agentPicker == 3) {
			userAgent = agent.getText().toString();
			edit.putString("agent", userAgent);
			edit.commit();
		}
		String home = h.getText().toString();
		if(!h.getText().toString().contains("about:blank")&&!h.getText().toString().contains("about:home")){
		if(!h.getText().toString().contains("http://") && !h.getText().toString().contains("https://")){
			home = "http://"+h.getText().toString();
			}}
		edit.putString("home", home);
		edit.commit();
		super.onBackPressed();
	}
}
