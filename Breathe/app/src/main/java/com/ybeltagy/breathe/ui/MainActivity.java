package com.ybeltagy.breathe.ui;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.os.Bundle;

import com.ybeltagy.breathe.ble.BLEService;
import com.ybeltagy.breathe.data.InhalerUsageEvent;
import com.ybeltagy.breathe.R;

import com.ybeltagy.breathe.weather_data_collection.CollectWeatherData;
import com.ybeltagy.breathe.weather_data_collection.GPSWorker;
import com.ybeltagy.breathe.weather_data_collection.TaskDataFinals;
import com.ybeltagy.breathe.weather_data_collection.WeatherAPIWorker;

import com.ybeltagy.breathe.data.Level;
import com.ybeltagy.breathe.data.WeatherData;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.ybeltagy.breathe.weather_data_collection.TaskDataFinals.KEY_WEATHER_DATA_RESULT;

/**
 * This activity contains the main logic of the Breathe app. It renders the UI and registers a
 * Bluetooth Broadcast receiver to listen for Bluetooth connection and disconnection to the phone
 * (to be changed to BLE).
 */
public class MainActivity extends AppCompatActivity {

    // MainActivity's interactions with the data layer are through BreatheViewModel alone
    private BreatheViewModel breatheViewModel; // view model for the recyclerview

    private static final String tag = MainActivity.class.getName(); // Maybe we can use this, going forward.

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //fixme: is there a better way to detect the app was opened?
        Intent foregroundServiceIntent = new Intent(this, BLEService.class);
        startForegroundService(foregroundServiceIntent); //FIXME why does it take a context too?

        // Note: constructor in code lab did not work; searched for a long time and this fixed it:
        // https://github.com/googlecodelabs/android-room-with-a-view/issues/145#issuecomment-739756244
        breatheViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(BreatheViewModel.class);

        // ProgressBar
        renderMedStatusView(UIFinals.TOTAL_DOSES_IN_CANISTER);

        // RecyclerView
        // populate the fake data for the RecyclerView using
        renderDiaryView();

        // WorkManager -> gets WeatherData
        weatherDataFlow();
    }


    /** // Todo: move this logic to the repository and access it through the viewModel
     * Get GPS and pass latitude,longitude to tomorrow.io to get weather info
     */
    @SuppressLint("NewApi")
    private void weatherDataFlow() {
        // create work request for GPS
        WorkManager dataFlowManager = WorkManager.getInstance(getApplication());
        OneTimeWorkRequest gpsRequest = new OneTimeWorkRequest.Builder(GPSWorker.class)
                .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS).build();

        //  create work request for online weather data for the GPS location
        Data requestWeather = new Data.Builder()
                .putString(TaskDataFinals.KEY_TIMESTAMP, Instant.now().toString())
                .putBoolean( // false, don't save to the database
                                TaskDataFinals.KEY_SAVE_WEATHER, false).build();

       OneTimeWorkRequest weatherAPIRequest =
                new OneTimeWorkRequest.Builder(WeatherAPIWorker.class)
                        .setInputData(requestWeather)
                        .build();

        dataFlowManager.beginWith(gpsRequest).then(weatherAPIRequest).enqueue();

        observerWeatherAPIWorkerForDisplay(weatherAPIRequest.getId());
    }

    /**
     * Observe the WeatherAPIWorker for weather data results
     *
     * @param weatherAPIRequestID
     */
    private void observerWeatherAPIWorkerForDisplay(UUID weatherAPIRequestID) {
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(weatherAPIRequestID)
                .observe(this, info -> {

                    if (info != null && info.getState().isFinished()) {
                        Log.d(tag, "Got data back from WeatherAPITask");

                        String responseJSONWeatherString
                                = info.getOutputData().getString(KEY_WEATHER_DATA_RESULT);
                        Log.d(tag, "Serialized object string: " + responseJSONWeatherString);

                        if (responseJSONWeatherString != null) {
                            WeatherData weatherData = CollectWeatherData
                                    .responseJSONToWeatherData(responseJSONWeatherString);

                            if (weatherData != null) {
                                displayWeatherData(weatherData);
                            }
                        }
                    }
                });
    }

    /**
     * Display weather data from tomorrow.io to the UI
     *
     * @param weatherData
     */
    private void displayWeatherData(WeatherData weatherData) {
        // check if weatherData request came back valid or not
        if (!weatherData.isDataValid()) {
            return;
        }

        TextView humidityText = findViewById(R.id.humidity_textview);
        humidityText.setText(String.format("%s%%", weatherData.getWeatherHumidity()));

        // max level between tree and grass pollen (for display simplicity)
        TextView pollen = findViewById(R.id.pollen_textview);
        Level maxPollen = Level.maxLevelOfTwo(
                weatherData.getWeatherGrassIndex(), weatherData.getWeatherTreeIndex());
        pollen.setText(maxPollen.toString());

        // precipitation (mm/hr)
        TextView precipitationText = findViewById(R.id.precipitation_textview);
        precipitationText
                .setText(String.format("%s mm/hr", weatherData.getWeatherPrecipitationIntensity()));
        Log.d(tag, "Set precipitation to -> " + precipitationText.getText());

        // temperature in Celsius
        TextView temperatureText = findViewById(R.id.temperature_textview);
        temperatureText.setText(String.format("%s °C",
                weatherData.getWeatherTemperature()));

        // AQI(a.k.a. EPA Index)
        TextView aqiText = findViewById(R.id.aqi_textview);
        Log.d(tag, "AQI was -> " + weatherData.getWeatherEPAIndex());
        // Note: setText() fails if the input is not a String (a plain old int
        // or something)
        aqiText.setText(String.format("Index : %d", weatherData.getWeatherEPAIndex()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the toolbar menu.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //Handle toolbar clicks.

        int id = item.getItemId(); // which item was clicked.

        if(id == R.id.settings_menuitem){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent); // fixme: should this be for a result?
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * render the Progress Bar for the medicine status in first pane (top of the screen)
     *
     * @param totalDosesInCanister number of boluses in the replaceable inhaler cartridge
     */
    private void renderMedStatusView(int totalDosesInCanister) {
        ProgressBar medicineStatusBar = findViewById(R.id.doses_progressbar);

        // update max amount of progress bar to number of doses in a full medicine canister
        medicineStatusBar.setMax(totalDosesInCanister);

        //Get text view to set the text for.
        TextView dosesTakenText = findViewById(R.id.doses_textview);

        breatheViewModel.getAllInhalerUsageEvents().observe(this, inhalerUsageEvents -> {
            // medicine left is number of doses in a full container - doses used
            medicineStatusBar.setProgress(medicineStatusBar.getMax() - inhalerUsageEvents.size());
            dosesTakenText.setText(String.format(Locale.ENGLISH,"%d / %d", medicineStatusBar.getProgress(), medicineStatusBar.getMax()));
        });
    }

    /**
     * render the diary RecyclerView for the diary timeline of events in third pane
     */
    @SuppressLint("NewApi")
    private void renderDiaryView() {

        Log.d(tag, "DiaryView starting to render...");

        RecyclerView iueRecyclerView = findViewById(R.id.diary_recyclerview);

        IUEListAdapter iueListAdapter = new IUEListAdapter(this); // make adapter and provide data to be displayed

        // set an on-click listener so we can get the InhalerUsageEvent at the clicked position and
        // pass it to the DiaryEntryActivity
        iueListAdapter.setOnItemClickListener((v, position) -> launchDiaryEntryActivity(iueListAdapter.getInhalerUsageEventAtPosition(position)));

        iueRecyclerView.setAdapter(iueListAdapter); // connect adapter and recyclerView

        // set layout manager for recyclerView
        iueRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Updated cached copy of InhalerUsageEvents
        breatheViewModel.getAllInhalerUsageEvents().observe(this, inhalerUsageEvents -> {
            // update all the dataset of the recycler view for any change in the inhaler usage events.
            // I'm not sure this is the most efficient way to do it, but it is guaranteed to work.
            iueListAdapter.setIUEs(inhalerUsageEvents);
            Log.d(tag, "database changed - added IUEs");
        });
    }

    /**
     * Opens the diary entry activity for the passed inhalerUsageEvent.
     * todo: as the project develops, consider not not passing anymore than than IUE timestamp in the intent and retrieving
     *  everything from the database in the DiaryEntryActivity
     * @param inhalerUsageEvent the iue to edit the diary for
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void launchDiaryEntryActivity(InhalerUsageEvent inhalerUsageEvent) {

        // don't do anything for invalid input.
        if (inhalerUsageEvent == null)
            return;

        Log.d(tag, "Launching diary entry activity!");

        Intent intent = new Intent(this, DiaryEntryActivity.class);

        // send timestamp
        intent.putExtra(UIFinals.EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_TIMESTAMP_KEY,
                inhalerUsageEvent.getInhalerUsageEventTimeStamp().toString());

        intent.putExtra(UIFinals.EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_EXISTING_MESSAGE,
                inhalerUsageEvent.getDiaryEntry().getMessage());

        intent.putExtra(UIFinals.EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_TAG,
                inhalerUsageEvent.getDiaryEntry().getTag());

        startActivityForResult(intent, UIFinals.UPDATE_INHALER_USAGE_EVENT_REQUEST_CODE);

    }

}