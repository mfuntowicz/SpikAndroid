package com.funtowiczmo.spik.repositories.observers;

import android.database.ContentObserver;
import android.net.Uri;
import com.funtowiczmo.spik.lang.ThreadedMessage;
import com.funtowiczmo.spik.repositories.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mfuntowicz on 26/12/15.
 */
public abstract class MessageObserver extends ContentObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageObserver.class);

    private final AtomicLong LAST_SMS_PROCEEDED;
    private final Marker OBSERVER_NAME;

    private final Uri target;
    private final MessageRepository repository;

    public MessageObserver(String name, Uri target, MessageRepository repository) {
        super(null);

        long lastId = Long.MAX_VALUE;

        try {
            lastId = repository.getLastReceivedMessageId();
        } catch (Exception e) {
            LOGGER.warn("Unable to initialize LAST_SMS_PROCEEDED from database", e);
        }

        this.LAST_SMS_PROCEEDED = new AtomicLong(lastId);
        this.OBSERVER_NAME = MarkerFactory.getMarker(name);
        this.target = target;
        this.repository = repository;
    }

    /**
     * Called when a new message is received
     *
     * @param message
     */
    protected abstract void onMessage(ThreadedMessage message);

    /**
     * Name of this observer
     *
     * @return
     */
    public String name() {
        return OBSERVER_NAME.getName();
    }

    /**
     * Target of this observer
     *
     * @return
     */
    public Uri target() {
        return target;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        if (target.equals(uri)) {

            if (LOGGER.isDebugEnabled())
                LOGGER.debug(OBSERVER_NAME, "Detected change on URI {}", uri.toString());

            try {
                final long id = repository.getLastReceivedMessageId();

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("New message with id {} (last proceeded : {})", id, LAST_SMS_PROCEEDED.get());

                if (LAST_SMS_PROCEEDED.getAndSet(id) != id) {
                    LOGGER.info("New message with id {}", id);

                    final ThreadedMessage msg = repository.getSmsById(id);
                    if (msg != null)
                        onMessage(msg);
                    else
                        LOGGER.warn("The message {} was null after #getSmsById()", id);
                }
            } catch (Exception e) {
                LOGGER.error("Got an exception while processing event", e);
            }
        }
    }

    @Override
    public boolean deliverSelfNotifications() {
        return false; //Don't deliver self change
    }
}
