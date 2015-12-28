package com.funtowiczmo.spik.lang;

import android.provider.Telephony;

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
        FAILED, RECEIVED, SENT, QUEUED, PENDING, DRAFT;

        public static State fromStatus(int i){
            switch (i){
                case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                    return DRAFT;
                case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                case Telephony.Sms.MESSAGE_TYPE_SENT:
                    return SENT;
                case Telephony.Sms.MESSAGE_TYPE_QUEUED:
                    return QUEUED;
                case Telephony.Sms.MESSAGE_TYPE_INBOX:
                    return RECEIVED;
                case Telephony.Sms.STATUS_FAILED:
                    return FAILED;
                default:
                    return SENT;
            }
        }
    }
}
