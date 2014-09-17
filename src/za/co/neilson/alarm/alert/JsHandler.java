package za.co.neilson.alarm.alert;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class JsHandler {
	AlarmAlertActivity activity;
	WebView webView;

	public JsHandler(AlarmAlertActivity activity,WebView webView) {
		this.activity = activity;
		this.webView = webView; 
	}

	/**
	 * This function handles call from JS
	 */
	@JavascriptInterface
	public void jsCallback(String jsString) {
		System.out.println("----------" + jsString);
		activity.cancelAlert();
	}
}
