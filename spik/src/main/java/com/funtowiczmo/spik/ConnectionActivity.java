package com.funtowiczmo.spik;

import android.app.ProgressDialog;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.funtowiczmo.spik.network.lan.LanDiscoveryAsyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionActivity extends AppCompatActivity {

    /** Constants **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionActivity.class);

    static {
        System.setProperty("io.netty.noKeySetOptimization", "true");
    }

     /** UI Model **/

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if(progressDialog != null) {
            if(progressDialog.isShowing()) {
                progressDialog.hide();
            }

            progressDialog = null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connectOnLan(View view) {
        LOGGER.info("Trying to connect on LAN");

        try {
            final InetAddress wifiIp = getWifiIp();

            new LanDiscoveryAsyncTask(this).execute(wifiIp.getHostAddress());
        } catch (UnknownHostException e) {
            LOGGER.warn("Unable to get WIFI Ip", e);

            Toast.makeText(this, getString(R.string.unable_get_wifi_ip), Toast.LENGTH_LONG).show();
        }
    }

    public InetAddress getWifiIp() throws UnknownHostException {
        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        byte[] bytes = BigInteger.valueOf(ip).toByteArray();
        return InetAddress.getByAddress(bytes);
    }
}
