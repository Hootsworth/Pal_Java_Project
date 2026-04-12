package model;

import java.io.Serializable;

/**
 * WeatherData — Represents current weather conditions from Open-Meteo API.
 * Contains temperature, wind speed, weather code, and derived descriptions.
 * Demonstrates: Encapsulation, Serialization, Strategy pattern (code → emoji mapping).
 */
public class WeatherData implements Serializable {
    private static final long serialVersionUID = 1L;

    private double temperature;    // Celsius
    private double windspeed;      // km/h
    private int weatherCode;       // WMO weather code
    private String description;    // "Partly Cloudy", etc.
    private String emoji;          // ☀️ 🌧️ ⛈️ etc.
    private String locationName;   // Display name for location

    public WeatherData() {}

    // ── Getters & Setters ──
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getWindspeed() { return windspeed; }
    public void setWindspeed(double windspeed) { this.windspeed = windspeed; }

    public int getWeatherCode() { return weatherCode; }
    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
        this.description = codeToDescription(weatherCode);
        this.emoji = codeToEmoji(weatherCode);
    }

    public String getDescription() { return description; }
    public String getEmoji() { return emoji; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    /** Returns formatted display like "☀️ 24°C — Clear sky" */
    public String getDisplayString() {
        return emoji + " " + Math.round(temperature) + "°C — " + description;
    }

    /** Returns short display like "☀️ 24°C" */
    public String getShortDisplay() {
        return emoji + " " + Math.round(temperature) + "°C";
    }

    // ── WMO Weather Code Mapping ──
    // See: https://www.nodc.noaa.gov/archive/arc0021/0002199/1.1/data/0-data/HTML/WMO-CODE/WMO4677.HTM

    private static String codeToDescription(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense drizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 66, 67 -> "Freezing rain";
            case 71 -> "Slight snow";
            case 73 -> "Moderate snow";
            case 75 -> "Heavy snow";
            case 77 -> "Snow grains";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 85 -> "Slight snow showers";
            case 86 -> "Heavy snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    private static String codeToEmoji(int code) {
        return switch (code) {
            case 0 -> "☀️";
            case 1 -> "🌤️";
            case 2 -> "⛅";
            case 3 -> "☁️";
            case 45, 48 -> "🌫️";
            case 51, 53, 55 -> "🌦️";
            case 61, 63, 80, 81 -> "🌧️";
            case 65, 82 -> "🌧️";
            case 66, 67 -> "🧊";
            case 71, 73, 85 -> "🌨️";
            case 75, 86 -> "❄️";
            case 77 -> "🌨️";
            case 95 -> "⛈️";
            case 96, 99 -> "⛈️";
            default -> "🌡️";
        };
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}
