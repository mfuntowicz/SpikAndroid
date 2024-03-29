package com.funtowiczmo.spik.utils;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.Iterator;

/**
 * Created by mfuntowicz on 29/10/15.
 */
public abstract class CursorIterator<T> implements Iterator<T>, AutoCloseable {

    private final Cursor cursor;
    private final UnmodifiableCursor wrapper;

    protected CursorIterator(Cursor cursor){
        this(cursor, false);
    }

    protected CursorIterator(Cursor cursor, boolean reset){
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

        if(cursor.isBeforeFirst())
            throw new IllegalStateException("Cursor is before the first row, you might be missing to call hasNext()");

        return fillFromCursor(wrapper);
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
    protected abstract T fillFromCursor(Cursor cursor);

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
