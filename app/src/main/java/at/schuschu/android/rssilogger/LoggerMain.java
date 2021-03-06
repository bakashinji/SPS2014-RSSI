package at.schuschu.android.rssilogger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LoggerMain extends Activity {

    BroCast bc = null;
    IntentFilter filter;
    WifiManager wifimanager;
    ListView listview;
    Button button;
    SimpleAdapter adapter;
    TextView lastscan;
    boolean running;
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    static HashMap<String, ArrayList<HashMap<String, String>>> backlog = new HashMap<String, ArrayList<HashMap<String, String>>>();

    List<ScanResult> results;
    final static String TS_KEY = "secret_ts_key_that_could_be_a_UUID";
    final static String SSID_KEY = "secret_ssid_key_that_could_be_a_UUID";
    final static String LEVEL_KEY = "secret_level_key_that_could_be_a_UUID";
    final static String BSSID_KEY = "secret_bssid_key_that_could_be_a_UUID";
    int size = 0;

    class BroCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context c, Intent intent) {
            results = wifimanager.getScanResults();
            size = results.size();
            updateview();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger_main);

        running = false;

        lastscan = (TextView) findViewById(R.id.tv_lastscan);
        button = (Button) findViewById(R.id.bu_scan);
        listview = (ListView) findViewById(R.id.lv_results);

        try {

            File json = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/rssilogger.json");
            if (json.exists()) {
                BufferedReader br;
                br = new BufferedReader(new FileReader(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/rssilogger.json"));
                Gson gson = new Gson();
                backlog = gson.fromJson(br, backlog.getClass());
            }
        } catch (IOException e) {
            e.printStackTrace();

        }

        wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        {
            Toast.makeText(getApplicationContext(), "Works better with WiFi(TM) enabled", Toast.LENGTH_LONG).show();
            wifimanager.setWifiEnabled(true);
        }

        adapter = new SimpleAdapter(this, arraylist, R.layout.listview_row, new String[]{SSID_KEY, LEVEL_KEY, BSSID_KEY}, new int[]{R.id.tv_row_ssid, R.id.tv_row_level, R.id.tv_row_bssid});
        listview.setAdapter(adapter);

        bc = new BroCast();
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(bc, filter);


    }

    protected void onPause() {
        super.onPause();
        if (bc != null) {
            unregisterReceiver(bc);
            bc = null;
        }
    }

    protected void onResume() {
        super.onResume();
        bc = new BroCast();
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(bc, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logger_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void replay(View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("BACKLOG", backlog);
        intent.putExtras(bundle);
        intent.setClass(this, LoggerReplay.class);
        startActivity(intent);
    }

    public void scan(View v) {
        Toast.makeText(this, "Scan STARTED!", Toast.LENGTH_SHORT).show();
        running = true;
        wifimanager.startScan();

    }

    public void stopscan(View view) {
        Toast.makeText(this, "Scan STOPPED!", Toast.LENGTH_SHORT).show();

        running = false;
        if (bc != null) {
            unregisterReceiver(bc);
            bc = null;
        }
    }

    public void updateview() {

        if (!running) {
            if (bc != null) {
                unregisterReceiver(bc);
                bc = null;
            }
            return;
        }

        arraylist.clear();
        wifimanager.startScan();

        try {
            size = size - 1;
            while (size >= 0) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(SSID_KEY, results.get(size).SSID);
                item.put(LEVEL_KEY, Integer.toString(results.get(size).level));
                item.put(BSSID_KEY, results.get(size).BSSID);

                arraylist.add(item);
                size--;
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) { /* this is a sad day for mankind*/ } finally {
            ArrayList<HashMap<String, String>> tempy = new ArrayList<HashMap<String, String>>(arraylist);

            long mills = System.currentTimeMillis();
            lastscan.setText("Last scan: " + mills);
            backlog.put(Long.toString(mills), tempy);

            try {
                Gson gson = new Gson();
                String json = gson.toJson(backlog);
                FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/rssilogger.json");
                writer.write(json);
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
