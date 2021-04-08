package com.ybeltagy.breathe;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// UTC/Greenwich ISO-8601 Timestamp
import java.time.Instant;

/**
 * Entity (used in Room database) that represents an inhaler usage event.  Contains:
 * - a unique UTC ISO-8601 timestamp (when it occurred)
 * <p>
 * The following objects are "embedded" (represents an object we would like to decompose into its
 * sub-fields within a table) ->
 * - a WearableData object: environmental conditions from smart pin/wearable at the time of the InhalerUsageEvent
 * , if the information is available
 * <p>
 * - a DiaryEntry object: user comments and tags for the InhalerUsageEvent, if the info is available
 * <p>
 * - a WeatherData object: weather data from the Climacell API at the time of the InhalerUsageEvent, if the
 * information is available
 * <p>
 * TODO: create static InhalerUsageEvent createIUE() to collect all data for InhalerUsageEvent and return an InhalerUsageEvent object
 */
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

    // I made these public because all of the Embedded examples I saw were public
    // For more info on Embedded objects within Entities,
    // see https://developer.android.com/training/data-storage/room/relationships
    // TODO: it should be possible to make them private as long as we provide setters and getters. Consider this when refactoring.
    @Embedded private WeatherData weatherData;
    @Embedded private DiaryEntry diaryEntry;
    @Embedded private WearableData wearableData;

    // @NonNull annotation means timeStamp parameter can never be null
    public InhalerUsageEvent(@NonNull Instant inhalerUsageEventTimeStamp, WeatherData weatherData,
                             DiaryEntry diaryEntry, WearableData wearableData) {
        this.inhalerUsageEventTimeStamp = inhalerUsageEventTimeStamp;
        this.weatherData = weatherData;
        this.diaryEntry = diaryEntry;
        this.wearableData = wearableData;
    }

    // Note: getter methods are required by Room so it can instantiate InhalerUsageEvent objects
    @NonNull
    public Instant getInhalerUsageEventTimeStamp() {
        return inhalerUsageEventTimeStamp;
    }
    // Although this function is "unused", the database needs it to exist (or else the compiler
    // complains)
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
