package com.example.weatherpro.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {
    @GET("data/2.5/forecast")
    Call<ResponseBody> getForecast(
            @Query("q") String city,
            @Query("appid") String apiKey,
            @Query("units") String units
    );
}
