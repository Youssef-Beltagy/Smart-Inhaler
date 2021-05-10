package com.ybeltagy.breathe.persistence;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.ybeltagy.breathe.data.InhalerUsageEvent;
import com.ybeltagy.breathe.data.WearableData;
import com.ybeltagy.breathe.data.Converters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The single database of our Breathe application
 * - contains the Inhaler_Usage_Event_table with InhalerUsageEvent entities
 * - contains the Wearable_Data_table with WearableData entities
 */
@Database(entities = {InhalerUsageEvent.class, WearableData.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class BreatheRoomDatabase extends RoomDatabase {

    public abstract BreatheDao breatheDao();

    private static volatile BreatheRoomDatabase INSTANCE; // this BreatheRoomDatabase is a singleton

    // Executor Service is used (replaced AsyncTask) so inserts may happen concurrently
    protected static final ExecutorService dbWriteExecutor = Executors.newCachedThreadPool();

    // creates a singleton BreatheRoomDatabase
    // (singleton to prevent multiple instances of the database being opened)
    public static BreatheRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BreatheRoomDatabase.class) {
                if (INSTANCE == null) {
                    // create database
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            BreatheRoomDatabase.class, "Breathe_database")
                            .addCallback(roomDatabaseCallback)
                            // Note: leaving implementation of migration strategy to future teams
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * IMPORTANT: The code for the RoomDatabase.Callback is for TESTING ONLY; this causes
     * the database to be populated with placeholder Inhaler usage events.
     * <p>
     * Please delete this code after the inhaler can successfully send IUEs to the app.
     */
    private static final RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            dbWriteExecutor.execute(() -> {
                // Populate the database with placeholder IUEs in the background.
                BreatheDao breatheDao = INSTANCE.breatheDao();

                if (breatheDao.getAnySingleInhalerUsageEvent().size() < 1) {
                    // Check if there are any placeholder IUEs in the database
                    // If there are none, make some placeholder IUEs and add them to the database
                    Instant now = Instant.now();
                    int dummyDataSize = 5;
                    for (int i = 0; i < dummyDataSize; i++) {
                        Instant aTime = now.minus( dummyDataSize - i, ChronoUnit.DAYS);
                        InhalerUsageEvent tInhalerUsageEvent = new InhalerUsageEvent(aTime);
                        Log.d("BreatheRoomDatabase", "adding InhalerUsageEvent "
                                + tInhalerUsageEvent.getInhalerUsageEventTimeStamp().toString());
                        breatheDao.insert(tInhalerUsageEvent);
                    }
                }
            });
        }
    };
}
