package com.funtowiczmo.spik.utils;

import android.os.Build;
import com.polytech.spik.domain.Phone;

/**
 * Created by momo- on 02/11/2015.
 */
public class CurrentPhone extends Phone {

    private static final String OS = "Android";

    public CurrentPhone(String ip) {
        super(Build.MODEL, Build.MANUFACTURER, Build.MODEL, OS, Build.VERSION.SDK_INT, ip, 1);
    }
}
