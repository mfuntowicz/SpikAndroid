package com.funtowiczmo.spik.service;

import android.app.*;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.funtowiczmo.spik.ConnectionActivity;
import com.funtowiczmo.spik.R;
import com.funtowiczmo.spik.lang.Computer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by momo- on 27/10/2015.
 */
public abstract class AbstractSpikService extends Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpikService.class);

    private static final int CONNECTED_NOTIFICATION_ID = 1;
    private static final int DISCONNECTED_NOTIFICATION_ID = 2;

    /** System resources **/
    private NotificationManager notificationManager;
    private final IBinder mBinder = new SpikBinder();

    /** Spik resouces **/
    private Computer computer;

    protected AbstractSpikService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
       onDisconnected();
    }

    /**
     * Called when the service is connected to a computer
     * @param computer The computer the device is connected to
     */
    protected void onConnected(Computer computer){
        LOGGER.info("Connected to {}", computer);

        this.computer = computer;
        showConnectedNotification();
    }

    /**
     * Called when the service is disconnect from a computer
     */
    protected void onDisconnected(){
        LOGGER.info("Disconnected from {}", computer);

        final Computer ref = computer;

        dispose();
        showDisconnectedNotification(ref);
    }

    /**
     * Show a notification about the connection to a device
     */
    private void showConnectedNotification(){
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

        //Flag : RUNNING | NO CLEAR
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notificationManager.notify(CONNECTED_NOTIFICATION_ID, notification);
    }

    /**
     * Show a notification about the disconnection
     * @param c
     */
    private void showDisconnectedNotification(Computer c){
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

    /**
     * Free all resource used by this service
     */
    private void dispose(){
        notificationManager.cancel(CONNECTED_NOTIFICATION_ID);
        computer = null;
    }

    /** Local Spik Service Binder **/
    public class SpikBinder extends Binder {

        /**
         * Return the service
         * @return
         */
        AbstractSpikService getService() {
            return AbstractSpikService.this;
        }
    }
}
