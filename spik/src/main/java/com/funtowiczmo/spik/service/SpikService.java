package com.funtowiczmo.spik.service;

import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.lang.Conversation;

import java.util.Collection;

/**
 * Created by momo- on 19/01/2016.
 */
public interface SpikService {

    /**
     * Send a message to the specified participants
     *
     * @param participants Phones' number of each participant
     * @param text         Message to send
     * @return Message's id
     */
    long sendMessage(Collection<String> participants, String text);


    /**
     * Retrieve all the conversations in the database
     *
     * @return
     */
    Iterable<Conversation> getConversations();


    /**
     * Retrieve all the contacts in the database
     *
     * @return
     */
    Iterable<Contact> getContacts();

}
