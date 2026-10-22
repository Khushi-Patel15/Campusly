package com.example.campusly.repositories;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.example.campusly.api.WeatherApi;
import com.example.campusly.models.WeatherResponse;
import com.example.campusly.utils.AppConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Fetches the campus weather. The weather is a "nice to have": every failure
// is reported through onError so the screen can show a small message, and the
// rest of the app keeps working normally.
public class WeatherRepository {

    public interface Callback {
        void onSuccess(WeatherResponse.Current weather);

        void onError();
    }

    // Weather doesn't change every second, so reuse a result for 10 minutes
    // instead of calling the API every time Home is opened.
    private static final long CACHE_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private static WeatherApi api;
    private static WeatherResponse.Current lastWeather;
    private static long lastFetchTime;

    private static WeatherApi api() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(WeatherApi.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherApi.class);
        }
        return api;
    }

    public void fetchCampusWeather(boolean forceRefresh, Callback callback) {
        boolean cacheFresh = lastWeather != null
                && SystemClock.elapsedRealtime() - lastFetchTime < CACHE_MILLIS;
        if (cacheFresh && !forceRefresh) {
            callback.onSuccess(lastWeather);
            return;
        }

        // enqueue() runs the request in the background and calls us back on
        // the main thread, so it's safe to update the screen in the callback.
        api().getCurrentWeather(
                AppConfig.CAMPUS_LATITUDE,
                AppConfig.CAMPUS_LONGITUDE,
                "temperature_2m,weather_code",
                "auto"
        ).enqueue(new retrofit2.Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call,
                                   @NonNull Response<WeatherResponse> response) {
                WeatherResponse body = response.body();
                boolean valid = response.isSuccessful()
                        && body != null
                        && body.current != null
                        && body.current.temperature != null
                        && body.current.weatherCode != null;
                if (!valid) {
                    callback.onError();
                    return;
                }
                lastWeather = body.current;
                lastFetchTime = SystemClock.elapsedRealtime();
                callback.onSuccess(body.current);
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                // No internet, timeout, or the service is down.
                callback.onError();
            }
        });
    }
}