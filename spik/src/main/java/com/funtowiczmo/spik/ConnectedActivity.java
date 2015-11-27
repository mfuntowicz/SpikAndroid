package com.funtowiczmo.spik;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;

public class ConnectedActivity extends AppCompatActivity {

    public static final String COMPUTER_NAME_EXTRA = "computer.name";

    @Bind(R.id.connected_computer_desc)
    TextView description_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        ButterKnife.bind(this);

        final Intent intent = getIntent();
        final String name = intent.getStringExtra(COMPUTER_NAME_EXTRA);

        description_view.setText(getResources().getString(R.string.connexion_information_details, name));
    }
}
