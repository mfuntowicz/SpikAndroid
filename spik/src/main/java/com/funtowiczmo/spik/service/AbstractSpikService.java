package com.funtowiczmo.spik.service;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import com.funtowicz.spik.sms.transport.SpikClient;
import com.funtowiczmo.spik.ConnectionActivity;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.Computer;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.utils.CurrentPhone;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.service.RoboService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by momo- on 27/10/2015.
 */
public abstract class AbstractSpikService extends RoboService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpikService.class);

    private static final int CONNECTED_NOTIFICATION_ID = 1;
    private static final int DISCONNECTED_NOTIFICATION_ID = 2;

    /** System resources **/
    @Inject
    private NotificationManager notificationManager;
    private SmsManager smsManager = SmsManager.getDefault();

    /** Spik resouces **/
    private final AtomicBoolean isInForeground = new AtomicBoolean(false);

    private SpikContext spikContext;
    private SpikClient spikClient;

    @Override
    public void onCreate() {
        super.onCreate();

        spikContext = RoboGuice.getInjector(this).getInstance(SpikContext.class);
        LOGGER.info("Spik Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LOGGER.info("Destroying Service");
        stopSpik();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    /**
     * Show a notification about the disconnection
     * @param c
     */
    protected void showDisconnectedNotification(Computer c){
        PendingIntent contentIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), new Intent(this, ConnectionActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.local_service_finished, c.name()))
                .setTicker(c.name())
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(DISCONNECTED_NOTIFICATION_ID, notification);
    }

    protected void launchSpik(Computer computer, SpikClient client){
        LOGGER.info("Starting Spik Service");

        if(isInForeground.compareAndSet(false, true)) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), new Intent(this, ConnectionActivity.class), 0);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getResources().getString(R.string.local_service_started, computer.name()))
                    .setTicker(computer.name())
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build();

            notification.flags = Notification.FLAG_NO_CLEAR;
            startForeground(CONNECTED_NOTIFICATION_ID, notification);

            spikClient = client;

            client.sendHello(CurrentPhone.CURRENT_PHONE);
            sendConversations();
        }
    }

    protected void stopSpik(){
        LOGGER.info("Stopping Spik Service");
        if(isInForeground.compareAndSet(true, false)){
            spikClient = null;
            stopForeground(true);
            stopSelf();
        }
    }

    protected void sendConversations() {
        try(LazyCursorIterator<Conversation> it = spikContext.messageRepository().getConversations()){
            while (it.hasNext()){
                final Conversation c = it.next();
                for (Contact contact : c.participants()) {
                    spikClient.sendContact(contact);
                }

                spikClient.sendConversation(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void sendMessage(long tid, String[] participants, String text){
        assert participants.length > 0 : "No participants provided";
        assert text != null : "No text in the message";

        LOGGER.info("Sending message to {}", Arrays.toString(participants));

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

    protected void sendMessageStateChanged(long tid, Message.State state){
        LOGGER.info("Sending new state ({}) for message {}", state, tid);

        spikClient.sendMessageStateChanged(tid, state);
    }

    /**
     * Listen for Sms Part sending events
     */
    private class SmsSentReceiver extends BroadcastReceiver{

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
                    if(partsCounter.decrementAndGet() == 0){
                        LOGGER.debug("Received Sent Part Event, remaining {}", partsCounter.get());
                        sendResult(Message.State.SENT);
                    }
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    LOGGER.debug("Sms was not sent, errno : {}", getResultCode());
                    sendResult(Message.State.FAILED);
                    break;
            }
        }

        private void sendResult(Message.State state){
            //Remove receiver
            AbstractSpikService.this.unregisterReceiver(this);

            //Send result to server
            sendMessageStateChanged(messageId, state);
        }
    }
}
