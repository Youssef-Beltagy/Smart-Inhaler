package com.ybeltagy.breathe;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Simple tests for updating entities in the DAO and querying between dates
 * <p>
 * Note: BreatheRepository is difficult to unit test (so this step has been skipped) as the
 * application must be passed to its constructor
 */
@RunWith(AndroidJUnit4.class)
public class BreatheDaoTest {
    private BreatheDao tBreatheDao;
    private BreatheRoomDatabase tBreatheRoomDatabase;

    @Before
    public void createDB() {
        Context context = ApplicationProvider.getApplicationContext();
        tBreatheRoomDatabase = Room.inMemoryDatabaseBuilder(context, BreatheRoomDatabase.class).build();
        tBreatheDao = tBreatheRoomDatabase.breatheDao();
    }

    @After
    public void closeDB() {
        tBreatheDao.deleteAll(); // delete the records in the test database
        tBreatheRoomDatabase.close();
    }

    @Test
    public void updateInhalerUsageEventTest() {
        Instant rightNow = Instant.now();
        // original InhalerUsageEvent
        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(rightNow);
        tBreatheDao.insert(tInhalerUsageEvent);

        // updated InhalerUsageEvent

        // simulating we gathered the wearableData 3 minutes later
        WearableData wearableData = new WearableData(rightNow.plus(3, ChronoUnit.MINUTES));
        float testTemp = 100;
        wearableData.setTemperature(testTemp);
        tInhalerUsageEvent.setWearableData(wearableData);
        tBreatheDao.updateInhalerUsageEvent(tInhalerUsageEvent);

        List<InhalerUsageEvent> allEvents = tBreatheDao.getAllIUEsTest();
        assert allEvents != null;
        assertEquals(allEvents.get(0).getWearableData().getTemperature(),
                testTemp,
                0);

        tBreatheDao.deleteAll();
    }

    @Test
    public void updateTwiceInhalerUsageEventTest() {
        Instant rightNow = Instant.now();
        // original InhalerUsageEvent
        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(rightNow);
        tBreatheDao.insert(tInhalerUsageEvent);

        // simulating we gathered the wearableData 3 minutes later
        WearableData wearableData = new WearableData(rightNow.plus(3, ChronoUnit.MINUTES));
        float testTemp = 100;
        wearableData.setTemperature(testTemp);
        tInhalerUsageEvent.setWearableData(wearableData);
        tBreatheDao.updateInhalerUsageEvent(tInhalerUsageEvent);

        // now add diary entry activity with a new InhalerUsageEvent object with the same timestamp
        // (primary key) as the record we want to update
        DiaryEntry diaryEntry = new DiaryEntry(Tag.PREVENTATIVE, "Test Test");
        tInhalerUsageEvent.setDiaryEntry(diaryEntry);
        tBreatheDao.updateInhalerUsageEvent(tInhalerUsageEvent);

        List<InhalerUsageEvent> allEvents = tBreatheDao.getAllIUEsTest();
        assert allEvents != null;
        assertEquals(allEvents.size(), 1);
        // was the wearable data preserved when we updated the diary entry?
        assertEquals(allEvents.get(0).getWearableData().getTemperature(),
                testTemp,
                0);
        assertEquals(allEvents.get(0).getDiaryEntry().getTag(), Tag.PREVENTATIVE);
        tBreatheDao.deleteAll();
    }

    @Test
    public void updateDiaryEntryTest() {
        Instant rightNow = Instant.now();
        // original InhalerUsageEvent
        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(rightNow);
        tBreatheDao.insert(tInhalerUsageEvent);

        // update only the diary entry using only the timestamp string of the Instant
        String timeStamp = rightNow.toString();
        tBreatheDao.updateDiaryEntry(rightNow, Tag.PREVENTATIVE, "test test");

        // simulating we gathered the wearableData 3 minutes later
        float testTemp = 100;
        float testHumidity = 50;
        char testChar1 = '1';
        char testChar2 = '2';
        tBreatheDao.updateWearableData(
                rightNow, rightNow.plusSeconds(180), testTemp, testHumidity, testChar1, testChar2);

        List<InhalerUsageEvent> allEvents = tBreatheDao.getAllIUEsTest();
        assert allEvents != null;
        assertEquals(allEvents.size(), 1);

        // was the wearable data preserved when we updated the diary entry?
        assertEquals(allEvents.get(0).getWearableData().getTemperature(),
                testTemp,
                0);
        assertEquals(
                allEvents.get(0).getWearableData().getWearableDataTimeStamp(),
                rightNow.plusSeconds(180));

        // was the diary entry data preserved?
        assertEquals(allEvents.get(0).getDiaryEntry().getTag(), Tag.PREVENTATIVE);
        assertEquals(allEvents.get(0).getDiaryEntry().getMessage(), "test test");
        tBreatheDao.deleteAll();
    }

    @Test
    public void getAnySingleInhalerUsageEventTest() {
        tBreatheDao.deleteAll();
        assertEquals(tBreatheDao.getAnySingleInhalerUsageEvent().size(), 0);

        Instant now = Instant.now();
        // create some inhaler usage events
        for (int i = 0; i < 10; i++) {
            Instant aTime = now.minus(i, ChronoUnit.DAYS);
            InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(aTime, null,
                    null, new WearableData(aTime.plus(3, ChronoUnit.MINUTES)));
            tBreatheDao.insert(tInhalerUsageEvent);
        }
        assertEquals(tBreatheDao.getAnySingleInhalerUsageEvent().size(), 1);
    }

    @Test
    public void eventsBetweenDatesTest() {
        Instant now = Instant.now();
        // create some inhaler usage events
        for (int i = 0; i < 10; i++) {
            Instant aTime = now.minus(i, ChronoUnit.DAYS);
            InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(aTime, null,
                    null, new WearableData(
                    aTime.plus(3, ChronoUnit.MINUTES)));
            tBreatheDao.insert(tInhalerUsageEvent);
        }

        // query between dates
        int week = 7;
        List<InhalerUsageEvent> thisPastWeek =
                tBreatheDao.loadAllInhalerUsageEventsBetweenDatesTest(
                        now.minus(week, ChronoUnit.DAYS), now);
        assert thisPastWeek != null;
        assertEquals(thisPastWeek.size(),
                week + 1); // note: the range is inclusive

        tBreatheDao.deleteAll();
    }
}
