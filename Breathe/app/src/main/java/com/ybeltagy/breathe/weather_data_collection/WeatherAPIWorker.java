package com.ybeltagy.breathe.weather_data_collection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ybeltagy.breathe.R;

import java.time.Instant;

import static com.ybeltagy.breathe.weather_data_collection.TaskDataFinals.KEY_WEATHER_DATA_RESULT;

/**
 * Worker class that takes a double array of latitude, longitude and requests weather data from
 * tomorrow.io's v4 API
 */
public class WeatherAPIWorker extends Worker {
    /**
     * debug
      */
    private static final String WEATHER_API_WORKER_TAG = "WeatherAPIWorker";

    // handle to Context from constructor
    Context context;

    public WeatherAPIWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    /**
     * Get weather data from tomorrow.io
     *
     * @return Result.success() if valid weather data was returned
     * or Result.failure if valid weather data wasn't returned from API call
     */
    @NonNull
    @Override
    @SuppressLint("NewApi")
    public Result doWork() {

        // Input data - latitude and longitude
        double[] latLongArray = getInputData().getDoubleArray(TaskDataFinals.KEY_GPS_RESULT);

        if (latLongArray == null) Result.failure();
        Log.d(WEATHER_API_WORKER_TAG,
                "Received location in WeatherAPIWorker: "
                        + latLongArray[0] + " , " + latLongArray[1]);

        // Input data - timestamp string
        String timestampInputString = getInputData().getString(TaskDataFinals.KEY_TIMESTAMP);
        if (timestampInputString == null) Result.failure();
        Instant timestamp = Instant.parse(timestampInputString);

        CollectWeatherData.apiKey = context.getString(R.string.clima_cell_api_key);
        String returnJSONResponseForWeatherData =
                CollectWeatherData.syncGetWeatherDataJSONString(timestamp,
                        latLongArray[0], latLongArray[1]);

        if (returnJSONResponseForWeatherData != null) {
            // set output
            Log.d(WEATHER_API_WORKER_TAG,
                    "JSON weather data response -> " + returnJSONResponseForWeatherData);
            Data weatherDataOutput = new Data.Builder()
                    .putString(KEY_WEATHER_DATA_RESULT, returnJSONResponseForWeatherData)
                    .build();
            return Result.success(weatherDataOutput);

        } else { // we didn't get any weather data
            Log.d(WEATHER_API_WORKER_TAG,
                    "Weather data API response was null");
            return Result.failure();
        }
    }
}
