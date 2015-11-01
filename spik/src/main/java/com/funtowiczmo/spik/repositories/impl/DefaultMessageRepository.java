package com.funtowiczmo.spik.repositories.impl;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.repositories.ContactRepository;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static android.provider.Telephony.*;

/**
 * Created by momo- on 28/10/2015.
 */
public class DefaultMessageRepository implements MessageRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageRepository.class);

    /** Threads Constants **/
    private static final String[] THREADS_PROJECTION = new String[]{
        Sms.ADDRESS,
    };

    private static final int THREADS_RECIPIENT_IDX = 0;


    /** SMS Contants **/
    private static final String[] SMS_PROJECTION = new String[]{
        Sms.DATE,
        Sms.BODY,
        Sms.TYPE,
        Sms.READ,
    };

    private static final int SMS_DATE_IDX = 0;
    private static final int SMS_BODY_IDX = 1;
    private static final int SMS_TYPE_IDX = 2;
    private static final int SMS_READ_IDX = 3;


    /** MMS Constants **/
    private static final Uri MMS_PART_URI = Uri.parse("content://mms/part");
    private static final String MMS_ADDR_URI_BASE = "content://mms/{id}/addr";

    private static final String[] MMS_PROJECTION = new String[]{
        Mms._ID,
        Mms.DATE,
        Mms.THREAD_ID,
    };
    private static final int MMS_ID_IDX = 0;
    private static final int MMS_DATE_IDX = 1;

    private static final int MMS_TID_IDX = 2;

    /** MMS Part Constants **/
    private static final String[] MMS_PART_PROJECTION = new String[]{
        Mms.Part.CONTENT_TYPE,
        Mms.Part.CT_TYPE,
        Mms.Part.TEXT
    };
    private static final int MMS_PART_MIME_IDX = 0;
    private static final int MMS_PART_CT_IDX = 0;
    private static final int MMS_PART_TEXT_IDX = 1;

    /** Repositories **/
    private final ContentResolver repository;
    private final ContactRepository contactRepository;

    @Inject
    public DefaultMessageRepository(Context context, ContactRepository contactRepository) {
        repository = context.getContentResolver();
        this.contactRepository = contactRepository;
    }

    @Override
    @SuppressLint("Recycle")
    public LazyCursorIterator<Conversation> getConversations() {
        LOGGER.info("Trying to get all conversations");

        final String[] PROJECTION = new String[]{ "DISTINCT " + Mms.THREAD_ID, Sms.ADDRESS, Mms._ID };

        try(Cursor c = repository.query(MmsSms.CONTENT_CONVERSATIONS_URI, null, null, null, null)){
            if(c != null) {
                if(c.moveToFirst()) {
                    do {
                        long t_id = c.getLong(c.getColumnIndex(Mms.THREAD_ID));
                        String recipients = c.getString(c.getColumnIndex(Sms.ADDRESS));

                        if(recipients == null){
                            final String m_id = String.valueOf(c.getLong(c.getColumnIndex(Mms._ID)));
                            final Uri MMS_ADDR_URI = Uri.parse(MMS_ADDR_URI_BASE.replace("{id}", m_id));
                            try(Cursor c2 = repository.query(MMS_ADDR_URI, new String[]{Mms.Addr.ADDRESS, Mms.Addr.CONTACT_ID}, null, null, null)){
                                if(c2 != null){
                                    if(c2.moveToFirst()){
                                        recipients = c2.getString(c2.getColumnIndex(Mms.Addr.ADDRESS));
                                    }
                                }
                            }
                        }

                        LOGGER.info("Thread {} with {}", t_id, recipients);
                    }while (c.moveToNext());
                }
            }
        }
        return null;
        //return new ConversationIterator(c, true);
    }

    @Override
    public Conversation getConversationById(long id) {
        LOGGER.info("Getting Conversation {}", id);

        final Uri THREADS_URI = ContentUris.withAppendedId(MmsSms.CONTENT_CONVERSATIONS_URI, id);
        final Uri SMS_URI = ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, id);

        List<Message> messages = null;
        List<Contact> contacts = null;

        //Recipients of the conversation
        try(Cursor c = repository.query(THREADS_URI, THREADS_PROJECTION, null, null, null)){
            if(c != null){
                if(c.moveToFirst()){
                    final String recipients = c.getString(THREADS_RECIPIENT_IDX);
                    contacts = contactRepository.getContactsByPhone(recipients);
                }
            }
        }

        //SMS of the conversation
        try(Cursor c = repository.query(SMS_URI, SMS_PROJECTION, null, null, null)){
            if(c != null){
                if(c.moveToFirst()){
                    messages = new ArrayList<>(c.getCount());

                    do{
                        messages.add(new Message(
                                c.getLong(SMS_DATE_IDX),
                                c.getString(SMS_BODY_IDX),
                                c.getInt(SMS_READ_IDX) == 1,
                                getMessageState(c.getInt(SMS_TYPE_IDX))
                        ));
                    }while (c.moveToNext());
                }
            }
        }

        //MMS of the conversation
        /*try(Cursor c = repository.query(Mms.CONTENT_URI, MMS_PROJECTION, null, null, null)){
            if(c != null){
                if(c.moveToFirst()){

                    //Allocate messages if no SMS found
                    if(messages == null)
                        messages = new ArrayList<>(c.getCount());

                    do{
                        final long mmsId = c.getLong(MMS_ID_IDX);
                        final long mmsDate = c.getLong(MMS_DATE_IDX);
                        final long tid = c.getLong(MMS_TID_IDX);

                        if(tid == id) {
                            try (Cursor c2 = repository.query(ContentUris.withAppendedId(MMS_PART_URI, mmsId), MMS_PART_PROJECTION, null, null, null)) {
                                if(c2 != null) {
                                    if(c2.moveToFirst())
                                        LOGGER.info("MMS Mime Type {}", c2.getString(MMS_PART_MIME_IDX));
                                }
                            }
                        }

                    }while (c.moveToNext());
                }
            }
        }*/

        return new Conversation(id, contacts, messages);
    }

    private Message.State getMessageState(int state) {
        switch (state){
            case Sms.MESSAGE_TYPE_DRAFT:
                return Message.State.DRAFT;
            case Sms.MESSAGE_TYPE_FAILED:
                return Message.State.FAILED;
            case Sms.MESSAGE_TYPE_INBOX:
                return Message.State.RECEIVED;
            case Sms.MESSAGE_TYPE_SENT:
                return Message.State.SENT;
            case Sms.MESSAGE_TYPE_QUEUED:
                return Message.State.QUEUED;
            case Sms.MESSAGE_TYPE_OUTBOX:
                return Message.State.PENDING;
            default:
                return Message.State.PENDING;
        }
    }

    private class ConversationIterator extends LazyCursorIterator<Conversation> {

        protected ConversationIterator(Cursor cursor, boolean reset) {
            super(cursor, reset);
        }

        @Override
        protected Conversation handleEntity(Cursor cursor) {
            return getConversationById(cursor.getLong(0));
        }
    }
}
