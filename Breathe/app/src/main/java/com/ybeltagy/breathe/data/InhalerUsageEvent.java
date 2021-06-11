package com.ybeltagy.breathe.data;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.time.Instant;

/**
 * Entity (used in Room database) that represents an inhaler usage event.  Contains:
 * - a unique UTC ISO-8601 timestamp (when it occurred)
 * <p>
 * The following objects are "embedded" (represents an object we would like to decompose into its
 * sub-fields within a table) ->
 * <p>
 * - a DiaryEntry object: user comments and tags for the InhalerUsageEvent, if the info is available
 * <p>
 * - a WearableData object: environmental conditions from smart pin/wearable at the time of
 * the InhalerUsageEvent, if the information is available
 * <p>
 * - a WeatherData object: weather data from the Tomorrow.io API at the time of the InhalerUsageEvent,
 * if the information is available (https://docs.tomorrow.io/reference/welcome)
 * <p>
 */
@RequiresApi(api = Build.VERSION_CODES.O)
@Entity(tableName = "InhalerUsageEvent_table")
public class InhalerUsageEvent {
    @PrimaryKey // timeStamp is the unique identifier for each InhalerUsageEvent record
    @NonNull // this can never be null
    // name of the column in Room that stores timeStamp
    @ColumnInfo(name = "Inhaler_Usage_Event_UTC_ISO_8601_date_time")
    private Instant inhalerUsageEventTimeStamp = DataFinals.DEFAULT_INSTANT;
    // Note: the java.time package and the Instant class seem like the correct way to store
    // our dates for Java 8
    // (https://medium.com/decisionbrain/dates-time-in-modern-java-4ed9d5848a3e)

    @Embedded
    private DiaryEntry diaryEntry;
    @Embedded
    private WearableData wearableData;
    @Embedded
    private WeatherData weatherData;

    /**
     * - the @Ignore annotation is used so the Room database explicitly knows to use the
     * constructor with only the Instant timeStamp input parameter to construct InhalerUsageEvents
     * <p>
     * This annotation is necessary or the program will not compile.
     *
     * @param inhalerUsageEventTimeStamp when the inhaler usage event occurred
     * @param diaryEntry  object containing any user-entered message
     *                    and tag (RESCUE or PREVENTATIVE)
     * @param wearableData object containing local environmental data from the smart wearable from
     *                     the time of the inhaler usage event
     * @param weatherData object containing weather data from the Internet from the user's GPS
     *                    location at the time of the inhaler usage event
     */
    @Ignore
    public InhalerUsageEvent(@NonNull Instant inhalerUsageEventTimeStamp, DiaryEntry diaryEntry,
                             WearableData wearableData, WeatherData weatherData) {
        setInhalerUsageEventTimeStamp(inhalerUsageEventTimeStamp);
        setDiaryEntry(diaryEntry);
        setWearableData(wearableData);
        setWeatherData(weatherData);
    }

    /**
     * The Room database uses this constructor as it is not marked with an @Ignore annotation
     *
     * @param inhalerUsageEventTimeStamp when the inhaler usage event occurred
     */
    public InhalerUsageEvent(@NonNull Instant inhalerUsageEventTimeStamp) {
        this(inhalerUsageEventTimeStamp,
                new DiaryEntry(), new WearableData(), new WeatherData()
        );
    }

    // Note: getter methods are required by Room so it can instantiate InhalerUsageEvent objects
    @NonNull
    public Instant getInhalerUsageEventTimeStamp() {
        return inhalerUsageEventTimeStamp;
    }

    public void setInhalerUsageEventTimeStamp(@NonNull Instant inhalerUsageEventTimeStamp) {
        this.inhalerUsageEventTimeStamp = inhalerUsageEventTimeStamp;
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

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public void setWeatherData(WeatherData weatherData) {
        this.weatherData = weatherData;
    }
}
