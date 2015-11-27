package com.funtowiczmo.spik;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.funtowiczmo.spik.lan.discovery.LanDiscoveryClient;
import com.funtowiczmo.spik.network.lan.LanDiscoveryClientCallbackImpl;
import com.funtowiczmo.spik.service.LanSpikService;
import com.funtowiczmo.spik.sms.lang.Computer;
import com.funtowiczmo.spik.ui.ComputerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.funtowiczmo.spik.network.lan.LanDiscoveryClientCallbackImpl.DISCOVERY_ENDED_HANDLER_MSG;
import static com.funtowiczmo.spik.network.lan.LanDiscoveryClientCallbackImpl.DISCOVERY_STARTED_HANDLER_MSG;

public class ConnectionActivity extends AppCompatActivity {

    /** Constants **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionActivity.class);

    static {
        System.setProperty("io.netty.noKeySetOptimization", "true");
    }

    /**
     * Service reference
     **/
    Intent serviceIntent = null;
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

        if(serviceIntent != null) {
            stopService(serviceIntent);
            serviceIntent = null;
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

    public void handleLanConnection(View view) {
        LOGGER.info("Trying to connect on LAN");

        /**
         * Handler to communicate from background thread and UI thread
         */
        final Handler handler = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what){
                    case DISCOVERY_STARTED_HANDLER_MSG:
                        if(progressDialog != null)
                            progressDialog.dismiss();

                        progressDialog = new ProgressDialog(ConnectionActivity.this);
                        progressDialog.setIndeterminate(true);
                        progressDialog.setTitle(R.string.searching_hosts);
                        progressDialog.setMessage(getString(R.string.please_wait));
                        progressDialog.show();


                        break;
                    case DISCOVERY_ENDED_HANDLER_MSG:
                        final List<Computer> computers = (List<Computer>) message.obj;

                        if (computers != null) {

                            AlertDialog.Builder dialog = new AlertDialog.Builder(ConnectionActivity.this);

                            if(computers.size() > 0) {
                                LOGGER.info("{} Hosts found", computers.size());
                                dialog.setAdapter(new ComputerAdapter(ConnectionActivity.this, computers), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        final Computer computer = computers.get(i);
                                        final Intent intent = new Intent(ConnectionActivity.this, LanSpikService.class);

                                        intent.putExtra(LanSpikService.COMPUTER_NAME_EXTRA, computer.name());
                                        intent.putExtra(LanSpikService.COMPUTER_OS_EXTRA, computer.os());
                                        intent.putExtra(LanSpikService.COMPUTER_IP_EXTRA, computer.ip());
                                        intent.putExtra(LanSpikService.COMPUTER_PORT_EXTRA, computer.port());

                                        LOGGER.info("Selected ic_computer {}", computer);

                                        if(serviceIntent == null)
                                            serviceIntent = intent;

                                        startService(serviceIntent);
                                    }
                                }).setTitle(R.string.hosts_found);
                            }else{
                                LOGGER.info("No host found");
                                dialog.setTitle(R.string.no_host_found).setMessage(R.string.no_host_help);
                            }

                            progressDialog.dismiss();
                            dialog.show();
                        }
                        break;
                }

                return false;
            }
        });


        new Thread(new Runnable() {
            @Override
            public void run() {
                try(LanDiscoveryClient client = new LanDiscoveryClient(new LanDiscoveryClientCallbackImpl(handler))){
                    client.connect();
                    client.sendDiscoveryRequest();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
