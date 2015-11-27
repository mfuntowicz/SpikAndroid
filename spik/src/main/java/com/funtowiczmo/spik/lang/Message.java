package com.funtowiczmo.spik.lang;

/**
 * Created by momo- on 25/11/2015.
 */
public class Message {

    private long id;
    private String text;
    private boolean read;
    private State state;

    public Message(long id, String text, boolean read, State state) {
        this.id = id;
        this.text = text;
        this.read = read;
        this.state = state;
    }

    public long id() {
        return id;
    }

    public String text() {
        return text;
    }

    public boolean isRead() {
        return read;
    }

    public State state() {
        return state;
    }

    public enum State {
        FAILED, RECEIVED, SENT, QUEUED, PENDING, DRAFT

    }
}
