package com.funtowiczmo.spik.activites;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.service.AbstractSpikService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConnectedActivity extends AppCompatActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedActivity.class);

    private final Object lock = new Object();
    private AbstractSpikService service;

    @Bind(R.id.connected_computer_desc)
    TextView description_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        ButterKnife.bind(this);
    }

    protected boolean bindSpikService(Intent i, SpikServiceConnection connection){
        return bindService(i, connection, BIND_AUTO_CREATE);
    }

    /**
     * Called when user ask for disconnection
     * @param view
     */
    public void disconnect(View view){
        synchronized (lock){
            if(service != null){
                service.disconnect();
            }
        }
    }


    protected class SpikServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AbstractSpikService.SpiKServiceBinder binder = ((AbstractSpikService.SpiKServiceBinder) iBinder);

            synchronized (lock) {
                ConnectedActivity.this.service = binder.getService();
            }

            onSpikServiceConnected(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LOGGER.info("Spik Service is now disconnected");

            synchronized (lock) {
                ConnectedActivity.this.service = null;
            }
        }

        protected void onSpikServiceConnected(AbstractSpikService service){
            description_view.setText(
                getString(R.string.connexion_information_details, service.computer().name())
            );
        }
    }
}
