package com.ybeltagy.breathe.collection;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.ybeltagy.breathe.data.BreatheDao;
import com.ybeltagy.breathe.data.DiaryEntry;
import com.ybeltagy.breathe.data.InhalerUsageEvent;
import com.ybeltagy.breathe.data.WearableData;
import com.ybeltagy.breathe.data.WeatherData;

import com.ybeltagy.breathe.weather_data_collection.GPSWorker;
import com.ybeltagy.breathe.weather_data_collection.TaskDataFinals;
import com.ybeltagy.breathe.weather_data_collection.WeatherAPIWorker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The BreatheRepository class:
 * - Handles data operations -> provides clean API for app data
 * - Abstracts access to multiple data sources (such as getting data from a network or
 * cached data from local database)
 * - Manages query threads and allows the use of multiple backends (for future teams)
 */
public class BreatheRepository {
    private final BreatheDao breatheDao;
    private final LiveData<List<InhalerUsageEvent>> allInhalerUsageEvents;
    private static final String tag = BreatheRepository.class.getName();

    public BreatheRepository(Application app) {
        BreatheRoomDatabase breatheDB = BreatheRoomDatabase.getDatabase(app); // get handle to database
        breatheDao = breatheDB.breatheDao();
        allInhalerUsageEvents = breatheDao.getAllIUEs();
    }

    public LiveData<List<InhalerUsageEvent>> getAllInhalerUsageEvents() {
        return allInhalerUsageEvents;
    }

    /**
     * Wrapper for BreatheDao insert method
     * - inserts a single InhalerUsageEvent
     *
     * @param inhalerUsageEvent
     */
    public void insertIUE(final InhalerUsageEvent inhalerUsageEvent) {
        BreatheRoomDatabase.dbWriteExecutor.execute(() -> breatheDao.insert(inhalerUsageEvent));
    }

    /**
     * CAUTION: Using this "clobbers" our existing IUE's inner objects; use only if you wish this
     * to happen
     * <p>
     * wrapper for BreatheDao update method
     * - uses Executor Service (non-UI thread)
     *
     * @param inhalerUsageEvent
     */
    public void updateIUE(InhalerUsageEvent inhalerUsageEvent) {
        BreatheRoomDatabase.dbWriteExecutor.execute(()
                -> breatheDao.updateInhalerUsageEvent(inhalerUsageEvent));
    }

    /**
     * Note: Use this one to update an existing inhalerUsageEvent with DiaryEntry data so the
     * existing other inner objects (WearableData, WeatherData) don't get "clobbered"
     *
     * @param timeStamp
     * @param diaryEntry
     */
    public void updateDiaryEntry(Instant timeStamp, DiaryEntry diaryEntry) {
        BreatheRoomDatabase.dbWriteExecutor.execute(()
                -> breatheDao.updateDiaryEntry(timeStamp, diaryEntry.getTag(), diaryEntry.getMessage()));
    }

    /**
     * Note: Use this one to update an existing inhalerUsageEvent with WearableData data so the
     * existing other inner objects (DiaryEntry, WeatherData) don't get "clobbered"
     *
     * @param inhalerUsageTimeStamp
     * @param wearableData
     */
    public void updateWearableData(Instant inhalerUsageTimeStamp, WearableData wearableData) {
        BreatheRoomDatabase.dbWriteExecutor.execute(() ->
                breatheDao.updateWearableData(
                        inhalerUsageTimeStamp,
                        wearableData.getWearableDataTimeStamp(),
                        wearableData.getTemperature(),
                        wearableData.getHumidity(),
                        wearableData.getCharacter(),
                        wearableData.getDigit()));
    }

    /**
     * Updates an existing inhalerUsageEvent with WeatherData without overwriting
     * other existing inner objects (DiaryEntry, WeatherData)
     *
     * @param inhalerUsageTimeStamp
     * @param weatherData
     */
    public void updateWeatherData(Instant inhalerUsageTimeStamp, WeatherData weatherData) {
        BreatheRoomDatabase.dbWriteExecutor.execute(() ->
                breatheDao.updateWeatherData(
                        inhalerUsageTimeStamp,
                        weatherData.getWeatherTemperature(),
                        weatherData.getWeatherHumidity(),
                        weatherData.getWeatherPrecipitationIntensity(),
                        weatherData.getWeatherTreeIndex(),
                        weatherData.getWeatherGrassIndex(),
                        weatherData.getWeatherEPAIndex()));
    }


    /**
     * Saves the IUE into the database and uses the workmanager to collect the other data.
     * <p>
     * Unfortunately, the behavior of the workmanager once the app closes is
     * <a href=https://stackoverflow.com/questions/50682061/android-is-workmanager-running-when-app-is-closed>not well defined.</a>
     *
     * @param timestamp the IUE timestamp
     * @param context   the calling context
     */
    @SuppressLint("NewApi")
    public static void startDataCollection(Instant timestamp, Context context){ //fixme: made static temporarily

        InhalerUsageEvent iue = new InhalerUsageEvent(timestamp);

        // fixme: replace back after changing from static
        //insertIUE(iue);
        (new Thread() {
            public void run() {
                BreatheRoomDatabase.getDatabase(context).breatheDao().insert(iue);
            }
        }).start();

        // Get WearableData
        // - check if timestamp is <= 5 minutes old - if it is, get environmental data
        //   from the smart wearable
        Instant now = Instant.now();
        Instant wearableLimit = now.minus(5, ChronoUnit.MINUTES);
        if (!timestamp.isBefore(wearableLimit)) {
            wearableDataHelper(timestamp, context);
        }

        // Get WeatherData for this IUE:
        // - check if timestamp is <= 6 hours old (+ a 5 min cushion in case it takes a bit
        //   for the weather request to be made and/or retry); if it's older,
        //   we can't get historical weather data for it
        // - may be unnecessary but also check if this timestamp is erroneously from the future
        //   because then we won't be able to get weather data for it
        Instant weatherLimit = now.minus(6, ChronoUnit.HOURS)
                .plus(5, ChronoUnit.MINUTES);
        if (!timestamp.isBefore(weatherLimit) && timestamp.isBefore(now)) {
            getAndSaveWeatherDataHelper(timestamp, context);
        }
    }

    private static void wearableDataHelper(Instant timestamp, Context context) {
        WorkRequest wearableWorkRequest =
                new OneTimeWorkRequest
                        .Builder(WearableWorker.class)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .setInputData(
                                new Data.Builder().putString(
                                        TaskDataFinals.KEY_TIMESTAMP, timestamp.toString()).build())
                        // todo: Merge the weather data package and this package. It is not a good idea to refer to a final in another package.
                        .build();
        WorkManager
                .getInstance(context)
                .enqueue(wearableWorkRequest);
    }

    @SuppressLint("NewApi")
    private static void getAndSaveWeatherDataHelper(Instant timestamp, Context context) {
        // create work request for GPS
        WorkManager dataFlowManager = WorkManager.getInstance(context);
        OneTimeWorkRequest gpsRequest = new OneTimeWorkRequest
                .Builder(GPSWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS).build();

        //  create work request for online weather data for the GPS location
        //  and save it to the database
        Data requestWeather = new Data.Builder()
                .putString(TaskDataFinals.KEY_TIMESTAMP, timestamp.toString())
                .putBoolean( // true, save it to the database
                        TaskDataFinals.KEY_SAVE_WEATHER, true).build();

        OneTimeWorkRequest weatherAPIRequest =
                new OneTimeWorkRequest.Builder(WeatherAPIWorker.class)
                        .setInputData(requestWeather)
                        .build();

        dataFlowManager
                .beginWith(gpsRequest)
                .then(weatherAPIRequest)
                .enqueue();
    }

    public void clearIUEs(){

        BreatheRoomDatabase.dbWriteExecutor.execute(() ->
                breatheDao.deleteAll());

    }
}
