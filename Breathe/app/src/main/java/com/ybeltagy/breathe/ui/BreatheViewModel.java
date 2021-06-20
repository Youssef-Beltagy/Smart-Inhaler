package com.ybeltagy.breathe.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.ybeltagy.breathe.collection.Export;
import com.ybeltagy.breathe.data.DiaryEntry;
import com.ybeltagy.breathe.collection.BreatheRepository;
import com.ybeltagy.breathe.data.InhalerUsageEvent;

import java.time.Instant;
import java.util.List;

/**
 * Class whose role is to act as a communication center between the Repository and the UI; provides
 * data and survives configuration changes.
 *
 * As this class's main purpose is to provide an interface for the UI, it only has the methods the UI needs.
 */
public class BreatheViewModel extends AndroidViewModel {
    private final BreatheRepository breatheRepository;
    private final LiveData<List<InhalerUsageEvent>> allInhalerUsageEvents;


    public BreatheViewModel(@NonNull Application application) {
        super(application);
        breatheRepository = new BreatheRepository(application);
        allInhalerUsageEvents = breatheRepository.getAllInhalerUsageEvents();
    }

    public LiveData<List<InhalerUsageEvent>> getAllInhalerUsageEvents() {
        return allInhalerUsageEvents;
    }

    public void updateDiaryEntry(Instant timeStamp, DiaryEntry diaryEntry) {
        breatheRepository.updateDiaryEntry(timeStamp, diaryEntry);
    }

    @SuppressLint("NewApi")
    public void simulateIUE(Context context){
        breatheRepository.startDataCollection(Instant.now(), context);
        //TODO: is passing a context really the best idea?
    }

    public void clearIUEs(){
        breatheRepository.clearIUEs();
    }

}
