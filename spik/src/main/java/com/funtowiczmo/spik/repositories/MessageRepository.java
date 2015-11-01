package com.funtowiczmo.spik.repositories;

import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.utils.LazyCursorIterator;

import java.util.Collection;

/**
 * Created by momo- on 27/10/2015.
 */
public interface MessageRepository {
    /**
     * Return all the conversations
     * @return
     */
    LazyCursorIterator<Conversation> getConversations();

    /**
     * Try to find a conversation according to his id
     * @param id Conversation's id
     * @return
     */
    Conversation getConversationById(long id);

    /**
     * Try to find a conversation in which all the contact are taking part
     * @param ids Contacts' id
     * @param create True if we need to create the conversation if not found, false otherwise
     * @return

    Conversation getConversationByContact(long[] ids, boolean create);*/
}
