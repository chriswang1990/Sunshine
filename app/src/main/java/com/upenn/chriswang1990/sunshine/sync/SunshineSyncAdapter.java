/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.upenn.chriswang1990.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.upenn.chriswang1990.sunshine.BuildConfig;
import com.upenn.chriswang1990.sunshine.MainActivity;
import com.upenn.chriswang1990.sunshine.R;
import com.upenn.chriswang1990.sunshine.Utility;
import com.upenn.chriswang1990.sunshine.data.WeatherContract;
import com.upenn.chriswang1990.sunshine.sync.retrofit.TimezoneAPI;
import com.upenn.chriswang1990.sunshine.sync.retrofit.TimezoneResponse;
import com.upenn.chriswang1990.sunshine.sync.retrofit.WeatherAPI;
import com.upenn.chriswang1990.sunshine.sync.retrofit.WeatherResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Vector;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    private static final int SYNC_INTERVAL = 60 * 180;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    private Context context = getContext();
    private String mTimezoneID = "";

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.LocationEntry.COLUMN_CITY_NAME
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;
    private static final int INDEX_CITY_NAME = 4;

    //Location status annotation
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_UNKNOWN, LOCATION_STATUS_INVALID, LOCATION_STATUS_NOT_SET})
    public @interface LocationStatus {
    }

    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;
    public static final int LOCATION_STATUS_NOT_SET = 4;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        //Do nothing if location is not set or no network condition
        if (Utility.getPreferredLocation(context).equals("*") || !Utility.isNetworkAvailable(context)) {
            return;
        }
        //refreshing last sync time
        Utility.setLastDataSync(context);
        //Initialize the timezone status when refreshing
        setTimezoneStatus(context, true);
        Observable<Response<WeatherResponse>> weatherObservable = fetchWeatherData();
        weatherObservable
                .subscribe(new Subscriber<Response<WeatherResponse>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Utility.setLocationStatus(context, LOCATION_STATUS_SERVER_DOWN);
                    }

                    @Override
                    public void onNext(final Response<WeatherResponse> response) {
                        if (response.isSuccessful()) {
                            WeatherResponse.CityBean.CoordBean coord =
                                    response.body().getCity().getCoord();
                            String latAndLon = coord.getLat() + "," + coord.getLon();
                            Observable<TimezoneResponse> timezoneObservable = fetchTimezoneID(latAndLon);
                            timezoneObservable
                                    .subscribe(new Subscriber<TimezoneResponse>() {
                                        @Override
                                        public void onCompleted() {

                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            saveToDatabase(response, "");
                                            //When API failed to return correct info, update the timezone status to notify user
                                            setTimezoneStatus(context, false);
                                        }

                                        @Override
                                        public void onNext(TimezoneResponse timezoneResponse) {
                                            mTimezoneID = timezoneResponse.getTimeZoneId();
                                            setTimezoneStatus(context, true);
                                            //Save the weather and timezone data to the database
                                            saveToDatabase(response, mTimezoneID);
                                        }
                                    });
                        } else {
                            Utility.setLocationStatus(context, LOCATION_STATUS_INVALID);
                        }
                    }
                });
    }

    private Observable<Response<WeatherResponse>> fetchWeatherData() {
        // We no longer need just the location String, but also potentially the latitude and
        // longitude, in case we are syncing based on a new Place Picker API result.
        final String locationQuery = Utility.getPreferredLocation(context);
        String format = "json";
        String units = "metric";
        int numDays = 14;
        //retrofit call building
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WeatherAPI.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        WeatherAPI weatherAPI = retrofit.create(WeatherAPI.class);
        Observable<Response<WeatherResponse>> weatherObservable;
        if (Utility.isLocationLatLonAvailable(context)) {
            String locationLatitude = String.valueOf(Utility.getLocationLatitude(context));
            String locationLongitude = String.valueOf(Utility.getLocationLongitude(context));
            weatherObservable = weatherAPI.getResponse(null, locationLatitude, locationLongitude, format, units, numDays, BuildConfig.OPEN_WEATHER_MAP_API_KEY);
        } else {
            weatherObservable = weatherAPI.getResponse(locationQuery, null, null, format, units, numDays, BuildConfig.OPEN_WEATHER_MAP_API_KEY);
        }
        return weatherObservable;
    }

    /**
     * Get the mTimezoneID from google API by city lat and lon from weatherResponse, then process
     * the data and store in SQL Database
     */
    private Observable<TimezoneResponse> fetchTimezoneID(String latAndLon) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TimezoneAPI.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        TimezoneAPI timezoneAPI = retrofit.create(TimezoneAPI.class);
        Observable<TimezoneResponse> timezoneObservable = timezoneAPI.getResponse(latAndLon,
                Long.toString(System.currentTimeMillis() / 1000),
                BuildConfig.GOOGLE_ANDROID_API_KEY);
        return timezoneObservable;
    }

    /**
     * Take the retrofit weather response and the returned timezone ID and get the corresponding
     * weather data, save them in local SQL database
     */
    private void saveToDatabase(Response<WeatherResponse> response,
                                String timezoneID) {
        // Location information
        WeatherResponse weatherData = response.body();
        String locationSetting = Utility.getPreferredLocation(context);
        String cityName = weatherData.getCity().getName();
        double cityLatitude = weatherData.getCity().getCoord().getLat();
        double cityLongitude = weatherData.getCity().getCoord().getLon();
        long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude,
                timezoneID);
        int listLength = weatherData.getList().toArray().length;
        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<>(listLength);
        for (int i = 0; i < listLength; i++) {
            // These are the values that will be collected.
            WeatherResponse.ListBean dateWeather = weatherData.getList().get(i);
            long unixTimestamp = dateWeather.getDt();
            // the unix time(in milliseconds);
            long dateTime = Utility.normalizeDate(unixTimestamp, timezoneID);
            double pressure = dateWeather.getPressure();
            int humidity = dateWeather.getHumidity();
            double windSpeed = dateWeather.getSpeed();
            int windDirection = dateWeather.getDeg();
            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            double high = dateWeather.getTemp().getMax();
            double low = dateWeather.getTemp().getMin();
            // Description is in a child array called "weather", which is 1 element long.
            // That element also contains a weather code.
            String description = dateWeather.getWeather().get(0).getMain();
            int weatherId = dateWeather.getWeather().get(0).getId();

            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE_UNIX_TIMESTAMP, unixTimestamp);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
            cVVector.add(weatherValues);
        }
        // add to database
        int rowsDeleted = 0;
        if (cVVector.size() > 0) {
            ContentValues[] values = new ContentValues[cVVector.size()];
            cVVector.toArray(values);
            context.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI,
                    values);
            rowsDeleted = context.getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                    WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?",
                    new String[]{Long.toString(Utility.normalizeDate(System.currentTimeMillis()
                            / 1000, timezoneID) - 1)});
            notifyWeather();
        }
        Utility.setLocationStatus(context, LOCATION_STATUS_OK);
    }

    private void notifyWeather() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications) {
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the weather.
                String locationQuery = Utility.getPreferredLocation(context);
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
                Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery
                        , Utility.normalizeDate(System.currentTimeMillis() / 1000, mTimezoneID));

                // we'll query our contentProvider, as always
                Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    String cityName = cursor.getString(INDEX_CITY_NAME);
                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);

                    int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            Utility.getArtResourceForWeatherCondition(weatherId));
                    String title = context.getString(R.string.app_name);

                    // Define the text of the forecast.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            cityName,
                            desc,
                            Utility.formatTemperature(context, high),
                            Utility.formatTemperature(context, low));

                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setColor(context.getResources().getColor(R.color.primary_light))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.apply();
                }
                cursor.close();
            }
        }
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @param timezoneID      timezone info of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon, String
            timezoneID) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        long locationId;
        Cursor locationCursor = getContext().getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);
        if (locationCursor != null && locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
            locationCursor.close();
        } else {
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_TIMEZONE_ID, timezoneID);
            Uri locationUri = getContext().getContentResolver().insert(WeatherContract.LocationEntry
                    .CONTENT_URI, locationValues);
            locationId = ContentUris.parseId(locationUri);
        }
        return locationId;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));
        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /**
     * Sets the timezone status into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c              Context to get the PreferenceManager from.
     * @param timezoneStatus The IntDef value to set
     */
    public static void setTimezoneStatus(Context c, boolean timezoneStatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean(c.getString(R.string.pref_timezone_status_key), timezoneStatus);
        spe.apply();
    }
}