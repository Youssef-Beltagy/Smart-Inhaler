package com.ybeltagy.breathe.weather_data_collection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

/**
 * Worker class that gets the last location known to the OS. This location may be null if the
 * Location permission has been turned off and on and no other client has requested location.
 * If this occurs, the GPSWorker will be garbage collected and WeatherAPIWorker will not run.
 */
public class GPSWorker extends ListenableWorker {

    // debug
    String GPS_WORKER_LOG_TAG = "GPSWorker";

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public GPSWorker(@NonNull @NotNull Context appContext, @NonNull @NotNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    /**
     * Get GPS last known location and pass the GPS result as Data (this output will be used by
     * WeatherAPI worker if it is not null)
     * @return Result.success with data or Result.failure with Data.empty
     */
    @SuppressLint("MissingPermission")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Log.d(GPS_WORKER_LOG_TAG, "Starting work " + getId());
        return CallbackToFutureAdapter.getFuture( completer ->
                // Get the fused location provider from Google Play Services
                LocationServices.getFusedLocationProviderClient(getApplicationContext())
                .getLastLocation()
                .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(GPS_WORKER_LOG_TAG, "Got location in GPSWorker : "
                            + location.getLatitude() + " , " + location.getLongitude());
                            completer.set(Result.success(createGPSOutput(location)));
                        } else {
                            Log.d(GPS_WORKER_LOG_TAG, "Location was null");
                        }
                })
                .addOnFailureListener(
                        exception -> {
                            Log.e(GPS_WORKER_LOG_TAG, "Exception occurred : "
                                    + exception.getMessage());
                            completer.set(Result.retry());
                        }
                ));
    }

    /**
     * Creates data output from location
     * @param location
     * @return
     */
    private static Data createGPSOutput(Location location) {
        double[] latLong = {location.getLatitude(), location.getLongitude()};
        return new Data.Builder()
                .putDoubleArray(TaskDataFinals.KEY_GPS_RESULT, latLong)
                .build();
    }
}

