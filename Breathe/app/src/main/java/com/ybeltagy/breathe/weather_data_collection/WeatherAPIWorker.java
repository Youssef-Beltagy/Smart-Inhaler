package com.ybeltagy.breathe.weather_data_collection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ybeltagy.breathe.R;
import com.ybeltagy.breathe.collection.BreatheRoomDatabase;
import com.ybeltagy.breathe.data.WeatherData;

import java.time.Instant;

import static com.ybeltagy.breathe.weather_data_collection.TaskDataFinals.KEY_SAVE_WEATHER;
import static com.ybeltagy.breathe.weather_data_collection.TaskDataFinals.KEY_WEATHER_DATA_RESULT;

/**
 * Worker class that takes a double array of latitude, longitude and requests weather data from
 * tomorrow.io's v4 API
 */
public class WeatherAPIWorker extends Worker {
    /**
     * debug
     */
    private static final String tag = WeatherAPIWorker.class.getName();

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

        // Input data - timestamp string
        String timestampInputString = getInputData().getString(TaskDataFinals.KEY_TIMESTAMP);
        if (timestampInputString == null) {
            Log.d(tag, "timestamp input string as null");
            Result.failure(); // we can't retry at this point
        }
        Instant timestamp = Instant.parse(timestampInputString);

        // Input data - latitude and longitude
        double[] latLongArray = getInputData().getDoubleArray(TaskDataFinals.KEY_GPS_RESULT);

        if (latLongArray == null) {
            Log.d(tag, "latitude/longitude were null for " + timestamp.toString());
            Result.failure(); // we can't retry at this point
        }

        Log.d(tag,
                "Received location in WeatherAPIWorker: "
                        + latLongArray[0] + " , " + latLongArray[1]);

        // make the API request
        CollectWeatherData.apiKey = context.getString(R.string.clima_cell_api_key);
        String apiResponse =
                CollectWeatherData.syncGetWeatherDataJSONString(timestamp,
                        latLongArray[0], latLongArray[1]);

        if (apiResponse != null) {
            // should we save to the database? or is this request for the UI?
            boolean saveToDB = getInputData().getBoolean(KEY_SAVE_WEATHER, false);

            if (saveToDB) {
                WeatherData weatherData = CollectWeatherData.responseJSONToWeatherData(apiResponse);

                if (weatherData != null && weatherData.isDataValid()) {
                    saveToDB(timestamp, weatherData);
                    return Result.success(); // no need to return a JSON string
                }
                // weather data wasn't parsed/was invalid, log an error statement and retry
                Log.d(tag, "weather data wasn't parsed/was invalid.  Retrying for timestamp " +
                        timestamp.toString());
                return Result.retry();
            }
            // this is for the UI, send back a JSON string
            Log.d(tag, "JSON weather data response -> " + apiResponse);

            // set output
            Data weatherDataOutput = new Data.Builder()
                    .putString(KEY_WEATHER_DATA_RESULT, apiResponse)
                    .build();
            return Result.success(weatherDataOutput);

        }
        // we didn't get an API response; we should retry
        Log.d(tag,
                "Weather data API response was null for timestamp " + timestamp.toString());
        return Result.retry();
    }

    private void saveToDB(Instant timestamp, WeatherData weatherData) {
        BreatheRoomDatabase.getDatabase(context).breatheDao().
                updateWeatherData(timestamp,
                        weatherData.getWeatherTemperature(),
                        weatherData.getWeatherHumidity(),
                        weatherData.getWeatherPrecipitationIntensity(),
                        weatherData.getWeatherTreeIndex(),
                        weatherData.getWeatherGrassIndex(),
                        weatherData.getWeatherEPAIndex());
    }
}

