package com.example.campusly.utils;

import com.example.campusly.R;

// Turns Open-Meteo's WMO weather codes into an emoji and a short description.
// Full list: https://open-meteo.com/en/docs (section "WMO Weather interpretation codes").
public final class WeatherCodes {

    private WeatherCodes() {
    }

    public static String emojiFor(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤️";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 67) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌦️";
        if (code == 85 || code == 86) return "❄️";
        if (code >= 95) return "⛈️";
        return "🌡️";
    }

    public static int descriptionFor(int code) {
        if (code == 0) return R.string.weather_clear;
        if (code <= 2) return R.string.weather_partly_cloudy;
        if (code == 3) return R.string.weather_cloudy;
        if (code == 45 || code == 48) return R.string.weather_fog;
        if (code >= 51 && code <= 57) return R.string.weather_drizzle;
        if (code >= 61 && code <= 67) return R.string.weather_rain;
        if (code >= 71 && code <= 77) return R.string.weather_snow;
        if (code >= 80 && code <= 82) return R.string.weather_showers;
        if (code == 85 || code == 86) return R.string.weather_snow;
        if (code >= 95) return R.string.weather_thunderstorm;
        return R.string.weather_unknown;
    }
}