package com.funtowiczmo.spik.repositories;

import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.ThreadedMessage;
import com.funtowiczmo.spik.repositories.observers.MessageObserver;

/**
 * Created by momo- on 27/10/2015.
 */
public interface MessageRepository {

    /**
     * Look for the message with specified id
     * @param id
     * @return
     */
    ThreadedMessage getSmsById(long id) throws Exception;

    /**
     * Retrieve the last message's id in the repository
     *
     * @return
     */
    long getLastReceivedMessageId() throws Exception;


    /**
     * Return all the conversations
     * @return
     */
    Iterable<? extends Conversation> getConversations();

    /**
     * Try to find a conversation according to his id
     * @param id Conversation's id
     * @return
     */
    Conversation getConversationById(long id) throws Exception;

    /**
     * Try to find a conversation in which all the contact are taking part
     * @param ids Contacts' id
     * @param create True if we need to create the conversation if not found, false otherwise
     * @return

    Conversation getConversationByContact(long[] ids, boolean create);*/

    /**
     * Register an observer to this MessageRepository
     *
     * @param observer
     */
    void registerObserver(MessageObserver observer);
}
