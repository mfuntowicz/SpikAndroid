package com.funtowiczmo.spik.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.funtowicz.spik.sms.transport.SpikClient;
import com.funtowiczmo.spik.ConnectionActivity;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.context.SpikContext;
import com.funtowiczmo.spik.lang.Computer;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.utils.CurrentPhone;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.service.RoboService;

import java.util.concurrent.atomic.AtomicBoolean;

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

    private void sendConversations() {
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
}
