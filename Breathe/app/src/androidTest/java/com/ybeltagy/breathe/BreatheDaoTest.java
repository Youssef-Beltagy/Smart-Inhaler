package com.ybeltagy.breathe;

import android.content.Context;

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
 *
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
        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(rightNow,
                null, null, null);
        tBreatheDao.insert(tInhalerUsageEvent);

        // updated InhalerUsageEvent

        // simulating we gathered the wearableData 3 minutes later
        WearableData wearableData = new WearableData(rightNow.plus(3, ChronoUnit.MINUTES));
        float testTemp = 100;
        wearableData.setTemperature(testTemp);
        tInhalerUsageEvent.setWearableData(wearableData);
        tBreatheDao.updateInhalerUsageEvent(tInhalerUsageEvent);

        List<InhalerUsageEvent> allEvents = tBreatheDao.getAllIUEs();
        assertEquals(allEvents.get(0).getWearableData().getTemperature(), testTemp, 0);

        tBreatheDao.deleteAll();
    }

    @Test
    public void eventsBetweenDatesTest() {
        Instant now = Instant.now();
        // create some inhaler usage events
        for (int i = 0; i < 10; i++) {
            Instant aTime = now.minus(i, ChronoUnit.DAYS);
            InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(aTime, null,
                    null, new WearableData(aTime.plus(3, ChronoUnit.MINUTES)));
            tBreatheDao.insert(tInhalerUsageEvent);
        }

        // query between dates
        int week = 7;
        List<InhalerUsageEvent> thisPastWeek =
                tBreatheDao.loadAllInhalerUsageEventsBetweenDates(now.minus(week, ChronoUnit.DAYS), now);
        assertEquals(thisPastWeek.size(), week + 1); // note: the range is inclusive

        tBreatheDao.deleteAll();
    }
}
