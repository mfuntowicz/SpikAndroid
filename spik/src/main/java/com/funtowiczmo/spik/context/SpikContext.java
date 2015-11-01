package com.funtowiczmo.spik.context;

import com.funtowiczmo.spik.repositories.ContactRepository;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.google.inject.Inject;

/**
 * Created by momo- on 28/10/2015.
 */
public class SpikContext {

    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;

    @Inject
    public SpikContext(ContactRepository contactRepository, MessageRepository messageRepository) {
        this.contactRepository = contactRepository;
        this.messageRepository = messageRepository;
    }

    public ContactRepository contactRepository(){
        return contactRepository;
    }

    public MessageRepository messageRepository(){
        return messageRepository;
    }
}
