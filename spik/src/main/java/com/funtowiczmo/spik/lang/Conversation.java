package com.funtowiczmo.spik.lang;

import android.content.Context;
import com.funtowiczmo.spik.utils.CursorIterator;


/**
 * Created by momo- on 25/11/2015.
 */
public interface Conversation {

    /**
     * Conversation's id
     * @return
     */
    long id();


    /**
     * Return the id use by Spik to identify this conversation
     * @return
     */
    long spikId();

    /**
     * Creation date of this conversation
     * @return
     */
    long date();

    /**
     * Id of the recipients of the conversation
     * @return
     */
    String[] participants();

    /**
     * Number of messages
     * @return
     */
    int messagesCount();

    /**
     * Iterate through messages
     * @return
     */
    CursorIterator<Message> messages(Context context);
}
