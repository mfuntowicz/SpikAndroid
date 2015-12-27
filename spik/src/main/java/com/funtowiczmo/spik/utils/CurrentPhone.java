package com.funtowiczmo.spik.utils;

import android.os.Build;
import com.polytech.spik.domain.Phone;

import java.net.InetSocketAddress;

/**
 * Created by momo- on 02/11/2015.
 */
public class CurrentPhone extends Phone {

    public CurrentPhone(InetSocketAddress address) {
        super(
                Build.MODEL,
                Build.MANUFACTURER,
                Build.MODEL,
                "Android",
                Build.VERSION.SDK_INT,
                address.getHostString(),
                address.getPort()
        );
    }
}
