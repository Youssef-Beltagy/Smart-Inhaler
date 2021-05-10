package com.ybeltagy.breathe.data;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// UTC/Greenwich ISO-8601 Timestamp

import java.time.Instant;

/**
 * Entity (used in Room database) that represents an inhaler usage event.  Contains:
 * - a unique UTC ISO-8601 timestamp (when it occurred)
 * <p>
 * The following objects are "embedded" (represents an object we would like to decompose into its
 * sub-fields within a table) ->
 * - a WearableData object: environmental conditions from smart pin/wearable at the time of
 * the InhalerUsageEvent, if the information is available
 * <p>
 * - a DiaryEntry object: user comments and tags for the InhalerUsageEvent, if the info is available
 * <p>
 * - a WeatherData object: weather data from the Climacell API at the time of the InhalerUsageEvent,
 * if the information is available
 * <p>
 */
@RequiresApi(api = Build.VERSION_CODES.O)
@Entity(tableName = "InhalerUsageEvent_table")
public class InhalerUsageEvent {
    @PrimaryKey // timeStamp is the unique identifier for each InhalerUsageEvent record
    @NonNull // this can never be null
    // name of the column in Room that stores timeStamp
    @ColumnInfo(name = "Inhaler_Usage_Event_UTC_ISO_8601_date_time")
    private Instant inhalerUsageEventTimeStamp;
    // Note: the java.time package and the Instant class seem like the correct way to store
    // our dates for Java 8
    // (https://medium.com/decisionbrain/dates-time-in-modern-java-4ed9d5848a3e)

    @Embedded
    private WeatherData weatherData = new WeatherData();
    @Embedded
    private DiaryEntry diaryEntry = new DiaryEntry();
    @Embedded
    private WearableData wearableData = new WearableData();

    // @NonNull annotation means timeStamp parameter can never be null

    /**
     * TODO: We probably should delete this and use the constructor with only the timestamp
     * because we would never realistically use this (unless we are overwriting all of our
     * objects at once for some reason).
     * - the @Ignore annotation is used so the Room database explicitly knows to use the
     * constructor with only the Instant timeStamp input parameter to construct InhalerUsageEvents
     * This annotation is necessary or the program will not compile.
     *
     * @param inhalerUsageEventTimeStamp
     * @param weatherData
     * @param diaryEntry
     * @param wearableData
     */
    @Ignore
    public InhalerUsageEvent(@NonNull Instant inhalerUsageEventTimeStamp, WeatherData weatherData,
                             DiaryEntry diaryEntry, WearableData wearableData) {
        setInhalerUsageEventTimeStamp(inhalerUsageEventTimeStamp);;
        setWeatherData(weatherData);
        setDiaryEntry(diaryEntry);
        setWearableData(wearableData);
    }

    /**
     * The Room database uses this constructor as it is not marked with an @Ignore annotation
     *
     * @param inhalerUsageEventTimeStamp
     */
    public InhalerUsageEvent(@NonNull Instant inhalerUsageEventTimeStamp) {
        setInhalerUsageEventTimeStamp(inhalerUsageEventTimeStamp);
    }

    // Note: getter methods are required by Room so it can instantiate InhalerUsageEvent objects
    @NonNull
    public Instant getInhalerUsageEventTimeStamp() {
        return inhalerUsageEventTimeStamp;
    }

    public void setInhalerUsageEventTimeStamp(@NonNull Instant inhalerUsageEventTimeStamp) {
        this.inhalerUsageEventTimeStamp = inhalerUsageEventTimeStamp;
    }

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public void setWeatherData(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    public DiaryEntry getDiaryEntry() {
        return diaryEntry;
    }

    public void setDiaryEntry(DiaryEntry diaryEntry) {
        this.diaryEntry = diaryEntry;
    }

    public WearableData getWearableData() {
        return wearableData;
    }

    public void setWearableData(WearableData wearableData) {
        this.wearableData = wearableData;
    }
}
