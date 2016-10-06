package com.upenn.chriswang1990.sunshine.sync.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherAPI {
    String ENDPOINT = "http://api.openweathermap.org";
    @GET("data/2.5/forecast/daily")
    Call<WeatherResponse> getResponse(@Query("q") String locationQuery,
                                      @Query("lat") String locationLatitude,
                                      @Query("lon") String locationLongitude,
                                      @Query("mode") String format,
                                      @Query("units") String units,
                                      @Query("cnt") int numDays,
                                      @Query("APPID") String appID);
}
