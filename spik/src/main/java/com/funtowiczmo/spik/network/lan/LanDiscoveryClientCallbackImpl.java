package com.funtowiczmo.spik.network.lan;

/**
 * Created by momo- on 01/11/2015.
 */

import android.os.Handler;
import com.funtowiczmo.spik.lan.discovery.LanDiscoveryClientCallback;
import com.funtowiczmo.spik.sms.lang.Computer;

import java.util.Collection;

/**
 * Lan Discovery Callback implementation through a Handler.
 * Using a Handler allow us to communicate easily between background and UI Thread
 */
public class LanDiscoveryClientCallbackImpl implements LanDiscoveryClientCallback {

    public static final int DISCOVERY_STARTED_HANDLER_MSG = -1;
    public static final int DISCOVERY_ENDED_HANDLER_MSG = -2;

    private final Handler handler;

    public LanDiscoveryClientCallbackImpl(Handler handler) {
        this.handler = handler;
    }


    @Override
    public void onDiscoveryStarted() {
        handler.obtainMessage(DISCOVERY_STARTED_HANDLER_MSG).sendToTarget();
    }


    @Override
    public void onDiscoveryDone(Collection<Computer> collection) {
        handler.obtainMessage(DISCOVERY_ENDED_HANDLER_MSG, collection).sendToTarget();
    }
}
