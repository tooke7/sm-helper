package com.jacobobryant.scripturemastery;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.util.*;

public class DataSource implements Closeable {
    private SQLiteDatabase db;
    private DBHandler handler;

    public DataSource(Context context) throws SQLException {
        handler = new DBHandler(context);
        open();
    }

    public void open() throws SQLException {
        db = handler.getWritableDatabase();
    }

    public void close() {
        handler.close();
    }

    public Book[] getBooks() {
        Cursor bookCursor;
        int id;
        String table;
        String title;
        String routine;
        boolean preloaded;
        Cursor scriptureCursor;
        List<Book> books = new ArrayList<Book>();
        List<Scripture> scriptures;

        bookCursor = db.rawQuery("SELECT _id, title, routine, " +
                "preloaded FROM books", null);
        bookCursor.moveToFirst();
        while (! bookCursor.isAfterLast()) {
            id = bookCursor.getInt(0);
            table = DBHandler.getTable(id);
            title = bookCursor.getString(1);
            routine = bookCursor.getString(2);
            preloaded = (bookCursor.getInt(3) == 1) ? true : false;
            scriptures = new ArrayList<Scripture>();
            scriptureCursor = db.rawQuery("SELECT _id, reference, " +
                    "keywords, verses, status, finishedStreak FROM " +
                    table, null);
            scriptureCursor.moveToFirst();
            while (! scriptureCursor.isAfterLast()) {
                scriptures.add(new Scripture(
                        scriptureCursor.getInt(0),
                        scriptureCursor.getString(1),
                        scriptureCursor.getString(2),
                        scriptureCursor.getString(3),
                        scriptureCursor.getInt(4),
                        scriptureCursor.getInt(5)));
                scriptureCursor.moveToNext();
            }
            scriptureCursor.close();
            books.add(new Book(title, scriptures, routine, id,
                    preloaded));
            bookCursor.moveToNext();
        }
        bookCursor.close();
        return books.toArray(new Book[books.size()]);
    }

    public Book getBook(int id) {
        Cursor bookCursor;
        String table;
        String title;
        String routine;
        boolean preloaded;
        Cursor scriptureCursor;
        List<Scripture> scriptures;

        bookCursor = db.rawQuery("SELECT title, routine, preloaded " +
                "FROM " + DBHandler.BOOKS + " WHERE _id = " + id, null);
        bookCursor.moveToFirst();
        if (bookCursor.isAfterLast()) {
            throw new NoSuchElementException("Couldn't find book with " +
                    "id = " + id);
        }
        table = DBHandler.getTable(id);
        title = bookCursor.getString(0);
        routine = bookCursor.getString(1);
        preloaded = (bookCursor.getInt(2) == 1) ? true : false;
        bookCursor.close();
        scriptures = new ArrayList<Scripture>();
        scriptureCursor = db.rawQuery("SELECT _id, reference, keywords, " +
                "verses, status, finishedStreak FROM " + table, null);
        scriptureCursor.moveToFirst();
        while (! scriptureCursor.isAfterLast()) {
            scriptures.add(new Scripture(
                    scriptureCursor.getInt(0),
                    scriptureCursor.getString(1),
                    scriptureCursor.getString(2),
                    scriptureCursor.getString(3),
                    scriptureCursor.getInt(4),
                    scriptureCursor.getInt(5)));
            scriptureCursor.moveToNext();
        }
        scriptureCursor.close();
        return new Book(title, scriptures, routine, id, preloaded);
    }

    public void commit(Scripture scripture) {
        db.execSQL(String.format("UPDATE %s SET status=%d," +
                "finishedStreak=%d WHERE _id=%d", DBHandler.getTable(
                scripture.getParent().getId()), scripture.getStatus(),
                scripture.getFinishedStreak(), scripture.getId()));
    }

    public void commit(Book book) {
        Routine routine = book.getRoutine();
        String strRoutine = (routine.length() == 0) ?
                "null" : "\"" + routine + "\"";
        db.execSQL(String.format("UPDATE books SET routine=%s " +
                "WHERE _id=%d", strRoutine, book.getId()));
    }

    public void addGroup(Book book) {
        handler.addBook(db, book);
    }

    public void addPassage(Scripture passage) {
        int id = passage.getParent().getId();
        handler.addScripture(db, DBHandler.getTable(id), passage);
    }

    public void deletePassage(Scripture passage) {
        String table = DBHandler.getTable(passage.getParent().getId());
        int id = passage.getId();
        db.execSQL("DELETE FROM " + table + " WHERE _id = " + id);
    }

    public void deleteGroup(Book group) {
        String table = DBHandler.getTable(group.getId());
        int id = group.getId();
        db.execSQL("DROP TABLE " + table);
        db.execSQL("DELETE FROM books WHERE _id = " + id);
    }
}
