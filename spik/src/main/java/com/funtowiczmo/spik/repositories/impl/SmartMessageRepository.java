package com.funtowiczmo.spik.repositories.impl;

import android.database.Cursor;
import android.provider.Telephony;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.ThreadedMessage;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.funtowiczmo.spik.repositories.observers.MessageObserver;
import com.funtowiczmo.spik.utils.LazyCursorIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mfuntowicz on 28/12/15.
 */
public class SmartMessageRepository implements MessageRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartMessageRepository.class);

    @Override
    public ThreadedMessage getSmsById(long id) throws Exception {
        return null;
    }

    @Override
    public long getLastReceivedMessageId() throws Exception {
        return 0;
    }

    @Override
    public LazyCursorIterator<Conversation> getConversations() {
        return null;
    }

    @Override
    public Conversation getConversationById(long id) throws Exception {
        return null;
    }

    @Override
    public void registerObserver(MessageObserver observer) {

    }

    /**
     * Lazy iterator on conversation
     */
    private class ConversationIterator extends LazyCursorIterator<Conversation> {

        private ConversationIterator(Cursor cursor, boolean reset) {
            super(cursor, reset);
        }

        @Override
        protected Conversation fillFromCursor(Cursor cursor) {
            try {
                final long t_id = cursor.getLong(cursor.getColumnIndex(Telephony.Mms.THREAD_ID));
                return getConversationById(t_id);
            }catch (Exception e){
                LOGGER.warn("Unable to fillFromCursor", e);
            }

            return null;
        }
    }
}
