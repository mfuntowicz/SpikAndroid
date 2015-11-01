package com.funtowiczmo.spik.repositories;

import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.utils.LazyCursorIterator;

import java.util.Collection;
import java.util.List;

/**
 * Created by momo- on 27/10/2015.
 */
public interface ContactRepository {
    /**
     * Return all the contacts
     * @return
     */
    LazyCursorIterator<Contact> getContacts();

    /**
     * Try to find a contact according to his id
     * @param id Contact's id
     * @return
     */
    Contact getContactById(long id);

    /**
     * Try to find a contact according to some String
     * @param desc Descriptive string for the contact (name, phone, ...)
     * @return
     */
    LazyCursorIterator<Contact> getContactByName(String desc);


    /**
     * Try to find the contact according to his phone number
     * @param phone Phone's number
     * @return
     */
    Contact getContactByPhone(String phone);

    /**
     * Try to find all the contact according to their phone number
     * @param contacts String holding phone numbers separated by space
     * @return
     */
    List<Contact> getContactsByPhone(String contacts);
}
