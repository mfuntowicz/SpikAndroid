package com.funtowiczmo.spik.service;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.funtowiczmo.spik.lang.RemoteComputer;
import com.polytech.spik.protocol.SpikMessages;
import com.polytech.spik.sms.service.LanSmsClient;
import com.polytech.spik.sms.service.LanSmsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by momo- on 27/10/2015.
 */
public class LanSpikService extends AbstractSpikService  {

    /** Intent related **/
    public static final String COMPUTER_NAME_EXTRA = "name";
    public static final String COMPUTER_OS_EXTRA = "os";
    public static final String COMPUTER_OS_VERSION_EXTRA = "version";
    public static final String COMPUTER_IP_EXTRA = "ip";
    public static final String COMPUTER_PORT_EXTRA = "port";


    private static final Logger LOGGER = LoggerFactory.getLogger(LanSpikService.class);

    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final LanSmsClient client;

    private LanComputer computer;

    public static Intent fillIntent(Intent i, String name, String os, String version, String ip, int port) throws IllegalArgumentException {

        i.putExtra(LanSpikService.COMPUTER_NAME_EXTRA, name);
        i.putExtra(LanSpikService.COMPUTER_OS_EXTRA, os);
        i.putExtra(LanSpikService.COMPUTER_OS_VERSION_EXTRA, version);
        i.putExtra(LanSpikService.COMPUTER_IP_EXTRA, ip);
        i.putExtra(LanSpikService.COMPUTER_PORT_EXTRA, port);

        return i;
    }

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder = super.onBind(intent);

        if(isStarted.compareAndSet(false, true)){
            try {
                client.connect(computer.ip(), computer.port());
            } catch (InterruptedException e) {
                LOGGER.warn("Unable to connect to {}:{} -> {}", computer.ip(), computer.port(), e.getMessage());
                stopSpik();
            }
        }

        return binder;
    }

    @Override
    public void onDestroy() {
        try {
            stopSpik();
        } finally {
            super.onDestroy();
        }
    }

    @Override
    protected RemoteComputer initService(Intent intent) {
        computer =  new LanComputer(
            intent.getStringExtra(COMPUTER_NAME_EXTRA),
            intent.getStringExtra(COMPUTER_OS_EXTRA),
            intent.getStringExtra(COMPUTER_OS_VERSION_EXTRA),
            intent.getStringExtra(COMPUTER_IP_EXTRA),
            intent.getIntExtra(COMPUTER_PORT_EXTRA, 0)
        );

        return computer;
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

    @Override
    public void disconnect() {
        if(client != null) {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.warn("Got exception while closing LanSpikService", e);
            }
        }
    }


    /**
     * Represents a computer connected on a TCP socket
     */
    private final class LanComputer implements RemoteComputer {

        private final String name;
        private final String os;
        private final String version;
        private final InetSocketAddress address;

        public LanComputer(String name, String os, String version, String ip, int port) {
            this.name = name;
            this.os = os;
            this.version = version;
            this.address = new InetSocketAddress(ip, port);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String os() {
            return os;
        }

        @Override
        public String version() {
            return version;
        }

        public String ip(){
            return address.getHostString();
        }

        public int port(){
            return address.getPort();
        }

        public SocketAddress adress(){
            return address;
        }

        @Override
        public String toString() {
            return "LanComputer{" +
                    "name='" + name + '\'' +
                    ", os='" + os + '\'' +
                    ", version='" + version + '\'' +
                    ", address=" + address +
                    '}';
        }
    }
}
