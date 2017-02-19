package com.quickswitch;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

	private static final String LOG = "QuickSwitch";
	private static final String SERVICE_ADDRESS = "http://192.168.0.1";

	Button btnGate;

	String networkSSID = "\"ESPap\"";
	String networkPass = "\"thereisnospoon\"";

	boolean actionFlag = false;

	AsyncHttpClient client = new AsyncHttpClient();

	ProgressDialog progress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnGate = (Button) findViewById(R.id.btnGate);

		btnGate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				DoAction();
			}
		});

		Configure();

		final ConnectivityManager conMan =
				(ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = conMan.getActiveNetworkInfo();

		NetworkRequest.Builder request = new NetworkRequest.Builder();
		request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

		conMan.registerNetworkCallback(request.build(),
				new ConnectivityManager.NetworkCallback() {
					@Override
					public void onAvailable(Network network) {
						conMan.bindProcessToNetwork(network);

						if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
							WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
							WifiInfo info = wifiManager.getConnectionInfo();
							String ssid = info.getSSID();

							Log.d(LOG, "Connected with " + ssid);

							if ((actionFlag) && (ssid.equals(networkSSID))) {
								actionFlag = false;
								CallService();
							}
						}

						if (progress != null) progress.dismiss();
					}
				});
	}

	private void Configure() {
		boolean configured = false;
		WifiManager wifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

		for (WifiConfiguration i : list) {
			if (i.SSID != null && i.SSID.equals(networkSSID)) {
				if (i.preSharedKey.equals(networkPass))
					configured = true;
				else
					wifiManager.removeNetwork(i.networkId);
				break;
			}
		}

		if (!configured) {
			WifiConfiguration conf = new WifiConfiguration();
			conf.SSID = networkSSID;
			conf.preSharedKey = networkPass;
			wifiManager.addNetwork(conf);
			wifiManager.saveConfiguration();

			Log.d(LOG, "WiFi: " + networkSSID + " configured.");
		}
	}

	private void DoAction() {
		WifiManager wifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

		WifiInfo info = wifiManager.getConnectionInfo();
		String ssid = info.getSSID();

		if (ssid.equals(networkSSID)) {
			CallService();
		} else {
			Log.d(LOG, "Connecting with gate.");

			actionFlag = true;

			progress = new ProgressDialog(this);
			progress.setTitle("Connecting");
			progress.setMessage("Connecting with gate...");
			progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
			progress.show();

			for (WifiConfiguration i : list) {
				if (i.SSID != null && i.SSID.equals(networkSSID)) {
					wifiManager.disconnect();
					wifiManager.enableNetwork(i.networkId, true);
					wifiManager.reconnect();

					Log.d(LOG, "Reconnecting with " + networkSSID);
					break;
				}
			}
		}
	}

	private void CallService() {
		client.get(SERVICE_ADDRESS, new AsyncHttpResponseHandler() {

			@Override
			public void onStart() {
				Log.d(LOG, "Calling service at " + SERVICE_ADDRESS);
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
				Log.d(LOG, "Service success");
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
				Log.d(LOG, "Service failure");
			}

//			@Override
//			public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
//				Log.d(LOG, "Service success JSONArray");
//			}
//
//			@Override
//			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//				Log.d(LOG, "Service success JSONObject");
////
////				try {
////					boolean ledstate = Boolean.valueOf((String) response.get("ledstate"));
////
////				} catch (JSONException e) {
////					e.printStackTrace();
////				}
//
//			}
		});
	}
}
