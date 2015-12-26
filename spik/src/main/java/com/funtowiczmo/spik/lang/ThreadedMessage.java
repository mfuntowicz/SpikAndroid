package com.funtowiczmo.spik.lang;

/**
 * Created by mfuntowicz on 26/12/15.
 */
public class ThreadedMessage {

    private long thread;
    private Message message;

    public ThreadedMessage(long thread, Message message) {
        this.thread = thread;
        this.message = message;
    }

    public long thread() {
        return thread;
    }

    public Message message() {
        return message;
    }

    @Override
    public String toString() {
        return "ThreadedMessage{" +
                "thread=" + thread +
                ", message=" + message +
                '}';
    }
}
