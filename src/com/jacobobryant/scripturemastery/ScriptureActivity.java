package com.jacobobryant.scripturemastery;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class ScriptureActivity extends Activity {
    public static final String PASSAGE_BUNDLE = "passageBundle";
    public static final int RESULT_MEMORIZED = RESULT_FIRST_USER;
    public static final int RESULT_PARTIALLY_MEMORIZED =
                                                    RESULT_FIRST_USER + 1;
    public static final int RESULT_MASTERED = RESULT_FIRST_USER + 2;
    private static final int ROUTINE_DIALOG = 0;
    private static final int PROGRESS_DIALOG = 1;
    private long touchTime;
    private String routine;
    private Passage passage;
    int progress;

    public class TouchListener implements OnTouchListener {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handlePress();
                    break;
                case MotionEvent.ACTION_UP:
                    handleRelease();
                    break;
            }
            return false;
        }

        public void handlePress() {
            final int DOUBLE_TAP_WINDOW = 300;
            long time = SystemClock.uptimeMillis();

            if (touchTime + DOUBLE_TAP_WINDOW > time) {
                passage.setHintActive(true);
                setText();
            }
            touchTime = time;
        }

        public void handleRelease() {
            if (passage.hintActive()) {
                passage.setHintActive(false);
                setText();
            }
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.scripture_activity);
        LayoutInflater inflater = LayoutInflater.from(this);
        ViewGroup layout = (ViewGroup) findViewById(R.id.layout);
        TextView lblVerse;
        Scripture scripture = MainActivity.getScripture();
        Paint defaultPaint = ((TextView)
                inflater.inflate(R.layout.verse, null)).getPaint();
        View scrollView = findViewById(R.id.scroll);
        Bundle passageBundle;

        if (scripture == null) {
            setResult(RESULT_CANCELED);
            finish();
        }
        // there appears to be a bug in the Bundle.get*() methods. They
        // shouldn't throw NullPointerExceptions, but they do.
        try {
            passageBundle = state.getBundle(PASSAGE_BUNDLE);
        } catch (NullPointerException e) {
            passageBundle = null;
        }

        passage = (passageBundle == null) ?
                new Passage(scripture, defaultPaint) :
                new Passage(scripture, defaultPaint, passageBundle);
        routine = scripture.getParent().getRoutine().toString(true);
        setTitle(scripture.getReference());
        touchTime = 0;
        for (int i = 0; i < passage.getParagraphs().length; i++) {
            lblVerse = (TextView)
                    inflater.inflate(R.layout.verse, null);
            layout.addView(lblVerse);
        }
        scrollView.setOnTouchListener(new TouchListener());
        setText();
        progress = RESULT_MEMORIZED;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBundle(PASSAGE_BUNDLE, passage.getBundle());
        super.onSaveInstanceState(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scripture_activity_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (! passage.hasMoreLevels()) {
            menu.findItem(R.id.mnuIncreaseLevel).setVisible(false);
        }
        if (routine == null) {
            menu.findItem(R.id.mnuRoutine).setVisible(false);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuIncreaseLevel:
                passage.increaseLevel();
                setText();
                return true;
            case R.id.mnuDone:
                showDialog(PROGRESS_DIALOG);
                return true;
            case R.id.mnuRoutine:
                showDialog(ROUTINE_DIALOG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case ROUTINE_DIALOG:
                builder.setTitle(R.string.routineDialog)
                        .setMessage(routine)
                        .setPositiveButton(android.R.string.ok, null);
                break;
            case PROGRESS_DIALOG:
                buildProgressDialog(builder);
                break;
        }
        return builder.create();
    }

    private void buildProgressDialog(AlertDialog.Builder builder) {
        builder.setTitle(R.string.progressDialog)
                .setSingleChoiceItems(R.array.progress, 0, 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int which) {
                        // the cases correspond to the items in
                        // R.array.progress
                        switch (which) {
                            case 0:
                                progress = RESULT_MEMORIZED;
                                break;
                            case 1:
                                progress = RESULT_PARTIALLY_MEMORIZED;
                                break;
                            case 2:
                                progress = RESULT_MASTERED;
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setResult(progress);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
    }

    public void setText() {
        ViewGroup layout = (ViewGroup) findViewById(R.id.layout);
        String[] verses = passage.getParagraphs();
        for (int i = 0; i < verses.length; i++) {
            ((TextView) layout.getChildAt(i)).setText(verses[i]);
        }
    }
}