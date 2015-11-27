package com.funtowiczmo.spik.lang;

import java.util.Collection;
import java.util.List;

/**
 * Created by momo- on 25/11/2015.
 */
public class Conversation {

    private long id;
    private Collection<Contact> participants;
    private List<Message> messages;

    public Conversation(long threadId, List<Contact> contacts, List<Message> messages) {
        this.id = threadId;
        this.participants = contacts;
        this.messages = messages;
    }

    public long id() {
        return id;
    }

    public Collection<Contact> participants() {
        return participants;
    }

    public List<Message> messages() {
        return messages;
    }
}
