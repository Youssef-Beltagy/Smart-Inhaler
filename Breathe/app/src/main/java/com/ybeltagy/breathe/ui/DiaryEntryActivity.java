package com.ybeltagy.breathe.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ybeltagy.breathe.R;
import com.ybeltagy.breathe.data.DataFinals;
import com.ybeltagy.breathe.data.DiaryEntry;
import com.ybeltagy.breathe.data.Tag;

import java.time.Instant;

public class DiaryEntryActivity extends AppCompatActivity {

    /**
     * DiaryEntryActivity's interactions with the data layer are through BreatheViewModel alone
     */
    private BreatheViewModel breatheViewModel = null;

    /**
     * Logging Tag
     */
    private static final String logTag = DiaryEntryActivity.class.getName();

    /**
     * IUE timestamp as a string
     */
    private String timestampString = null;
    private String diaryEntryMessage = null;
    private Tag iueTag = DataFinals.DEFAULT_TAG;

    /**
     * The edit box of the Diary Entry
     */
    private EditText messageEditText = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_entry);

        Log.d("DiaryEntryActivity", "super.onCreate() and setContentView() finished");

        // construct the view model to the Breathe DB
        breatheViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(BreatheViewModel.class);

        // The edit box for the diary
        messageEditText = findViewById(R.id.edit_diary_edittext);

        // get extras passed from the main activity
        final Bundle extras = getIntent().getExtras();

        // The clicked InhalerUsageEvent's Instant timestamp
        timestampString = extras.getString(UIFinals.EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_TIMESTAMP_KEY);

        // The existing message for this inhalerUsageEvent from the database
        diaryEntryMessage = extras.getString(UIFinals.EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_EXISTING_MESSAGE);

        // The exiting tag for this inhalerUsageEvent from the database
        // Ideally, we would like to display it too.
        iueTag = (Tag) extras.get(UIFinals.EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_TAG);

        setHeaderDateForInhalerUsageEvent(timestampString);
        setExistingDiaryMessage(diaryEntryMessage, messageEditText);

    }

    /**
     * Set text to show which InhalerUsageEvent the user is adding/editing a message to
     * - called from onCreate()
     *
     * @param timeStampString
     */
    private void setHeaderDateForInhalerUsageEvent(String timeStampString) {
        TextView eventTimeStamp = findViewById(R.id.entry_date_textview);
        eventTimeStamp.setText(String.format("Entry for %s", timeStampString));
    }

    /**
     * If there is already a message for this IUE, display it so the user may edit it
     *
     * @param diaryEntryMessage
     * @param messageEditText
     */
    private void setExistingDiaryMessage(String diaryEntryMessage, EditText messageEditText) {
        messageEditText.setText(diaryEntryMessage);
        messageEditText.setSelection(diaryEntryMessage.length());
        messageEditText.requestFocus();
    }

    public void onRescueButtonClick(View view) {
        iueTag = Tag.RESCUE;
    }

    public void onPreventativeButtonClick(View view) {
        iueTag = Tag.PREVENTATIVE;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onSaveButtonClick(View view) {
        DiaryEntry diaryEntry = new DiaryEntry(iueTag, messageEditText.getText().toString().trim());
        breatheViewModel.updateDiaryEntry(Instant.parse(timestampString), diaryEntry);
        finish(); // stop this activity
    }
}