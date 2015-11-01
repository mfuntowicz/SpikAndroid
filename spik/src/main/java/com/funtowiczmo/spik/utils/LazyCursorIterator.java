package com.funtowiczmo.spik.utils;

import android.database.*;

import java.util.Iterator;

/**
 * Created by mfuntowicz on 29/10/15.
 */
public abstract class LazyCursorIterator<T> implements Iterator<T>, AutoCloseable {

    private final Cursor cursor;
    private final UnmodifiableCursor wrapper;

    protected LazyCursorIterator(Cursor cursor){
        this(cursor, false);
    }

    protected LazyCursorIterator(Cursor cursor, boolean reset){
        this.cursor = cursor;
        this.wrapper = new UnmodifiableCursor(cursor);

        if(reset)
            reset();
    }

    @Override
    public boolean hasNext() {
        if(cursor.isClosed())
            throw new IllegalStateException("Cursor is closed");

        return cursor.moveToNext();
    }

    @Override
    public T next() {
        if(cursor.isClosed())
            throw new IllegalStateException("Cursor is closed");

        if(cursor.isAfterLast())
            throw new IllegalStateException("Cursor has no more entity");

        return handleEntity(wrapper);
    }

    public void reset(){
        cursor.moveToPosition(-1);
    }

    @Override
    public void close() throws Exception {
        wrapper.close();
    }

    @Override
    public void remove() {}

    /**
     * Handle unmarshalling of content in the cursor
     * @param cursor
     * @return
     */
    protected abstract T handleEntity(Cursor cursor);

    /**
     * Create a cursor which disable moving operations. Only the current position is accessible.
     */
    private class UnmodifiableCursor extends CursorWrapper{

        public UnmodifiableCursor(Cursor cursor) {
            super(cursor);
        }

        @Override
        public boolean move(int offset) {
            throw new IllegalStateException("Moving is forbidden");
        }

        @Override
        public boolean moveToFirst() {
            throw new IllegalStateException("Moving is forbidden");
        }

        @Override
        public boolean moveToLast() {
            throw new IllegalStateException("Moving is forbidden");
        }

        @Override
        public boolean moveToNext() {
            throw new IllegalStateException("Moving is forbidden");
        }

        @Override
        public boolean moveToPosition(int position) {
            throw new IllegalStateException("Moving is forbidden");
        }

        @Override
        public boolean moveToPrevious() {
            throw new IllegalStateException("Moving is forbidden");
        }
    }
}
