package com.quickswitch;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

	private static final String LOG = "QuickSwitch";
	private static final String SERVICE_ADDRESS = "http://192.168.0.1";

	Button btnGate;

	ConnectivityManager conMan;

	String networkSSID = "\"ESPap\"";
	String networkPass = "\"thereisnospoon\"";

	AsyncHttpClient client = new AsyncHttpClient();

	ProgressDialog progress;

	boolean doActionFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnGate = (Button) findViewById(R.id.btnGate);
		btnGate.setOnClickListener(new btnGateListener());

		conMan = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

		Configure();

		NetworkRequest.Builder request = new NetworkRequest.Builder();
		request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

		conMan.registerNetworkCallback(request.build(), new NetCallback());
	}

	// Listen the button click event, if it's not connected to the right network it changes.
	private class btnGateListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

			WifiInfo info = wifiManager.getConnectionInfo();
			String ssid = info.getSSID();

			doActionFlag = true;

			if (ssid.equals(networkSSID)) {
				CallService();
			} else {
				Log.d(LOG, "Connecting with gate.");

				progress = new ProgressDialog(MainActivity.this);
				progress.setTitle("Connecting");
				progress.setMessage("Connecting with gate...");
				progress.setCancelable(true); // disable dismiss by tapping outside of the dialog
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
	}

	// Listener to when the network is switched and connected to the network defined above.
	private class NetCallback extends ConnectivityManager.NetworkCallback {
		@Override
		public void onAvailable(Network network) {
			Log.d(LOG, "Network available.");
			if (conMan != null)
				conMan.bindProcessToNetwork(network);

			if (progress != null) progress.dismiss();

			if (doActionFlag) {
				WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				WifiInfo info = wifiManager.getConnectionInfo();
				String ssid = info.getSSID();

				Log.d(LOG, "Connected with " + ssid);

				if (ssid.equals(networkSSID)) {
					doActionFlag = false;
					CallService();
				}
			}
		}
	}

	// Adds the network configuration defined above to the WiFi manager.
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

	// Calls the http service.
	private void CallService() {
		client.get(SERVICE_ADDRESS, new JsonHttpResponseHandler() {

			@Override
			public void onStart() {
				Log.d(LOG, "Calling service at " + SERVICE_ADDRESS);
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				Log.d(LOG, "Service success JSONObject");

				try {
					String r = (String) response.get("ledstate");

					if (r.equals("0"))
						Log.d(LOG, "LED state false");
					else if (r.equals("1"))
						Log.d(LOG, "LED state true");

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
