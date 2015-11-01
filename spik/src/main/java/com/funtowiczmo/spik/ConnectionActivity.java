package com.funtowiczmo.spik;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.modules.DefaultAndroidModule;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import com.google.inject.Guice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectionActivity extends AppCompatActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SpikContext context = Guice.createInjector(new DefaultAndroidModule(this)).getInstance(SpikContext.class);

        try(LazyCursorIterator<Conversation> it = context.messageRepository().getConversations()){
            if(it != null) {
                while (it.hasNext())
                    LOGGER.info("Conversation Found {}", it.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    }
}
