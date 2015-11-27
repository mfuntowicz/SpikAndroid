package com.funtowiczmo.spik.service;

import android.content.Intent;
import com.funtowiczmo.spik.lan.LanSpikClient;
import com.funtowiczmo.spik.protocol.ServiceMessages;
import com.funtowiczmo.spik.sms.lang.Computer;
import com.funtowiczmo.spik.sms.listeners.AbstractSpikServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by momo- on 27/10/2015.
 */
public class LanSpikService extends AbstractSpikService  {

    /** Lan Service Constants **/
    public static final String COMPUTER_NAME_EXTRA = "name";
    public static final String COMPUTER_OS_EXTRA = "os";
    public static final String COMPUTER_IP_EXTRA = "ip";
    public static final String COMPUTER_PORT_EXTRA = "port";
    private static final Logger LOGGER = LoggerFactory.getLogger(LanSpikService.class);
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final LanSpikClient client;

    public LanSpikService() {
        client = new LanSpikClient(new AbstractSpikServiceListener() {

            @Override
            public void onConnected() {
                launchSpik(client);
            }

            @Override
            public void onExceptionOccured(Throwable t) {
                t.printStackTrace();
                LOGGER.warn("Connection exception {}", t.getMessage());
            }

            @Override
            public void onDisconnected() {
                LOGGER.info("Disconnected");
                showDisconnectedNotification();
                stopSpik();
            }

            @Override
            public void handleSendMessage(ServiceMessages.ServiceMessage.SendMessage msg) {
                String[] participants = new String[msg.getParticipantsCount()];
                msg.getParticipantsList().toArray(participants);

                sendMessage(msg.getMid(), participants, msg.getText());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(isStarted.compareAndSet(false, true)){
            final String name = intent.getStringExtra(COMPUTER_NAME_EXTRA);
            final String os = intent.getStringExtra(COMPUTER_OS_EXTRA);
            final String ip = intent.getStringExtra(COMPUTER_IP_EXTRA);
            final int port = intent.getIntExtra(COMPUTER_PORT_EXTRA, -1);

            computer = new Computer(name, os, ip, port);

            if(ip == null) {
                LOGGER.error("IP is null, cannot start -> aborting");
                stopSelf();
            }else if(port < 0){
                LOGGER.error("port < 0 ({}), cannot start -> aborting", port);
                stopSelf();
            }else{
                try {
                    client.connect(new InetSocketAddress(computer.ip(), computer.port()));
                } catch (InterruptedException e) {
                    LOGGER.warn("Unable to connect to {}:{} -> {}", ip, port, e.getMessage());
                    stopSelf();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        try {
            if (client != null)
                client.close();
        } catch (IOException e) {
            LOGGER.warn("Caught an exception", e);
        } finally {
            super.onDestroy();
        }
    }
}
