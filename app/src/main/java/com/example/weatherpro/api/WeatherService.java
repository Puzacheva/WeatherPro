package com.example.weatherpro.api;

import com.example.weatherpro.model.City;

import java.util.List;

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

    @GET("geo/1.0/direct")
    Call<List<City>> getCities(
            @Query("q") String cityName,
            @Query("limit") int limit,
            @Query("appid") String apiKey
    );
}
