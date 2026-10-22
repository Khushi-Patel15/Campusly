package com.example.campusly.models;

import com.google.gson.annotations.SerializedName;

// The part of Open-Meteo's JSON reply that we use. Gson fills this in
// automatically and ignores every field not listed here. Example reply:
// { "current": { "time": "2026-10-03T12:00", "temperature_2m": 30.1, "weather_code": 2 } }
public class WeatherResponse {

    @SerializedName("current")
    public Current current;

    public static class Current {

        @SerializedName("temperature_2m")
        public Double temperature;

        // A WMO weather code, e.g. 0 = clear sky, 61 = rain. See WeatherCodes.
        @SerializedName("weather_code")
        public Integer weatherCode;
    }
}