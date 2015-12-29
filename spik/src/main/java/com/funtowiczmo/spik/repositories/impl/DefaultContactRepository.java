package com.funtowiczmo.spik.repositories.impl;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import com.funtowiczmo.spik.lang.Contact;
import com.funtowiczmo.spik.repositories.ContactRepository;
import com.funtowiczmo.spik.utils.CursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static android.provider.ContactsContract.CommonDataKinds.Phone.*;

/**
 * Created by momo- on 28/10/2015.
 */
public class DefaultContactRepository implements ContactRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContactRepository.class);

    public static final String PHONE_SEPARATOR = ";";
    public static final String ISO_2_COUNTRY_CODE = Locale.getDefault().getISO3Country().substring(0, 2);

    /** Contact Constant **/
    private static final int ID_IDX = 0;
    private static final int LOOKUP_IDX = 1;
    private static final int DISPLAY_NAME_IDX = 2;
    private static final int NORMALIZED_NUMBER_IDX = 3;
    private static final String[] DEFAULT_PROJECTION = new String[]{
        _ID,
        LOOKUP_KEY,
        DISPLAY_NAME_PRIMARY,
        NORMALIZED_NUMBER,
    };

    /** Phone Lookup Constants **/
    /** We use the same order as DEFAULT_PROJECTION, no need to redefined indexes **/
    private static final String[] PHONE_LK_DEFAULT_PROJECTION = new String[]{
        ContactsContract.PhoneLookup._ID,
        ContactsContract.PhoneLookup.LOOKUP_KEY,
        ContactsContract.PhoneLookup.DISPLAY_NAME,
        ContactsContract.PhoneLookup.NORMALIZED_NUMBER
    };

    private final ContentResolver repository;

    @Inject
    public DefaultContactRepository(Context context) {
        repository = context.getContentResolver();
    }

    @Override
    @SuppressWarnings("Recycle")
    public CursorIterator<Contact> getContacts() {
        LOGGER.info("Getting all contacts");

        final String SELECTION = HAS_PHONE_NUMBER + " = ?";
        final String[] FILTER = new String[]{"1"};

        Cursor c = repository.query(
                CONTENT_URI,
                DEFAULT_PROJECTION,
                SELECTION,
                FILTER,
                DISPLAY_NAME_PRIMARY + " ASC"
        );

        return new ContactIterator(c, true);
    }

    @Override
    public Contact getContactById(long id) {
        LOGGER.info("Trying to get contact with ID {}", id);

        final String SELECTION = _ID + " = ?";
        final String[] FILTER = new String[]{ String.valueOf(id) };

        try(Cursor c = repository.query(CONTENT_URI, DEFAULT_PROJECTION, SELECTION, FILTER, null)){
            if(c.moveToFirst()){
                return contactFromCursor(c);
            }
        }

        LOGGER.info("Contact with ID {} not found", id);

        return null;
    }

    @Override
    @SuppressWarnings("Recycle")
    public CursorIterator<Contact> getContactByName(String desc) {
        final String SELECTION = DISPLAY_NAME_PRIMARY + " LIKE ?";
        final String[] FILTER = new String[]{ "%" + desc + "%" };

        Cursor c = repository.query(CONTENT_URI, DEFAULT_PROJECTION, SELECTION, FILTER, null);
        return new ContactIterator(c, true);
    }

    @Override
    public Contact getContactByPhone(String phone) {
        LOGGER.info("Looking contact with phone {}", phone);

        String phoneParam;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            phoneParam = PhoneNumberUtils.formatNumberToE164(phone, ISO_2_COUNTRY_CODE);
        }else{
            phoneParam = phone;
        }

        Uri LOOKUP_URI = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneParam));
        try (Cursor c = repository.query(LOOKUP_URI, PHONE_LK_DEFAULT_PROJECTION, null, null, null)){
            if(c != null){
                if(c.moveToFirst()){
                    return contactFromCursor(c);
                }else{
                    LOGGER.warn("Unable to move to the first row");
                }
            }else{
                LOGGER.warn("Cursor is null");
            }
        }

        return null;
    }

    @Override
    public List<Contact> getContactsByPhone(String phones) {
        LOGGER.info("Looking for contacts with phones {}", phones);

        final Collection<String> c_phones = Arrays.asList(phones.split(PHONE_SEPARATOR));
        final List<Contact> matched = new ArrayList<>(c_phones.size());

        if(!c_phones.isEmpty()) {
            for (String phone : c_phones) {
                Contact c = getContactByPhone(phone);

                if (c != null) {
                    matched.add(c);
                }

            }
        }
        return matched;
    }


    /**
     * Extract field from the cursor to make a Contact
     * @param c
     * @return
     */
    private Contact contactFromCursor(Cursor c) {
        return new Contact(
            c.getLong(ID_IDX),
            c.getString(DISPLAY_NAME_IDX),
            c.getString(NORMALIZED_NUMBER_IDX),
            getPhoto(c.getLong(ID_IDX))
        );
    }


    /**
     * Utility method to retrieve photo thumbnail
     * @param contactId
     * @return Null if no photo, byte[] otherwise
     */
    @Nullable
    private byte[] getPhoto(long contactId){
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

        try(InputStream in = ContactsContract.Contacts.openContactPhotoInputStream(repository, contactUri, true)){
            if(in != null) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    final byte[] buffer = new byte[4096];
                    while (in.read(buffer) != -1) {
                        out.write(buffer);
                    }

                    return out.toByteArray();
                }
            }
        } catch (IOException ignored) {}

        return null;
    }


    /**
     * Contact's provider lazy iterator
     */
    private class ContactIterator extends CursorIterator<Contact> {

        public ContactIterator(Cursor cursor, boolean reset) {
            super(cursor, reset);
        }

        @Override
        protected Contact fillFromCursor(Cursor c) {
            return contactFromCursor(c);
        }
    }
}
