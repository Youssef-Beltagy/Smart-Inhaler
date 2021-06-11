package com.ybeltagy.breathe.weather_data_collection;

import android.annotation.SuppressLint;
import android.util.Log;

import com.ybeltagy.breathe.data.DataFinals;
import com.ybeltagy.breathe.data.Level;
import com.ybeltagy.breathe.data.WeatherData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A helper class to hold the Tomorrow.io api call code for the weather data
 */
public class CollectWeatherData {

    protected static String apiKey;

    // API fields
    private static final String TEMPERATURE = "temperature";
    private static final String HUMIDITY = "humidity";
    private static final String EPAINDEX = "epaIndex";
    private static final String PRECIPITATIONINTENSITY = "precipitationIntensity";
    private static final String TREEINDEX = "treeIndex";
    private static final String GRASSINDEX = "grassIndex";

    private static final String QUERY_URL = "https://api.tomorrow.io/v4/timelines";

    /**
     * Get weather data for the requested fields for right now from tomorrow.io
     *
     * @return WeatherData object for the current time and input latitude/longitude location
     */
    @SuppressLint("NewApi")
    public static String syncGetWeatherDataJSONString(Instant startTime,
                                                      double latitude, double longitude) {

        Instant endTime = startTime.plusSeconds(60);

        String url =
                new StringBuilder(QUERY_URL)
                        .append("?")
                        .append("fields=")
                        .append(TEMPERATURE)
                        .append(",")
                        .append(HUMIDITY)
                        .append(",")
                        .append(EPAINDEX)
                        .append(",")
                        .append(PRECIPITATIONINTENSITY)
                        .append(",")
                        .append(TREEINDEX)
                        .append(",")
                        .append(GRASSINDEX)
                        .append("&startTime=")
                        .append(startTime.toString())
                        .append("&endTime=")
                        .append(endTime.toString())
                        .append("&timesteps=1m")
                        .append("&apikey=")
                        .append(apiKey)
                        .append("&location=")
                        .append(latitude)
                        .append(",")
                        .append(longitude)
                        .toString();

        Log.d("WeatherData", url);

        OkHttpClient client = new OkHttpClient(); // todo: continue: stopped here! ybeltagy continue from here

        Request request = new Request.Builder()
                .url(url)
                .build();

        Log.d("WeatherData", request.toString());

        try (Response response = client.newCall(request).execute()) {

            if(response.isSuccessful()) return response.body().string();

        } catch (IOException e) {
            Log.e("WeatherData", "IOException : " + e.getMessage());
        }

        return null;
    }

    public static WeatherData responseJSONToWeatherData(String response) {

        if (response == null || response.isEmpty()) return null;

        try {
            JSONObject obj = null;

            obj = new JSONObject(response).getJSONObject("data");

            obj = obj.getJSONArray("timelines")
                    .getJSONObject(0)
                    .getJSONArray("intervals")
                    .getJSONObject(0)
                    .getJSONObject("values");

            WeatherData weatherData = new WeatherData();

            // get NaN if temperature doesn't exist (instead of throwing exception, which happened
            // to me a couple times)
            // https://stackoverflow.com/questions/15477304/android-jsonexception-no-value-for
            weatherData.setWeatherTemperature(
                    (float) obj.optDouble(TEMPERATURE, DataFinals.DEFAULT_FLOAT));
            Log.d("WeatherData", "Temperature : " + weatherData.getWeatherTemperature());

            weatherData.setWeatherHumidity(
                    (float) obj.optDouble(HUMIDITY, DataFinals.DEFAULT_FLOAT));
            Log.d("WeatherData", "Humidity : " + weatherData.getWeatherHumidity());

            weatherData.setWeatherPrecipitationIntensity(
                    (float) obj.optDouble(PRECIPITATIONINTENSITY, DataFinals.DEFAULT_FLOAT));
            Log.d("WeatherData",
                    "Precipitation Intensity : "
                            + weatherData.getWeatherPrecipitationIntensity());

            weatherData.setWeatherTreeIndex(
                    Level.intToLevel(obj.optInt(TREEINDEX, Level.levelToInt(DataFinals.DEFAULT_LEVEL))));
            Log.d("WeatherData", "Tree Index : " + weatherData.getWeatherTreeIndex()
                    + " = " + Level.levelToInt(weatherData.getWeatherTreeIndex()));

            weatherData.setWeatherGrassIndex(
                    Level.intToLevel(obj.optInt(GRASSINDEX, Level.levelToInt(DataFinals.DEFAULT_LEVEL))));
            Log.d("WeatherData", "Grass Index : " + weatherData.getWeatherGrassIndex()
                    + " = " + Level.levelToInt(weatherData.getWeatherGrassIndex()));

            weatherData.setWeatherEPAIndex(obj.optInt(EPAINDEX, DataFinals.DEFAULT_INTEGER));
            Log.d("WeatherData", "EPA Index : " + weatherData.getWeatherEPAIndex());

            return weatherData;
        } catch (JSONException e) {
            Log.e("WeatherData", "JSONException : " + e.getMessage());
            return null;
        }
    }
}
