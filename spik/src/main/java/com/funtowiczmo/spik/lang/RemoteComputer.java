package com.funtowiczmo.spik.lang;

/**
 * Created by mfuntowicz on 31/12/15.
 */
public interface RemoteComputer {

    /**
     * Name of the remote computer
     * @return
     */
    String name();

    /**
     * Name of the OS
     * @return
     */
    String os();

    /**
     * Version of the OS
     * @return
     */
    String version();
}
