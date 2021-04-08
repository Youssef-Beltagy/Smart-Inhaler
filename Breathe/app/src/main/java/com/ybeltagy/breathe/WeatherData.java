package com.ybeltagy.breathe;

/**
 * 3 state level (used for pollen index)
 */
enum Level {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Encapsulates area weather conditions (data from ClimaCell API) at the time of a given InhalerUsageEvent
 * - One InhalerUsageEvent object contains one WeatherData object
 * <p>
 * TODO: write static WeatherData createWeatherData() to gather conditions from ClimaCell API
 * and create a WeatherData object
 */
public class WeatherData {

    private double weatherTemperature; // Celsius
    private double weatherHumidity; // percentage out of 100 - value should always be =< 1
    private Level weatherPollen; // LOW, MEDIUM, or HIGH
    private int weatherAQI; // Air quality index, as defined by the EPA

    public WeatherData(double weatherTemperature, double weatherHumidity,
                       Level weatherPollen, int weatherAQI) {
        this.weatherTemperature = weatherTemperature;
        this.weatherHumidity = weatherHumidity;
        this.weatherPollen = weatherPollen;
        this.weatherAQI = weatherAQI;
    }

    public double getWeatherTemperature() {
        return weatherTemperature;
    }

    public void setWeatherTemperature(double weatherTemperature) {
        this.weatherTemperature = weatherTemperature;
    }

    public double getWeatherHumidity() {
        return weatherHumidity;
    }

    public void setWeatherHumidity(double weatherHumidity) {
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
