package com.funtowiczmo.spik.network.lan;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.activites.LanConnectedActivity;
import com.funtowiczmo.spik.service.LanSpikService;
import com.funtowiczmo.spik.ui.ComputerAdapter;
import com.funtowiczmo.spik.utils.CurrentPhone;
import com.polytech.spik.domain.Computer;
import com.polytech.spik.sms.discovery.LanDiscoveryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.net.InetAddress.getByAddress;

/**
 * Created by mfuntowicz on 31/12/15.
 */
public class LanDiscoveryAsyncTask extends AsyncTask<String, Void, List<Computer>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanDiscoveryAsyncTask.class);
    private final Context context;

    private ProgressDialog progressDialog;

    public LanDiscoveryAsyncTask(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(R.string.searching_hosts);
        progressDialog.setMessage(context.getString(R.string.please_wait));
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(final List<Computer> computers) {
        progressDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if(computers.size() > 0) {
            builder.setTitle(R.string.hosts_found);
            builder.setAdapter(new ComputerAdapter(context, computers), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Computer computer = computers.get(i);
                    Intent intent = new Intent(context, LanConnectedActivity.class);
                    LanSpikService.fillIntent(intent, computer.name(), computer.os(), computer.version(), computer.ip(), computer.port());

                    context.startActivity(intent);
                }
            });
        }else{
            builder.setTitle(R.string.no_host_found).setMessage(R.string.no_host_help);
        }

        builder.show();
    }

    @Override
    protected List<Computer> doInBackground(String... strings) {
        LanDiscoveryClient client = new LanDiscoveryClient();

        if(strings == null || strings.length < 1){
            LOGGER.warn("No ip provided (args: {})", Arrays.toString(strings));
            return Collections.emptyList();
        }

        String broadcast = getBroadcastIp();
        CurrentPhone phone = new CurrentPhone(strings[0]);

        return client.discover(broadcast, phone);
    }

    private String getBroadcastIp(){
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        String broadcastIp = "255.255.255.255";

        if(dhcp != null) {
            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int i = 0; i < 4; i++) {
                quads[i] = ((byte) ((broadcast >> (i * 8)) & 0xFF));
            }

            try {
                broadcastIp = getByAddress(quads).getHostAddress();
            } catch (UnknownHostException e) {
                LOGGER.warn("Unable to resolve broadcast IP", e);
            }
        }

        LOGGER.trace("Coomputed Broadcast IP: {}", broadcastIp);
        return broadcastIp;
    }
}
