package com.funtowiczmo.spik.service;

import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.utils.CursorIterator;

import java.util.Collection;

/**
 * Created by momo- on 19/01/2016.
 */
public interface SpikService {

    /**
     * Send a message to the specified participants
     *
     * @param tid Transaction's id, needed to keep track of the status of the message
     * @param participants Phones' number of each participant
     * @param text         Message to send
     * @return Message's id
     */
    long sendMessage(long tid, Collection<String> participants, String text);

    /**
     * Retrieve all the conversations in the database
     *
     * @return
     */
    Iterable<? extends Conversation> getConversations();

    /**
     * Retrieve all the contacts in the database
     *
     * @return
     */
    CursorIterator<Contact> getContacts();
}
