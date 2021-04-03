package com.ybeltagy.breathe;

import android.content.Context;

import androidx.room.Room;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.OffsetDateTime;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SimpleIueReadWriteTest {
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
    public void writeTrivialIueAndReadInList() {
        OffsetDateTime rightNow = OffsetDateTime.now();
        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(rightNow, null, null, null);

        tBreatheDao.insert(tInhalerUsageEvent);
        List<InhalerUsageEvent> byTimeStamp = tBreatheDao.getAllIUEs();

        assertEquals(byTimeStamp.get(0).getInhalerUsageEventTimeStamp(), rightNow);
    }
}