package com.jacobobryant.scripturemastery;

import com.orm.androrm.DatabaseAdapter;

import com.orm.androrm.field.BlobField;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.OneToManyField;

import com.orm.androrm.Filter;
import com.orm.androrm.migration.Migrator;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

import android.content.Context;

import java.nio.ByteBuffer;
import java.util.*;

import android.content.SharedPreferences;

import android.preference.PreferenceManager;

public class Book extends Model {
    protected CharField title;
    protected OneToManyField<Book, Scripture> scriptures;
    protected BlobField routine;
    protected BooleanField preloaded;
    protected IntegerField position;
    private LinkedList<Integer> lstRoutine;

    public static final QuerySet<Book> objects(Context context) {
        return objects(context, Book.class);
    }

    public static Book object(Context context, int index) {
        Filter filter = new Filter().is("position", index);
        return objects(context, Book.class).filter(filter)
            .toList().get(0);
    }

    public Book() {
        super();
        title = new CharField();
        position = new IntegerField();
        preloaded = new BooleanField();
        routine = new BlobField();
        scriptures = new OneToManyField<Book, Scripture>(
                Book.class, Scripture.class);
    }

    private void loadRoutine() {
        if (lstRoutine != null) {
            return;
        }
        lstRoutine = new LinkedList<Integer>();
        if (routine.get() != null) {
            ByteBuffer buf = ByteBuffer.wrap(routine.get());
            while (buf.hasRemaining()) {
                lstRoutine.add(buf.getInt());
            }
        }
    }

    public Integer getPosition() {
        return position.get();
    }

    public void setPosition(Integer position) {
        this.position.set(position);
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }
    
    public QuerySet<Scripture> getScriptures(Context context) {
        return scriptures.get(context, this);
    }

    public Scripture getScripture(Context context, int index) {
        return scriptures.get(context, this).all().limit(index, 1)
            .toList().get(0);
    }

    public void addScripture(Scripture scrip) {
        scriptures.add(scrip);
    }
    
    public boolean getPreloaded() {
        return preloaded.get();
    }

    public void setPreloaded(boolean preloaded) {
        this.preloaded.set(preloaded);
    }

    public boolean hasKeywords(Context context) {
        if (scriptures.get(context, this).isEmpty()) {
            throw new UnsupportedOperationException("this book is empty");
        }
        String keywords = scriptures.get(context, this).all().limit(1)
            .toList().get(0).getKeywords();
        return (keywords != null && keywords.length() != 0);
    }

    public void createRoutine(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        float reviewPercent = Float.parseFloat(prefs.getString(
                SettingsActivity.REVIEW, 
                context.getString(R.string.pref_review_default)));
        List<Integer> notStarted = new ArrayList<Integer>();
        List<Integer> inProgress = new ArrayList<Integer>();
        List<Integer> finished = new ArrayList<Integer>();
        int reviewCount;
        Random rand = new Random();
        int index;
        List<Integer> list;

        for (Scripture scrip : scriptures.get(context, this).all()) {
            if (scrip.getStatus() == Scripture.NOT_STARTED) {
                list = notStarted;
            } else if (scrip.getStatus() == Scripture.IN_PROGRESS) {
                list = inProgress;
            } else {
                list = finished;
            }
            list.add(scrip.getId());
        }
        reviewCount = Math.round(reviewPercent * finished.size());
        if (finished.size() != 0 && reviewCount == 0) {
            reviewCount = 1;
        }
        lstRoutine = new LinkedList<Integer>();
        lstRoutine.addAll(inProgress);
        if (notStarted.size() > 0 &&
                inProgress.size() < Scripture.MAX_IN_PROGRESS) {
            lstRoutine.add(notStarted.get(0));
        }
        for (int i = 0; i < reviewCount; i++) {
            index = rand.nextInt(finished.size());
            lstRoutine.add(finished.remove(index));
        }
        save(context);
    }

    public String getRoutine(Context context) {
        StringBuilder sb = new StringBuilder();
        loadRoutine();
        if (lstRoutine.size() == 0) {
            return null;
        }
        sb.append(scriptures.get(context, this).get(lstRoutine.get(0))
            .getReference());
        for (int i = 1; i < lstRoutine.size(); i++) {
            sb.append("\n");
            sb.append(scriptures.get(context, this)
                    .get(lstRoutine.get(i)).getReference());
        }
        return sb.toString();
    }

    public String getDebugRoutine() {
        StringBuilder sb = new StringBuilder();
        loadRoutine();
        if (lstRoutine.size() == 0) {
            return "empty";
        }
        for (int index : lstRoutine) {
            sb.append(index);
            sb.append(' ');
        }
        return sb.toString();
    }

    public void deleteScripture(Scripture scrip, Context app) {
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(app);
        adapter.beginTransaction();
        loadRoutine();
        if (lstRoutine.remove(new Integer(scrip.getId()))) {
            save(app);
        }
        Filter f = new Filter().is("position", ">", scrip.getPosition());
        for (Scripture scripture :
                Scripture.objects(app).filter(f)) {
            scripture.setPosition(scripture.getPosition() - 1);
            scripture.save(app);
        }
        adapter.commitTransaction();
    }

    @Override
    public boolean save(Context app) {
        loadRoutine();
        if (lstRoutine.size() == 0) {
            routine.set(null);
        } else {
            ByteBuffer buf = ByteBuffer.allocate(lstRoutine.size() * 4);
            for (int i : lstRoutine) {
                buf.putInt(i);
            }
            routine.set(buf.array());
        }
        return super.save(app);
    }

    public int getRoutineLength() {
        loadRoutine();
        return lstRoutine.size();
    }

    public Scripture current(Context context) {
        loadRoutine();
        return scriptures.get(context, this).get(lstRoutine.element());
    }

    public void moveToNext(Context app) {
        loadRoutine();
        lstRoutine.remove();
        save(app);
    }

    // This is only needed for upgrading from a pre-androrm database.
    public void setRoutine(SyncDB.BookRecord book, Context app) {
        if (book.routine != null && book.routine.length() != 0) {
            lstRoutine = new LinkedList<Integer>();
            for (String index : book.routine.split(",")) {
                lstRoutine.add(book.scriptures
                        .get(Integer.parseInt(index)).id);
            }
            save(app);
        }
    }

    @Override
    protected void migrate(Context context) {
        Migrator<Book> migrator = new Migrator<Book>(Book.class);
        migrator.addField("position", new IntegerField());
        migrator.migrate(context);
    }
}
