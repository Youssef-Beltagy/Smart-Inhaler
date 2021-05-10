package com.ybeltagy.breathe.persistence;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.ybeltagy.breathe.data.DiaryEntry;
import com.ybeltagy.breathe.data.InhalerUsageEvent;
import com.ybeltagy.breathe.data.Tag;
import com.ybeltagy.breathe.data.WearableData;
import com.ybeltagy.breathe.data.WeatherData;

import java.time.Instant;
import java.util.List;

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
     * TODO: maybe we should never use this because it "clobbers" our existing IUEs (unless we
     * can also retrieve the existing inhalerUsageEvent, update the data and use the same
     * inhalerUsageEvent object as the input to this function) (might want to delete it)
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
                        weatherData.getWeatherPollen(),
                        weatherData.getWeatherAQI()));
    }

}
