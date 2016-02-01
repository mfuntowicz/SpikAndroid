package com.funtowiczmo.spik.service;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import com.funtowiczmo.spik.BuildConfig;
import com.funtowiczmo.spik.ConnectionActivity;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.*;
import com.funtowiczmo.spik.repositories.observers.MessageObserver;
import com.funtowiczmo.spik.utils.CursorIterator;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.polytech.spik.protocol.SpikMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.service.RoboService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by momo- on 27/10/2015.
 */
public abstract class AbstractSpikService extends RoboService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpikService.class);

    /** Notifications related **/
    private static final int CONNECTED_NOTIFICATION_ID = 1;
    private static final int DISCONNECTED_NOTIFICATION_ID = 2;

    /**
     * Spik resouces
     **/
    private final AtomicBoolean isInForeground = new AtomicBoolean(false);
    private MessageObserver observer;

    /**
     * System resources
     **/
    @Inject
    private NotificationManager notificationManager;
    private SmsManager smsManager = SmsManager.getDefault();

    private SpiKServiceBinder binder = new SpiKServiceBinder();
    private RemoteComputer computer;

    @Inject
    private SpikContext spikContext;

    //Contains ids of conversations already sent to the remote
    private Set<Long> knownConversation = new HashSet<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Starting Spik Service");

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
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Binding Spik Service");

        computer = initService(intent);
        observer = new SpikMessageObserver(computer.name() + " Observer");

        return binder;
    }

    /**
     * The computer this service is connected to
     * @return
     */
    public RemoteComputer computer(){
        return computer;
    }

    /**
     * Retrieve all the information about the remote computer from the intent
     * @param intent
     * @return
     */
    protected abstract RemoteComputer initService(Intent intent);

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

            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getResources().getString(R.string.local_service_started, computer.name()))
                    .setTicker(computer.name())
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                builder.setCategory(Notification.CATEGORY_SERVICE);

            Notification notification = builder.build();

            notification.flags = Notification.FLAG_NO_CLEAR;
            startForeground(CONNECTED_NOTIFICATION_ID, notification);


            LOGGER.debug("Starting to send information in a background thread");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendConversations();
                    sendContacts();
                }
            }, "SpikService Initialization Thread").start();
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

            //Stop the service
            stopForeground(true);
            stopSelf();

            //Show notification that we're now disconnected
            showDisconnectedNotification();
        } else {
            LOGGER.warn("Trying to stop a service not running");
        }
    }

    private void sendConversations() {
        for (Conversation c : spikContext.messageRepository().getConversations()) {
            if (knownConversation.add(c.spikId())) {
                for (String address : c.participants()) {

                    Contact contact = spikContext.contactRepository().getContactByPhone(address);
                    if (contact != null)
                        sendContact(contact);
                    else
                        LOGGER.warn("Repository returned null contact with phone {}", address);
                }

                sendConversation(c);
            }
        }
    }

    private void sendContacts() {
        CursorIterator<Contact> it = spikContext.contactRepository().getContacts();

        while (it.hasNext()) {
            Contact contact = it.next();
            if (contact != null)
                sendContact(contact);
            else
                LOGGER.warn("Repository returned null contact");
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

        SpikMessages.Conversation.Builder msg = SpikMessages.Conversation.newBuilder().setId(c.spikId());

        for (String recipient : c.participants()) {
            Contact contact = spikContext.contactRepository().getContactByPhone(recipient);

            msg.addParticipants(contact.id());
        }

        try(CursorIterator<Message> it = c.messages(this)){
            while (it.hasNext()) {
                Message message = it.next();
                if(message != null) {
                    msg.addMessages(
                        SpikMessages.Sms.newBuilder()
                            .setDate(message.id())
                            .setRead(message.isRead())
                            .setText(message.text())
                            .setStatus(
                                message.state() == Message.State.RECEIVED ?
                                    SpikMessages.Status.READ :
                                        message.state() == Message.State.SENT ?
                                        SpikMessages.Status.SENT : SpikMessages.Status.SENDING
                            )
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error while iterating on message for conversation " + c.id(), e);
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

    public abstract void disconnect();


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

    /**
     * Service Binder
     */
    public class SpiKServiceBinder extends Binder{
        public AbstractSpikService getService(){
            return AbstractSpikService.this;
        }
    }


    /**
     * Observer for the messages database
     */
    public class SpikMessageObserver extends MessageObserver{

        public SpikMessageObserver(String name) {
            super(name, Telephony.MmsSms.CONTENT_URI, spikContext.messageRepository());
        }

        @Override
        protected void onMessage(ThreadedMessage tm) {
            LOGGER.info("Received message {}", tm);

            //If the conversation is unknown send all the conversation
            if (knownConversation.add(tm.thread())) {
                try {
                    LOGGER.info("Conversation {} was not in the cache, send it to desktop", tm.thread());

                    Conversation conversation = spikContext.messageRepository().getConversationBySpikId(tm.thread());
                    sendConversation(conversation);
                } catch (Exception e) {
                    LOGGER.warn("Unable to send conversation with id {}", tm.thread());
                }
            } else { //Else send only the new message
                SpikMessages.Sms.Builder sms = SpikMessages.Sms.newBuilder()
                        .setDate(tm.message().id())
                        .setRead(tm.message().isRead())
                        .setStatus(SpikMessages.Status.NOT_READ)
                        .setText(tm.message().text())
                        .setThreadId(tm.thread());

                lowSend(SpikMessages.Wrapper.newBuilder().setSms(sms));
            }
        }
    }
}
