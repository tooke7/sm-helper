package com.jacobobryant.scripturemastery;

import java.util.List;
import java.util.NoSuchElementException;

public class Book {
    private int id;
    private String title;
    private Scripture[] scriptures;
    private boolean preloaded;
    private Routine routine;
    public final int length;

	public Book(String title, Scripture[] scriptures, String strRoutine,
            int id, boolean preloaded) {
		this.title = title;
		this.scriptures = scriptures;
        for (Scripture scripture : scriptures) {
            scripture.setParent(this);
        }
        this.routine = (strRoutine == null) ?
                new Routine(scriptures) :
                new Routine(scriptures, strRoutine);
        this.id = id;
        this.preloaded = preloaded;
        this.length = scriptures.length;
	}

	public Book(String title, List<Scripture> scriptures, String routine,
            int id, boolean preloaded) {
        this(title, scriptures.toArray(new Scripture[scriptures.size()]),
                routine, id, preloaded);
    }

    public Book(String title, List<Scripture> scriptures) {
        this(title, scriptures, null, 0, false);
    }

    public Book(String title, Scripture[] scriptures) {
        this(title, scriptures, null, 0, false);
    }

    public Book(String title, Scripture scripture) {
        this(title, new Scripture[] { scripture });
    }

    public int getId() {
        return id;
    }

	public String getTitle() {
		return title;
	}
	
    public Scripture[] getScriptures() {
        return scriptures;
    }

    public Scripture getScripture(int index) {
        return scriptures[index];
    }

    public Scripture findScriptureById(int id) {
        for (Scripture scrip : scriptures) {
            if (scrip.getId() == id) {
                return scrip;
            }
        }
        throw new NoSuchElementException("Couldn't find element with " +
                "id = " + id);
    }

	@Override
	public String toString() {
		return "Book [title=" + title + ", scriptures.length="
				+ scriptures.length + "]";
	}

    public void createRoutine() {
        routine.newRoutine();
    }

    public Routine getRoutine() {
        return routine;
    }

    public boolean wasPreloaded() {
        return preloaded;
    }

    public boolean hasKeywords() {
        if (scriptures.length == 0) {
            throw new UnsupportedOperationException(
                    "this book has no children");
        }
        return (scriptures[0].getKeywords().length() > 0);
    }
}
