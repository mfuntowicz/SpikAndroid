package com.funtowiczmo.spik.service;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.utils.CursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.service.RoboService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

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
    public long sendMessage(long tid, Collection<String> participants, String text) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Sending message: (participants.length={}, text.length={})");

        if (participants != null) {
            if (participants.size() > 0) {
                final String SENT = "SMS_SENT";

                final ArrayList<String> smsParts = smsManager.divideMessage(text);
                final SmsSentReceiver smsSentReceiver = new SmsSentReceiver(tid, smsParts.size());

                final PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
                final ArrayList<PendingIntent> sentIntents = new ArrayList<>();

                LOGGER.debug("Sms converted into {} parts", smsParts.size());

                for (int i = 0; i < smsParts.size(); i++) {
                    sentIntents.add(sentPI);
                }

                registerReceiver(smsSentReceiver, new IntentFilter(SENT));

                for (String participant : participants) {
                    smsManager.sendMultipartTextMessage(participant, null, smsParts, sentIntents, null);
                }
            }
        }

        return -1;
    }

    @Override
    public Iterable<? extends Conversation> getConversations() {
        return spikContext.messageRepository().getConversations();
    }

    @Override
    public CursorIterator<Contact> getContacts() {
        return spikContext.contactRepository().getContacts();
    }


    /**
     * Listen for Sms Part sending events
     */
    private class SmsSentReceiver extends BroadcastReceiver {

        private final long messageId;
        private final AtomicInteger partsCounter;

        public SmsSentReceiver(long messageId, int parts) {
            this.messageId = messageId;
            this.partsCounter = new AtomicInteger(parts);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    if (partsCounter.decrementAndGet() == 0) {
                        LOGGER.info("Received Sent Part Event, remaining {}", partsCounter.get());
                        sendResult(Message.State.SENT);
                    }
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    LOGGER.info("Sms was not sent, errno : {}", getResultCode());
                    sendResult(Message.State.FAILED);
                    break;
            }
        }

        private void sendResult(Message.State state) {
            //Remove receiver
            DefaultSpikService.this.unregisterReceiver(this);
        }
    }
}
