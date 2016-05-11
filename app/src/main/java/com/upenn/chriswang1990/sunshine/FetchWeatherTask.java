package com.upenn.chriswang1990.sunshine;/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.upenn.chriswang1990.sunshine.data.WeatherContract;
import com.upenn.chriswang1990.sunshine.data.WeatherContract.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private ForecastAdapter mForecastAdapter;
    private final Context mContext;

    public FetchWeatherTask(Context context) {
        mContext = context;
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */

//
//    /**
//     * Prepare the weather high/lows for presentation.
//     */
//    private String formatHighLows(double high, double low) {
//        // Data is fetched in Celsius by default.
//        // If user prefers to see in Fahrenheit, convert the values here.
//        // We do this rather than fetching in Fahrenheit so that the user can
//        // change this option without us having to re-fetch the data once
//        // we start storing the values in a database.
//        SharedPreferences sharedPrefs =
//              PreferenceManager.getDefaultSharedPreferences(mContext);
//        String unitType = sharedPrefs.getString(
//              mContext.getString(R.string.pref_units_key),
//              mContext.getString(R.string.pref_units_metric));
//
//        if (unitType.equals(mContext.getString(R.string.pref_units_imperial))) {
//            high = (high * 1.8) + 32;
//            low = (low * 1.8) + 32;
//        } else if (!unitType.equals(mContext.getString(R.string.pref_units_metric))) {
//            Log.d(LOG_TAG, "Unit type not found: " + unitType);
//        }
//
//        // For presentation, assume the user doesn't care about tenths of a degree.
//        long roundedHigh = Math.round(high);
//        long roundedLow = Math.round(low);
//
//        String highLowStr = roundedHigh + "/" + roundedLow;
//        return highLowStr;
//    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        long locationId;
        Cursor locationCursor = mContext.getContentResolver().query(
              WeatherContract.LocationEntry.CONTENT_URI,
              new String[]{WeatherContract.LocationEntry._ID},
              WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
              new String[]{locationSetting},
              null);
        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry
                  ._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry
                  .CONTENT_URI, locationValues);
            locationId = ContentUris.parseId(locationUri);
        }
        locationCursor.close();
        return locationId;
    }

    /*
        Students: This code will allow the FetchWeatherTask to continue to return the strings that
        the UX expects so that we can continue to test the application even once we begin using
        the database.
     */
//    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
//        // return strings to keep UI functional for now
//        //Log.d("Check Date", cvv.elementAt(1).getAsString(WeatherEntry.COLUMN_DATE));
//        String[] resultStrs = new String[cvv.size()];
//        for ( int i = 0; i < cvv.size(); i++ ) {
//            ContentValues weatherValues = cvv.elementAt(i);
//            String highAndLow = formatHighLows(
//                  weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP),
//                  weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
//            resultStrs[i] = getReadableDateString(
//                  weatherValues.getAsLong(WeatherEntry.COLUMN_DATE_UNIX_TIMESTAMP)) +
//                  "   -   " + weatherValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC) +
//                  "   -   " + highAndLow;
//        }
//        return resultStrs;
//    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getWeatherDataFromJson(String forecastJsonStr,
                                            String locationSetting)
          throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Date Infomation
        final String OWM_DATE = "dt";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            //Get the timezone from google API
            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<>(weatherArray.length());
            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.
            for(int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                long unixTimestamp;
                long timeOffset;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
                unixTimestamp = dayForecast.getLong(OWM_DATE) * 1000; //convert to milliseconds
                timeOffset = getTimeOffset(cityLatitude, cityLongitude, unixTimestamp);
                unixTimestamp += timeOffset * 1000; //applied the time offset from google api to
                // the unix time(in milliseconds);
                dateTime = Utility.normalizeDate(unixTimestamp);
                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                      dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherEntry.COLUMN_DATE_UNIX_TIMESTAMP, unixTimestamp);
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] values = new ContentValues[cVVector.size()];
                cVVector.toArray(values);
                inserted = mContext.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI,
                      values);
            }

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + inserted + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(String... params) {

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }
        String locationQuery = params[0];

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            Uri.Builder weatherAPIBuilder = new Uri.Builder();
            weatherAPIBuilder.scheme("http").authority("api.openweathermap.org").appendPath("data").appendPath
                  ("2.5").appendPath("forecast").appendPath("daily");
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            weatherAPIBuilder.appendQueryParameter(QUERY_PARAM, params[0])
                  .appendQueryParameter(FORMAT_PARAM, format)
                  .appendQueryParameter(UNITS_PARAM, units)
                  .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                  .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                  .build();

            URL weatherAPIUrl = new URL(weatherAPIBuilder.toString());
            Log.d(LOG_TAG, weatherAPIUrl.toString());
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) weatherAPIUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                line += "\n";
                buffer.append(line);
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
            //Call api for the timezone info
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }

    private long getTimeOffset(double cityLatitude, double cityLongitude, long timestamp) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        long timeOffset = 0;
        try {
            String latAndLon = cityLatitude + "," + cityLongitude;
            Uri.Builder timezoneAPIBuilder = new Uri.Builder();
            timezoneAPIBuilder.scheme("https").authority("maps.googleapis.com").appendPath
                  ("maps").appendPath
                  ("api").appendPath("timezone").appendPath("json");
            timezoneAPIBuilder.appendQueryParameter("location", latAndLon)
                  .appendQueryParameter("timestamp", Long.toString(timestamp/1000))
                  .appendQueryParameter("key", BuildConfig.GOOGLE_ANDROID_API_KEY)
                  .build();
            URL timezoneAPIUrl = new URL(timezoneAPIBuilder.toString());
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) timezoneAPIUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    line += "\n";
                    buffer.append(line);
                }
                String timezoneInfoStr = buffer.toString();
                JSONObject timezoneInfo = new JSONObject(timezoneInfoStr);
                long rawOffset = timezoneInfo.getLong("rawOffset");
                long dstOffset = timezoneInfo.getLong("dstOffset");
                timeOffset = rawOffset + dstOffset;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return timeOffset;
    }
}