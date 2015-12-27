package com.funtowiczmo.spik.utils;

import android.os.Build;
import com.polytech.spik.domain.Phone;

/**
 * Created by momo- on 02/11/2015.
 */
public class CurrentPhone extends Phone {

    public static final Phone CURRENT_PHONE = new CurrentPhone();

    private CurrentPhone() {
        super(Build.MODEL, Build.MANUFACTURER, Build.MODEL, "Android", Build.VERSION.SDK_INT, null, 1);
    }
}
