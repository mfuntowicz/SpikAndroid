package com.funtowiczmo.spik.service;

import android.content.Intent;
import com.polytech.spik.domain.Computer;
import com.polytech.spik.protocol.SpikMessages;
import com.polytech.spik.sms.service.LanSmsClient;
import com.polytech.spik.sms.service.LanSmsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by momo- on 27/10/2015.
 */
public class LanSpikService extends AbstractSpikService  {

    /** Lan Service Constants **/
    public static final String COMPUTER_NAME_EXTRA = "name";
    public static final String COMPUTER_OS_EXTRA = "os";
    public static final String COMPUTER_OS_VERSION_EXTRA = "version";
    public static final String COMPUTER_IP_EXTRA = "ip";
    public static final String COMPUTER_PORT_EXTRA = "port";

    private static final Logger LOGGER = LoggerFactory.getLogger(LanSpikService.class);

    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final LanSmsClient client;

    public LanSpikService() {
        client = new LanSmsClient(new LanSmsHandler() {
            @Override
            public void onConnected() {
                LOGGER.trace("Channel is now connected");
                launchSpik();
            }

            @Override
            public void onDisconnected() {
                LOGGER.trace("Channel is now disconnected");
                stopSpik();
            }

            @Override
            public void onSendMessage(SpikMessages.SendMessage msg) {
                String[] participants = new String[msg.getParticipantsCount()];
                msg.getParticipantsList().toArray(participants);

                sendMessage(msg.getMid(), participants, msg.getText());
            }

            @Override
            public void onContactReceived(SpikMessages.Contact contact) {
            }

            @Override
            public void onConversationReceive(SpikMessages.Conversation conversation) {
            }

            @Override
            public void onMessage(SpikMessages.Sms sms) {
            }

            @Override
            public void onStatusChanged(SpikMessages.StatusChanged statusChanged) {
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(isStarted.compareAndSet(false, true)){
            final String name = intent.getStringExtra(COMPUTER_NAME_EXTRA);
            final String os = intent.getStringExtra(COMPUTER_OS_EXTRA);
            final String version = intent.getStringExtra(COMPUTER_OS_VERSION_EXTRA);
            final String ip = intent.getStringExtra(COMPUTER_IP_EXTRA);
            final int port = intent.getIntExtra(COMPUTER_PORT_EXTRA, -1);

            computer = new Computer(name, os, version, ip, port);

            if(ip == null) {
                LOGGER.error("IP is null, cannot start -> aborting");
                stopSelf();
            }else if(port < 0){
                LOGGER.error("port < 0 ({}), cannot start -> aborting", port);
                stopSelf();
            }else{
                try {
                    client.connect(computer.ip(), computer.port());
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

    @Override
    protected void lowSend(SpikMessages.WrapperOrBuilder msg) {
        if (client != null) {
            LOGGER.debug("Sending message to the computer");
            client.lowSend(msg);
        } else {
            LOGGER.debug("Client is null, unable to send message to the computer");
        }
    }
}
