package com.funtowiczmo.spik.repositories.impl;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.lang.ThreadedMessage;
import com.funtowiczmo.spik.repositories.ContactRepository;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.funtowiczmo.spik.repositories.observers.MessageObserver;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Telephony.*;

/**
 * Created by momo- on 28/10/2015.
 */
public class DefaultMessageRepository implements MessageRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageRepository.class);

    /** SMS Contants **/
    private static final String[] SMS_PROJECTION = new String[]{
        Sms.DATE,
        Sms.BODY,
        Sms.TYPE,
        Sms.READ,
    };

    private static final String[] SMS_PROJECTION_WITH_THREAD = new String[]{
            Sms.DATE,
            Sms.BODY,
            Sms.TYPE,
            Sms.READ,
            Sms.THREAD_ID
    };

    private static final int SMS_DATE_IDX = 0;
    private static final int SMS_BODY_IDX = 1;
    private static final int SMS_TYPE_IDX = 2;
    private static final int SMS_READ_IDX = 3;
    private static final int SMS_THREAD_IDX = 4;


    /** MMS Constants **/
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
    private static final Uri MMS_PART_URI = Uri.parse("content://mms/part");
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

    /**
     * Convert internal Android messages state to Spik one
     *
     * @param state Android state
     * @return
     */
    private static Message.State getMessageState(int state) {
        switch (state) {
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

    @Override
    public ThreadedMessage getSmsById(long id) throws Exception {
        LOGGER.trace("Trying to get SMS with id : {}", id);

        try (Cursor c = repository.query(Sms.CONTENT_URI, SMS_PROJECTION_WITH_THREAD,
                Sms._ID + " = ?",
                new String[]{String.valueOf(id)}, null)) {

            if (c.moveToFirst()) {
                long thread = c.getLong(SMS_THREAD_IDX);
                Message msg = getMessageFromCursor(c);

                return new ThreadedMessage(thread, msg);
            }
        }

        throw new Exception("Unable to find SMS with id " + id);
    }

    @Override
    public long getLastReceivedMessageId() throws Exception {
        LOGGER.trace("Trying to get last SMS ID in the repository");
        try (Cursor c = repository.query(Sms.Inbox.CONTENT_URI, new String[]{Sms._ID}, null, null, null)) {
            if (c.moveToFirst()) {

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Cursor items : {}", c.getCount());

                return c.getLong(0);
            }
        }

        throw new Exception("Unable retrieve last ID from content provider");
    }

    @Override
    @SuppressLint("Recycle")
    public LazyCursorIterator<Conversation> getConversations() {
        LOGGER.info("Trying to get all conversations");

        Cursor c = repository.query(MmsSms.CONTENT_CONVERSATIONS_URI, null, null, null, null);
        return new ConversationIterator(c, true);
    }

    @Override
    public Conversation getConversationById(long threadId) {
        LOGGER.info("Getting Conversation {}", threadId);

        final List<Contact> contacts = getParticipants(threadId);
        final List<Message> sms = getSMSForThread(threadId);
        final List<Message> mms = getMMSForThread(threadId);

        final List<Message> messages = new ArrayList<>(
                (sms != null ? sms.size() : 0) + (mms != null ? mms.size() : 0)
        );

        if(sms != null)
            messages.addAll(sms);

        /*if(mms != null)
            messages.addAll(mms);*/


        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Retrieved {} messages for conversation {}", messages.size(), threadId);

        return new Conversation(threadId, contacts, messages);
    }

    @Override
    public void registerObserver(MessageObserver observer) {
        LOGGER.info("Registering Observer '{}' for Uri : {}", observer.name(), observer.target());

        repository.registerContentObserver(observer.target(), false, observer);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Observer '{}' registered", observer.name());
    }

    private List<Message> getMMSForThread(long threadId) {
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
        return null;
    }

    private List<Message> getSMSForThread(long threadId){
        LOGGER.info("Getting all messages for conversation {}", threadId);
        final Uri SMS_URI = ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId);

        List<Message> messages = null;

        //SMS of the conversation
        try(Cursor c = repository.query(SMS_URI, SMS_PROJECTION, null, null, null)){
            if(c != null){
                if(c.moveToFirst()){
                    if(LOGGER.isDebugEnabled())
                        LOGGER.debug("Cursor#getCount() = {}", c.getCount());

                    messages = new ArrayList<>(c.getCount());

                    do {
                        messages.add(getMessageFromCursor(c));
                    }while (c.moveToNext());
                }else{
                    LOGGER.warn("Unable to move to the first row, might have 0 message in the conversation  (Cursor#getCount() = {})", c.getCount());
                }
            }else{
                LOGGER.warn("Cursor is null");
            }
        }

        return messages;
    }

    /**12-26 18:10:59.224    4251-4775/com.funtowiczmo.spik W/com.funtowiczmo.spik.repositories.observers.MessageObserver﹕ 18:10:59.224 [Binder_3] WARN  c.f.s.r.observers.MessageObserver - The message 3827 was null after #getSmsById()

     * Query the provider for the participants in the thread
     * @param threadId Id of thread we want to get the participants
     * @return Null if thread not found.
     */
    private List<Contact> getParticipants(long threadId){
        LOGGER.info("Getting participants in the conversation {}", threadId);

        final Uri THREAD_URI = ContentUris.withAppendedId(MmsSms.CONTENT_CONVERSATIONS_URI, threadId);
        String recipients = null;

        try(Cursor c = repository.query(THREAD_URI, new String[]{Mms._ID, Sms.ADDRESS}, null, null, null)) {
            if (c != null) {
                if (c.moveToFirst()) {
                    recipients = c.getString(c.getColumnIndex(Sms.ADDRESS));

                    if (recipients == null) {
                        LOGGER.debug("Sms.ADDRESS is null, might be a MMS");

                        final String m_id = String.valueOf(c.getLong(c.getColumnIndex(Mms._ID)));
                        final Uri MMS_ADDR_URI = Uri.parse(MMS_ADDR_URI_BASE.replace("{id}", m_id));
                        try (Cursor c2 = repository.query(MMS_ADDR_URI, new String[]{Mms.Addr.ADDRESS, Mms.Addr.CONTACT_ID}, null, null, null)) {
                            if (c2 != null) {
                                if (c2.moveToFirst()) {
                                    recipients = c2.getString(c2.getColumnIndex(Mms.Addr.ADDRESS));
                                }else{
                                    LOGGER.warn("Unable to move to the first row of the cursor to retrieve MMS Address");
                                }
                            }else{
                                LOGGER.warn("Cursor is null to retrieve MMS Address");
                            }
                        }
                    }
                }else{
                    LOGGER.warn("Unable to move to the first row of the cursor");
                }
            }else{
                LOGGER.warn("Cursor is null");
            }
        }

        return recipients == null ? null : contactRepository.getContactsByPhone(recipients);
    }

    private Message getMessageFromCursor(Cursor c) {
        return new Message(
                c.getLong(SMS_DATE_IDX),
                c.getString(SMS_BODY_IDX),
                c.getInt(SMS_READ_IDX) == 1,
                getMessageState(c.getInt(SMS_TYPE_IDX))
        );
    }

    private class ConversationIterator extends LazyCursorIterator<Conversation> {

        private ConversationIterator(Cursor cursor, boolean reset) {
            super(cursor, reset);
        }

        @Override
        protected Conversation handleEntity(Cursor cursor) {
            final long t_id = cursor.getLong(cursor.getColumnIndex(Mms.THREAD_ID));
            return getConversationById(t_id);
        }
    }
}
