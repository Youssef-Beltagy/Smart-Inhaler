package com.ybeltagy.breathe.data;

import androidx.room.Ignore;

/**
 * Encapsulates area weather conditions (data from ClimaCell API) at the time of a given InhalerUsageEvent
 * - One InhalerUsageEvent object contains one WeatherData object
 * <p>
 * TODO: write static WeatherData createWeatherData() to gather conditions from ClimaCell API
 * and create a WeatherData object
 */
public class WeatherData {

    private float weatherTemperature = DataFinals.DEFAULT_FLOAT; // Celsius
    private float weatherHumidity = DataFinals.DEFAULT_FLOAT; // percentage out of 100 - value should always be =< 1
    private Level weatherPollen = DataFinals.DEFAULT_LEVEL; // LOW, MEDIUM, or HIGH
    private int weatherAQI = DataFinals.DEFAULT_INTEGER; // Air quality index, as defined by the EPA

    public WeatherData(){}

    @Ignore
    public WeatherData(float weatherTemperature, float weatherHumidity,
                       Level weatherPollen, int weatherAQI) {
        setWeatherTemperature(weatherTemperature);
        setWeatherHumidity(weatherHumidity);
        setWeatherPollen(weatherPollen);
        setWeatherAQI(weatherAQI);
    }

    public float getWeatherTemperature() {
        return weatherTemperature;
    }

    public void setWeatherTemperature(float weatherTemperature) {
        this.weatherTemperature = weatherTemperature;
    }

    public float getWeatherHumidity() {
        return weatherHumidity;
    }

    public void setWeatherHumidity(float weatherHumidity) {
        this.weatherHumidity = weatherHumidity;
    }

    public Level getWeatherPollen() {
        return weatherPollen;
    }

    public void setWeatherPollen(Level weatherPollen) {
        this.weatherPollen = weatherPollen;
    }

    public int getWeatherAQI() {
        return weatherAQI;
    }

    public void setWeatherAQI(int weatherAQI) {
        this.weatherAQI = weatherAQI;
    }
}
