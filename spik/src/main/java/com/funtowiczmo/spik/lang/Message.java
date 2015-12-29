package com.funtowiczmo.spik.lang;

import android.provider.Telephony;
import org.roboguice.shaded.goole.common.base.Optional;

/**
 * Created by momo- on 25/11/2015.
 */
public class Message {

    private long id;
    private String text;
    private boolean read;
    private State state;
    private Optional<Attachment> attachment;

    public Message(long id, String text, boolean read, State state) {
        this(id, text, read, state, null, null);
    }

    public Message(long id, String text, boolean read, State state, byte[] attachment, String mimeType) {
        this.id = id;
        this.text = text;
        this.read = read;
        this.state = state;

        if(attachment != null)
            this.attachment = Optional.of(new Attachment(attachment, mimeType));
        else
            this.attachment = Optional.absent();
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

    public Optional<Attachment> attachment(){
        return attachment;
    }

    public static class Attachment{
        private final byte[] data;
        private final String mimeType;

        public Attachment(byte[] data, String mimeType) {
            this.data = data;
            this.mimeType = mimeType;
        }

        public byte[] data(){
            return data;
        }

        public int length(){
            return data == null ? 0 : data.length;
        }

        public String mimeType(){
            return mimeType;
        }

        @Override
        public String toString() {
            return "Attachment{" +
                    "data=" + length() +
                    ", mimeType='" + mimeType + '\'' +
                    '}';
        }
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
