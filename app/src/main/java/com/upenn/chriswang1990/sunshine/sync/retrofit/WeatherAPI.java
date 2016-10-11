package com.upenn.chriswang1990.sunshine.sync.retrofit;

import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface WeatherAPI {
    String ENDPOINT = "http://api.openweathermap.org";
    @GET("data/2.5/forecast/daily")
    Observable<Response<WeatherResponse>> getResponse(@Query("q") String locationQuery,
                                                     @Query("lat") String locationLatitude,
                                                     @Query("lon") String locationLongitude,
                                                     @Query("mode") String format,
                                                     @Query("units") String units,
                                                     @Query("cnt") int numDays,
                                                     @Query("APPID") String appID);
}
