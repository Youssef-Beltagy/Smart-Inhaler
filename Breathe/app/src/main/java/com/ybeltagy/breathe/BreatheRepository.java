package com.ybeltagy.breathe;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * The BreatheRepository class:
 * - Handles data operations -> provides clean API for app data
 * - Abstracts access to multiple data sources (such as getting data from a network or
 * cached data from local database)
 * - Manages query threads and allows the use of multiple backends (for future teams)
 */
public class BreatheRepository {
    private BreatheDao breatheDao;
    private LiveData<List<InhalerUsageEvent>> allInhalerUsageEvents;


    BreatheRepository(Application app) {
        BreatheRoomDatabase breatheDB = BreatheRoomDatabase.getDatabase(app); // get handle to database
        breatheDao = breatheDB.breatheDao();
        allInhalerUsageEvents = breatheDao.getAllIUEs();
    }

    LiveData<List<InhalerUsageEvent>> getAllInhalerUsageEvents() {
        return allInhalerUsageEvents;
    }

    // wrapper for BreatheDao insert method
    // - we must do this on a non-UI thread (or the app will crash)
    // - Executor Service is used so this occurs concurrently
    public void insert(InhalerUsageEvent inhalerUsageEvent) {
        BreatheRoomDatabase.dbWriteExecutor.execute( () -> breatheDao.insert(inhalerUsageEvent));
    }

}
