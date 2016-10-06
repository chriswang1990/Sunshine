package com.upenn.chriswang1990.sunshine.sync.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TimezoneAPI {
    String ENDPOINT = "https://maps.googleapis.com";
    @GET("maps/api/timezone/json")
    Call<TimezoneResponse> getResponse(@Query("location") String latAndLon,
                                       @Query("timestamp") String currentTimestamp,
                                       @Query("key") String timezoneKey);
}
