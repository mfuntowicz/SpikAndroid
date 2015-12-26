package com.funtowiczmo.spik.service;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import com.funtowiczmo.spik.BuildConfig;
import com.funtowiczmo.spik.ConnectedActivity;
import com.funtowiczmo.spik.ConnectionActivity;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.lang.ThreadedMessage;
import com.funtowiczmo.spik.repositories.observers.MessageObserver;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.polytech.spik.domain.Computer;
import com.polytech.spik.protocol.SpikMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    /**
     * Spik resouces
     **/
    private final AtomicBoolean isInForeground = new AtomicBoolean(false);
    protected Computer computer;
    private MessageObserver observer;

    /**
     * System resources
     **/
    @Inject
    private NotificationManager notificationManager;

    @Inject
    private SpikContext spikContext;

    private SmsManager smsManager = SmsManager.getDefault();

    @Override
    public void onCreate() {
        super.onCreate();

        observer = new MessageObserver("Test Observer", Telephony.MmsSms.CONTENT_URI, spikContext.messageRepository()) {
            @Override
            protected void onMessage(ThreadedMessage tm) {
                LOGGER.info("Received message {}", tm);

                SpikMessages.Sms.Builder sms = SpikMessages.Sms.newBuilder()
                        .setDate(tm.message().id())
                        .setRead(tm.message().isRead())
                        .setStatus(SpikMessages.Status.NOT_READ)
                        .setText(tm.message().text())
                        .setThreadId(tm.thread());

                lowSend(SpikMessages.Wrapper.newBuilder().setSms(sms));
            }
        };

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
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Show a notification about the disconnection
     */
    protected void showDisconnectedNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), new Intent(this, ConnectionActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.local_service_finished, computer.name()))
                .setTicker("Disconnected")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(DISCONNECTED_NOTIFICATION_ID, notification);
    }

    protected void launchSpik() {
        LOGGER.info("Starting Spik Service, connecting to {}", computer);

        if (isInForeground.compareAndSet(false, true)) {

            //Register content observer for SMS/MMS
            spikContext.messageRepository().registerObserver(observer);

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


            LOGGER.debug("Starting to send information in a background thread");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendConversations();
                }
            }, "SpikService Initialization Thread").start();


            //Let's start ConnectedActivity
            LOGGER.debug("Launching ConnectedActivity");

            Intent i = new Intent(getApplicationContext(), ConnectedActivity.class);

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            i.putExtra(ConnectedActivity.COMPUTER_NAME_EXTRA, computer.name());

            startActivity(i);
        }
    }

    /**
     * Stop the service if running
     */
    protected void stopSpik() {
        if (isInForeground.compareAndSet(true, false)) {
            LOGGER.info("Stopping Spik Service");

            //Remove observer
            getContentResolver().unregisterContentObserver(observer);

            stopForeground(true);
            stopSelf();
        } else {
            LOGGER.warn("Trying to stop a service not running");
        }
    }

    protected void sendConversations() {
        try (LazyCursorIterator<Conversation> it = spikContext.messageRepository().getConversations()) {
            while (it.hasNext()) {
                final Conversation c = it.next();
                for (Contact contact : c.participants()) {
                    sendContact(contact);
                }

                sendConversation(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void sendMessage(long tid, String[] participants, String text) {
        if (BuildConfig.DEBUG) {
            if (participants.length <= 0)
                throw new RuntimeException("No participants provided");

            if (text == null)
                throw new RuntimeException("No text in the message");
        }

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

    /**
     * Send a contact to the remote
     * @param c
     */
    private void sendContact(Contact c) {
        LOGGER.info("Sending contact {}", c.id());

        SpikMessages.Contact.Builder msg = SpikMessages.Contact.newBuilder()
                .setId(c.id())
                .setName(c.name())
                .setPhone(c.phone());

        if (c.hasPhoto())
            msg.setPicture(ByteString.copyFrom(c.photo()));

        lowSend(SpikMessages.Wrapper.newBuilder().setContact(msg));
    }

    /**
     * Send a conversation to the remote
     *
     * @param c
     */
    private void sendConversation(Conversation c) {

        SpikMessages.Conversation.Builder msg = SpikMessages.Conversation.newBuilder().setId(c.id());

        for (Contact contact : c.participants()) {
            msg.addParticipants(contact.id());
        }

        for (Message message : c.messages()) {
            msg.addMessages(
                    SpikMessages.Sms.newBuilder()
                            .setDate(message.id())
                            .setRead(message.isRead())
                            .setText(message.text())
                            .setStatus(
                                    message.state() == Message.State.RECEIVED ?
                                            SpikMessages.Status.READ :
                                            message.state() == Message.State.SENT ? SpikMessages.Status.SENT : SpikMessages.Status.SENDING
                            )
            );
        }

        lowSend(SpikMessages.Wrapper.newBuilder().setConversation(msg));
    }

    private void sendMessageStateChanged(long mId, Message.State state) {
        LOGGER.info("Sending new state ({}) for message {}", state, mId);

        SpikMessages.StatusChanged.Builder msg =
                SpikMessages.StatusChanged.newBuilder()
                        .setMid(mId);

        switch (state) {
            case DRAFT:
            case FAILED:
            case PENDING:
            case QUEUED:
                msg.setStatus(SpikMessages.Status.SENDING);
                break;
            case RECEIVED:
                msg.setStatus(SpikMessages.Status.READ);
                break;
            case SENT:
                msg.setStatus(SpikMessages.Status.SENT);
                break;
        }

        lowSend(SpikMessages.Wrapper.newBuilder().setStatusChanged(msg));
    }

    protected abstract void lowSend(SpikMessages.WrapperOrBuilder msg);


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

        private void sendResult(Message.State state) {
            //Remove receiver
            AbstractSpikService.this.unregisterReceiver(this);

            //Send result to server
            sendMessageStateChanged(messageId, state);
        }
    }
}
