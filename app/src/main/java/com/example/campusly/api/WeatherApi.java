package com.example.campusly.api;

import com.example.campusly.models.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Retrofit turns this interface into real HTTP code. Calling getCurrentWeather
// requests: https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=..&timezone=..
public interface WeatherApi {

    String BASE_URL = "https://api.open-meteo.com/";

    @GET("v1/forecast")
    Call<WeatherResponse> getCurrentWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String fields,
            @Query("timezone") String timezone);
}