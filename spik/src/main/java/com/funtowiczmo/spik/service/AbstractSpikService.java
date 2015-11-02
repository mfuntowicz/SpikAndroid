package com.funtowiczmo.spik.service;

import android.app.*;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.funtowiczmo.spik.ConnectionActivity;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.lang.Computer;
import com.funtowiczmo.spik.lang.Phone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by momo- on 27/10/2015.
 */
public abstract class AbstractSpikService extends Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpikService.class);

    private static final int CONNECTED_NOTIFICATION_ID = 1;
    private static final int DISCONNECTED_NOTIFICATION_ID = 2;

    /** System resources **/
    private NotificationManager notificationManager;

    /** Spik resouces **/
    private final AtomicBoolean isInForeground = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        LOGGER.info("Creating Service");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

    protected void launchSpik(Computer computer){
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
        }
    }

    protected void stopSpik(){
        LOGGER.info("Stopping Spik Service");
        if(isInForeground.compareAndSet(true, false)){
            stopForeground(true);
            stopSelf();
        }
    }
}
