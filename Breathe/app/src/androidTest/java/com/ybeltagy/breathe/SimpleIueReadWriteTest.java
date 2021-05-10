package com.ybeltagy.breathe;

import android.content.Context;

import androidx.room.Room;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ybeltagy.breathe.persistence.BreatheDao;
import com.ybeltagy.breathe.persistence.BreatheRoomDatabase;
import com.ybeltagy.breathe.data.InhalerUsageEvent;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;

/**
 * A trivial test that checks if we can create, insert, and retrieve from the BreatheRoomDatabase
 */
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
        Instant rightNow = Instant.now();
        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(rightNow);

        tBreatheDao.insert(tInhalerUsageEvent);

        List<InhalerUsageEvent> byTimeStamp = tBreatheDao.getAllIUEsTest();

        assertEquals(byTimeStamp.get(0).getInhalerUsageEventTimeStamp(), rightNow);
    }
}
