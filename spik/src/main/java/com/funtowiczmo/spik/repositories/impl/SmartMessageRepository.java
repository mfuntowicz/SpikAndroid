package com.funtowiczmo.spik.repositories.impl;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import com.funtowiczmo.spik.lang.Conversation;
import com.funtowiczmo.spik.lang.Message;
import com.funtowiczmo.spik.lang.ThreadedMessage;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.funtowiczmo.spik.repositories.observers.MessageObserver;
import com.funtowiczmo.spik.utils.CursorIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mfuntowicz on 28/12/15.
 */
public class SmartMessageRepository implements MessageRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartMessageRepository.class);
    private static final Marker CACHE_MARKER = MarkerFactory.getMarker("[Recipients Cache]");
    private static final Marker REPO_MARKER = MarkerFactory.getMarker("[Repository]");

    /** Cache related constants **/
    private static final Uri RECIPIENTS_CANONICAL_URI =
            Telephony.MmsSms.CONTENT_URI.buildUpon().appendPath("canonical-addresses").build();

    private static final int CANONICAL_ID_IDX       = 0;
    private static final int CANONICAL_NUMBER_IDX   = 1;

    /** Threads related constants **/
    public static final Uri ALL_THREADS_URI =
            Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();

    public static final String[] ALL_THREADS_PROJECTION = {
        Telephony.Threads._ID,
        Telephony.Threads.DATE,
        Telephony.Threads.MESSAGE_COUNT,
        Telephony.Threads.RECIPIENT_IDS,
    };

    private static final int THREADS_ID             = 0;
    private static final int THREADS_DATE           = 1;
    private static final int THREADS_MESSAGE_COUNT  = 2;
    private static final int THREADS_RECIPIENT_IDS  = 3;

    /** SMS/MMS related constants **/
    private static final Uri MMS_SMS_BASE_URI = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
    private  static final String MMS_MULTIPART_RELATED = "application/vnd.wap.multipart.related";

    private static final String[] ALL_SMS_MMS_PROJECTION = {
        Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
        Telephony.Sms.DATE,
        Telephony.Sms.BODY,
        Telephony.Sms.READ,
        Telephony.Sms.TYPE,
        Telephony.Mms.CONTENT_TYPE,
        Telephony.Mms.TEXT_ONLY,
        Telephony.Mms.THREAD_ID
    };

    private static final int MMS_SMS_DISCRIMINATOR  = 0;
    private static final int MMS_SMS_DATE           = 1;
    private static final int MMS_SMS_BODY           = 2;
    private static final int MMS_SMS_READ           = 3;
    private static final int MMS_SMS_TYPE           = 4;
    private static final int MMS_SMS_CONTENT_TYPE   = 5;
    private static final int MMS_SMS_THREAD_ID   = 6;
/*    private static final int MMS_SMS_TEXT_ONLY      = 6;
    private static final int MMS_SMS_   = 6;
    private static final int MMS_SMS_TEXT_ONLY      = 6;*/

    /** MMS.Part related constants **/
    //private static final Uri MMS_BASE_URI = Telephony.Mms.Part.

    /** Instance variables **/
    private Context context;
    private ConcurrentMap<Long, String> recipientsCache;
    private ConcurrentMap<Long, CachedConversation> threadsCache;

    @Inject
    public SmartMessageRepository(Context context) {
        this.context = context;
        this.recipientsCache = new ConcurrentHashMap<>();
        this.threadsCache = new ConcurrentHashMap<>();


        //Listen for modification in RECIPIENTS_CANONICAL_URI DatabaseALL_SMS_MMS_PROJECTION
        ContentObserver recipientsObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if(uri != null){
                    if(uri.equals(RECIPIENTS_CANONICAL_URI)){
                        LOGGER.info("Update detected in {} -> Invalidate cache", uri);
                        invalidateRecipientCache();
                    }
                }
            }
        };
        context.getContentResolver().registerContentObserver(RECIPIENTS_CANONICAL_URI, false, recipientsObserver);

        new Thread(new Runnable() {
            @Override
            public void run() {
                invalidateRecipientCache();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                invalidateThreadsCache();
            }
        }).start();
    }

    @Override
    public ThreadedMessage getSmsById(long id) throws Exception {
        LOGGER.trace("Trying to get SMS with id : {}", id);
        try (Cursor c = context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ, Telephony.Sms.TYPE, Telephony.Sms.THREAD_ID },
                Telephony.Sms._ID+ " = ?", new String[]{String.valueOf(id)}, null)) {

            if (c.moveToFirst()) {
                long thread = c.getLong(c.getColumnIndex(Telephony.Sms.THREAD_ID));
                Conversation conversation = getConversationById(thread);

                if(conversation != null){
                    Message msg = fillSmsFromCursor(c);
                    long threadId;

                    if(conversation instanceof CachedConversation) {
                        threadId = conversation.spikId();
                    }else{
                        threadId = Arrays.hashCode(conversation.participants());
                    }

                    return new ThreadedMessage(threadId, msg);
                }else{
                    throw new Exception("Unable to find Conversation with id " + id);
                }
            }
        }
        throw new Exception("Unable to find SMS with id " + id);
    }

    @Override
    public long getLastReceivedMessageId() throws Exception {
        LOGGER.trace("Trying to get last SMS ID in the repository");
        try (Cursor c = context.getContentResolver().query(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, new String[]{Telephony.Sms._ID}, null, null, Telephony.Sms.DATE + " DESC")) {
            if (c.moveToFirst())
                return c.getLong(0);

        }
        throw new Exception("Unable retrieve last ID from content provider");
    }

    @Override
    public Iterable<? extends Conversation> getConversations() {
        return threadsCache.values();
    }

    @Override
    public Conversation getConversationById(long id) throws Exception {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug(REPO_MARKER, "Trying to get conversation {}", id);

        //If we don't find in cache, invalidate it
        if(!threadsCache.containsKey(id)) {
            LOGGER.trace(REPO_MARKER, "Conversation {} not find in cache, reloading", id);
            invalidateThreadsCache();

            if(!threadsCache.containsKey(id)){
                LOGGER.warn(REPO_MARKER, "Conversation {} not find in cache after reload", id);
                throw new Exception("Unknown conversation " + id);
            }
        }

        return threadsCache.get(id);
    }

    @Override
    public void registerObserver(MessageObserver observer) {
        context.getContentResolver().registerContentObserver(observer.target(), false, observer);
    }

    private Message fillSmsFromCursor(Cursor cursor){
        return new Message(
                cursor.getLong(MMS_SMS_DATE),
                cursor.getString(MMS_SMS_BODY),
                cursor.getInt(MMS_SMS_READ) == 1,
                Message.State.fromStatus(cursor.getInt(MMS_SMS_TYPE))
        );
    }

    private Message fillMmsFromCursor(Cursor cursor){
        return null;
    }


    /**
     * Retrieve all the threads in the database
     */
    private void invalidateThreadsCache(){
        LOGGER.trace(REPO_MARKER, "Invalidating Threads cache ({} entries)", threadsCache.size());

        long start = System.currentTimeMillis();

        try(Cursor c = context.getContentResolver().query(ALL_THREADS_URI, ALL_THREADS_PROJECTION, null, null, null)){
            if(c != null){
                if(!c.isBeforeFirst())
                    c.move(-1);

                if(LOGGER.isDebugEnabled())
                    LOGGER.debug(REPO_MARKER, "{} entries in Cursor", c.getCount());

                while (c.moveToNext()){
                    long threadId = c.getLong(THREADS_ID);

                    CachedConversation conv = threadsCache.get(threadId);
                    if(conv == null) {
                        conv = new CachedConversation(c);
                        threadsCache.put(threadId, conv);
                    }else {
                        conv.fillFromCursor(c);
                    }
                }

            }else{
                LOGGER.warn(REPO_MARKER, "Unable to fill Threads cache => Cursor is null");
            }
        }

        long duration = System.currentTimeMillis() - start;

        LOGGER.trace(REPO_MARKER, "Threads cache filled in {} ms ({} entries)", duration, threadsCache.size());
    }

    /**
     * Try to find in the cache all the addresses for each id
     * If an id is not found in the cache, the cache is invalided and refilled
     * @param sepIds Ids concatened
     * @param sep Separator to split ids
     * @return
     */
    private List<RecipientCacheEntry> getAddresses(String sepIds, String sep){
        String[] ids = sepIds.split(sep);
        List<RecipientCacheEntry> addresses = new ArrayList<>(ids.length);

        for (String id : ids) {
            try {
                long lId = Long.parseLong(id);

                if(!recipientsCache.containsKey(lId)){
                    LOGGER.warn(CACHE_MARKER, "{} not in cache", id);
                    invalidateRecipientCache();
                }

                String address = recipientsCache.get(lId);
                if(TextUtils.isEmpty(address))
                    LOGGER.warn(CACHE_MARKER, "Address for id {} is empty", id);
                else
                    addresses.add(new RecipientCacheEntry(lId, address));

            }catch (NumberFormatException e){
                LOGGER.warn(CACHE_MARKER, "Unable to parse id {} as long", id);
            }
        }

        return addresses;
    }

    /**
     * Fill the cache from the canonical table
     */
    private void invalidateRecipientCache() {
        LOGGER.trace(CACHE_MARKER, "Invalidating cache ({} entries)", recipientsCache.size());

        long start = System.currentTimeMillis();

        //Clear all entries, if any
        if(recipientsCache.size() != 0)
            recipientsCache.clear();

        try(Cursor c = context.getContentResolver().query(RECIPIENTS_CANONICAL_URI, null, null, null, null)){
            if(c != null){
                //Go before the first
                if(!c.isBeforeFirst())
                    c.move(-1);

                if(LOGGER.isDebugEnabled())
                    LOGGER.debug(CACHE_MARKER, "{} entries in Cursor", c.getCount());

                while(c.moveToNext()){
                    long id = c.getLong(CANONICAL_ID_IDX);
                    String number = c.getString(CANONICAL_NUMBER_IDX);

                    recipientsCache.put(id, number);

                    if(LOGGER.isDebugEnabled())
                        LOGGER.debug(CACHE_MARKER, "Entry: {} -> {}", id, number);
                }

            }else{
                LOGGER.warn(CACHE_MARKER, "Unable to invalidateRecipientCache cache, null Cursor");
            }
        }

        long duration = System.currentTimeMillis() - start;

        LOGGER.trace(CACHE_MARKER, "Cache filled in {} ms ({} entries)", duration, recipientsCache.size());
    }

    /**
     * Represents an entry in the recipient cache
     */
    public static class RecipientCacheEntry {
        public final long id;
        public final String number;

        public RecipientCacheEntry(long id, String number) {
            this.id = id;
            this.number = number;
        }
    }

    /**
     * Represents an entry in the conversation cache
     */
    private  class CachedConversation implements Conversation {

        private long id;
        private long spikId;
        private long date;
        private int messagesCount;
        private String[] recipients;

        public CachedConversation(Cursor cursor){
            fillFromCursor(cursor);
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public long spikId(){ return spikId; }

        @Override
        public long date() {
            return date;
        }

        @Override
        public String[] participants() {
            return recipients;
        }

        @Override
        public int messagesCount(){
            return messagesCount;
        }

        @Override
        public CursorIterator<Message> messages(final Context context) {
            final Uri MESSAGES_URI = ContentUris.withAppendedId(MMS_SMS_BASE_URI, id);

            //Retrieve only SMS
            //TODO : Retrieve MMS
            final Cursor c = context.getContentResolver().query(MESSAGES_URI, ALL_SMS_MMS_PROJECTION, null, null, Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 150");
            return new CursorIterator<Message>(c, true){

                @Override
                protected Message fillFromCursor(Cursor cursor) {
                    final String type = cursor.getString(MMS_SMS_DISCRIMINATOR);

                    if(type.equals("sms")) {
                        return fillSmsFromCursor(c);
                    }else if(type.equals("mms")){
                        String contentType = cursor.getString(MMS_SMS_CONTENT_TYPE);
                        if(MMS_MULTIPART_RELATED.equals(contentType))
                            return fillMmsFromCursor(c);
                        else
                            LOGGER.trace(REPO_MARKER, "MMS Content-Type not supported {}", contentType);
                    }else{
                        LOGGER.warn(REPO_MARKER, "Unable to deserialize message with type {}", type);
                    }

                    return null;
                }
            };
        }

        public Conversation fillFromCursor(Cursor c){
            final List<RecipientCacheEntry> recipientsEntries =
                    getAddresses(c.getString(THREADS_RECIPIENT_IDS), " ");

            final String[] addresses = new String[recipientsEntries.size()];
            for (int i = 0; i < recipientsEntries.size(); i++) {
                addresses[i] = recipientsEntries.get(i).number;
            }

            id = c.getLong(THREADS_ID);
            spikId = Arrays.hashCode(addresses);
            date = c.getLong(THREADS_DATE);
            messagesCount = c.getInt(THREADS_MESSAGE_COUNT);
            recipients = addresses;

            return this;
        }
    }
}

