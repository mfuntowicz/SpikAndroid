package com.funtowiczmo.spik.service;

import android.content.Intent;
import com.funtowicz.spik.sms.transport.listeners.SpikClientListener;
import com.funtowiczmo.spik.lan.LanSpikClient;
import com.funtowiczmo.spik.lang.Computer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by momo- on 27/10/2015.
 */
public class LanSpikService extends AbstractSpikService  {

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    /** Lan Service Constants **/
    public static final String COMPUTER_NAME_EXTRA = "name";
    public static final String COMPUTER_OS_EXTRA = "os";
    public static final String COMPUTER_IP_EXTRA = "ip";
    public static final String COMPUTER_PORT_EXTRA = "port";


    private static final Logger LOGGER = LoggerFactory.getLogger(LanSpikService.class);
    private final LanSpikClient client;

    public LanSpikService() {
        client = new LanSpikClient(new SpikClientListener() {
            @Override
            public void onConnected(Computer computer) {
                LOGGER.info("Connected to computer {}", computer);
                launchSpik(computer, client);
            }

            @Override
            public void onConnectionException(Computer computer, Throwable throwable) {
                throwable.printStackTrace();
                LOGGER.warn("Connection exception {}, {}", computer, throwable.getMessage());
            }

            @Override
            public void onDisconnected(Computer computer) {
                LOGGER.info("Disconnected from computer {}", computer);
                showDisconnectedNotification(computer);
                stopSpik();
            }

            @Override
            public void onSendSmsMessage(long tid, String[] participants, String text) {
                sendMessage(tid, participants, text);
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

            final Computer remote = new Computer(name, os, ip, port);

            if(ip == null) {
                LOGGER.error("IP is null, cannot start -> aborting");
                stopSelf();
            }else if(port < 0){
                LOGGER.error("port < 0 ({}), cannot start -> aborting", port);
                stopSelf();
            }else{
                try {
                    client.connect(remote);
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
