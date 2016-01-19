package com.funtowiczmo.spik.service;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.service.RoboService;

import java.util.Collection;

/**
 * Created by momo- on 19/01/2016.
 */
public class DefaultSpikService extends RoboService implements SpikService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSpikService.class);

    /**
     * System resources
     **/
    @Inject
    private NotificationManager notificationManager;
    private SmsManager smsManager = SmsManager.getDefault();
    private SpikContext spikContext;


    @Override
    public void onCreate() {
        super.onCreate();

        LOGGER.info("Spik Service Created");
        spikContext = RoboGuice.getInjector(this).getInstance(SpikContext.class);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("onBind() Service");

        return null;
    }


    /**
     * SpikService Implementation
     **/

    @Override
    public long sendMessage(Collection<String> participants, String text) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Sending message: (participants.length={}, text.length={})");

        return 0;
    }

    @Override
    public Iterable<Conversation> getConversations() {
        return null;
    }

    @Override
    public Iterable<Contact> getContacts() {
        return null;
    }
}
