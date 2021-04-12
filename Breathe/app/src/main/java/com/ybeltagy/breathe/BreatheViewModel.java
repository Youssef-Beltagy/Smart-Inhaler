package com.ybeltagy.breathe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * Class whose role is to act as a communication center between the Repository and the UI; provides
 * data and survives configuration changes
 */
public class BreatheViewModel extends AndroidViewModel {
    private BreatheRepository breatheRepository;
    private LiveData<List<InhalerUsageEvent>> allInhalerUsageEvents;


    public BreatheViewModel(@NonNull Application application) {
        super(application);
        breatheRepository = new BreatheRepository(application);
        allInhalerUsageEvents = breatheRepository.getAllInhalerUsageEvents();
    }

    LiveData<List<InhalerUsageEvent>> getAllInhalerUsageEvents() {
        return allInhalerUsageEvents;
    }

    public void insert(InhalerUsageEvent inhalerUsageEvent) {
        breatheRepository.insert(inhalerUsageEvent);
    }
}
