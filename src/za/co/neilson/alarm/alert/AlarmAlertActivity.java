/* Copyright 2014 Sheldon Neilson www.neilson.co.za
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package za.co.neilson.alarm.alert;

import za.co.neilson.alarm.Alarm;
import za.co.neilson.alarm.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class AlarmAlertActivity extends Activity {

	private Alarm alarm;
	private MediaPlayer mediaPlayer;

	private Vibrator vibrator;

	private boolean alarmActive;
	private WebView mWebView;  
	private JsHandler _jsHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.alarm_alert);
		mWebView = new WebView(this){                                                                
			@Override                                                                                
			public void postUrl(String  url, byte[] postData)                                        
			{                                                                                        
				System.out.println("postUrl can modified here:" +url);                               
				super.postUrl(url, postData);                                                        
			}};                                                                                      
	    RelativeLayout layout = (RelativeLayout) findViewById(R.id.alert);                            
	    layout.addView(mWebView);                                                                    

		mWebView.setWebViewClient(new MyBrowser());  
		_jsHandler = new JsHandler(this, mWebView);		
		
		open();
		Bundle bundle = this.getIntent().getExtras();
		alarm = (Alarm) bundle.getSerializable("alarm");

        monitorCallState();
		startAlarm();
	}
	
	private void monitorCallState() {
		TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);

		PhoneStateListener phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					Log.d(getClass().getSimpleName(), "Incoming call: "
							+ incomingNumber);
					try {
						mediaPlayer.pause();
					} catch (IllegalStateException e) {

					}
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					Log.d(getClass().getSimpleName(), "Call State Idle");
					try {
						mediaPlayer.start();
					} catch (IllegalStateException e) {

					}
					break;
				}
				super.onCallStateChanged(state, incomingNumber);
			}
		};

		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		alarmActive = true;
	}

	private void startAlarm() {

		if (alarm.getAlarmTonePath() != "") {
			mediaPlayer = new MediaPlayer();
			if (alarm.getVibrate()) {
				vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				long[] pattern = { 1000, 200, 200, 200 };
				vibrator.vibrate(pattern, 0);
			}
			try {
				mediaPlayer.setVolume(1.0f, 1.0f);
				mediaPlayer.setDataSource(this,
						Uri.parse(alarm.getAlarmTonePath()));
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
				mediaPlayer.start();

			} catch (Exception e) {
				mediaPlayer.release();
				alarmActive = false;
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if (!alarmActive)
			super.onBackPressed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		StaticWakeLock.lockOff(this);
	}

	@Override
	protected void onDestroy() {
		try {
			if (vibrator != null)
				vibrator.cancel();
		} catch (Exception e) {

		}
		try {
			mediaPlayer.stop();
		} catch (Exception e) {

		}
		try {
			mediaPlayer.release();
		} catch (Exception e) {

		}
		super.onDestroy();
	}

	public void cancelAlert() {
		alarmActive = false;
		if (vibrator != null)
			vibrator.cancel();
		try {
			mediaPlayer.stop();
		} catch (IllegalStateException ise) {

		}
		try {
			mediaPlayer.release();
		} catch (Exception e) {

		}
		this.finish();
	}
	
	private class MyBrowser extends WebViewClient {                                                 

		@Override                                                                                  
		public WebResourceResponse shouldInterceptRequest (final WebView view, String url) {                                                          
			return super.shouldInterceptRequest(view, url);                                          
		}                                                                                          

		@Override                                                                                  
		public boolean shouldOverrideUrlLoading(WebView view, String url) {                                                                           
			if (Uri.parse(url).getHost().equals("www.google.com")) {                                                       
				// This is reCAPTCHA web site, so do not override; let my WebView load the page           
				return false;                                                                      
			}                                                                                                                                                  
			return true;                                                                           
		}                                                                                          
	} 
	
	@SuppressLint("SetJavaScriptEnabled") public void open(){                                                                                            
		String url = "http://haidong1.mtv.corp.google.com:8888/recaptcha/api2/mframe?k=6LehsLkSAAAAACYLmwf73O_1HrUnKy565VVmCDZR&callback=JsHandler";                                   
		mWebView.getSettings().setLoadsImagesAutomatically(true);                                    
		mWebView.getSettings().setJavaScriptEnabled(true);                                           
		mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);                                  
		mWebView.addJavascriptInterface(_jsHandler, "JsHandler");
		mWebView.loadUrl(url);
	}
}
