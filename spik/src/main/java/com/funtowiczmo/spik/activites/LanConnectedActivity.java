package com.funtowiczmo.spik.activites;

import android.content.Intent;
import android.os.Bundle;
import com.funtowiczmo.spik.service.LanSpikService;

/**
 * Created by mfuntowicz on 31/12/15.
 */
public class LanConnectedActivity extends ConnectedActivity {


    private SpikServiceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = new Intent(LanConnectedActivity.this, LanSpikService.class);
        intent.putExtras(getIntent().getExtras());

        connection = new SpikServiceConnection();

        bindSpikService(intent, connection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connection = null;
    }
}
