package com.ybeltagy.breathe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ProgressBar;
import android.widget.TextView;

import android.os.Bundle;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * This activity contains the main logic of the Breathe app. It renders the UI and registers a
 * Bluetooth Broadcast receiver to listen for Bluetooth connection and disconnection to the phone
 * (to be changed to BLE).
 */
public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // RecyclerView ----------------------------------------------------------------------------
        // populate the fake data for the RecyclerView

        // fake data - assumed number of doses in a canister
        // TODO: Replace with real number of doses in a canister
        // todo: or put that in the resources folder if we will continue using it.
        int totalDosesInCaniser = 200;

        // todo: replace with dynamically loaded data from the database
        LinkedList<String> eventList = new LinkedList<>();
        for (int i = 1; i <= 20; i++) {
            Date date = new Date(2020, 10, i); // todo: avoid using Date because it is partially deprecated.
            eventList.addLast(date.toString());
        }
        renderDiaryView(eventList);

        // ProgressBar -----------------------------------------------------------------------------
        renderMedStatusView(eventList, totalDosesInCaniser);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // render the Progress Bar for the medicine status in first pane (top of the screen)
    private void renderMedStatusView(List<String> eventList, int totalDosesInCaniser) {
        ProgressBar medicineStatusBar = findViewById(R.id.doses_progressbar);
        // update max amount of progress bar to number of doses in a full medicine canister
        medicineStatusBar.setMax(totalDosesInCaniser);
        // set doses taken shown in the ProgressBar to fake data increment (20 fake IUE events)
        // TODO: Delete fake data increment, replace with real increment (# of newly synced IUEs)
        medicineStatusBar.setProgress(eventList.size());

        // set text to show how many doses have been taken
        TextView dosesTakenText = findViewById(R.id.doses_textview);
        dosesTakenText.setText(String.format("%d / %d", medicineStatusBar.getProgress(), medicineStatusBar.getMax()));
    }

    // render the diary RecyclerView for the diary timeline of events in third pane
    // (bottom of screen)
    private void renderDiaryView(List<String> eventList) { // Use interfaces instead of actual classes
        RecyclerView iueRecyclerView = findViewById(R.id.diary_recyclerview);
        // make adapter and provide data to be displayed
        IUEListAdapter iueListAdapter = new IUEListAdapter(this, eventList);
        // connect adapter and recyclerView
        iueRecyclerView.setAdapter(iueListAdapter);
        // set layout manager for recyclerView
        iueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}