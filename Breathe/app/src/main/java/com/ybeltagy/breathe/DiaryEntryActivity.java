package com.ybeltagy.breathe;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;

public class DiaryEntryActivity extends AppCompatActivity {

    // key for Intent extra
    public static final String EXTRA_MESSAGE = "com.ybeltagy.breathe.extra.MESSAGE";
    // EditText view - where the user enters the diary entry message
    private EditText messageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_entry);
    }
}