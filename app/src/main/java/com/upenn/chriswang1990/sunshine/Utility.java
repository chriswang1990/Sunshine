package com.upenn.chriswang1990.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.upenn.chriswang1990.sunshine.data.WeatherContract;
import com.upenn.chriswang1990.sunshine.sync.SunshineSyncAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utility {
    public static final String DATE_FORMAT = "yyyyMMdd";
    public static final String WEEK_FORMAT = "EEEE";
    public static final String MONTH_DAY_FORMAT = "MMM d";
    public static final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
              context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
              context.getString(R.string.pref_units_metric))
              .equals(context.getString(R.string.pref_units_metric));
    }

    public static long getLastDataSync(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String dataSyncKey = context.getString(R.string.pref_last_data_sync);
        return prefs.getLong(dataSyncKey, 0);
    }

    public static String getTimezoneID(Context context) {
        String timezoneID;
        String locationSetting = getPreferredLocation(context);
        Cursor location = context.getContentResolver().query(WeatherContract.WeatherEntry
                        .buildWeatherLocation(locationSetting),
                new String[]{WeatherContract.LocationEntry.COLUMN_TIMEZONE_ID}, null, null, null);
        if (location != null && location.moveToFirst()) {
            timezoneID = location.getString(0);
            location.close();
        } else {
            timezoneID = "";
        }
        return timezoneID;
    }

    static String getReadableDateString(long unixTimestamp, String timezoneID) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date todayDate;
        long timeInMilliseconds = unixTimestamp * 1000;
        //back up method when catch parse exception
        Date date = new Date(timeInMilliseconds);
        SimpleDateFormat readableFormat = new SimpleDateFormat("E, MMM d", Locale.US);
        readableFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
        //use a simple date format in the local timezone
        SimpleDateFormat compareFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        compareFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));

        try {
            String dateStr = Long.toString(normalizeDate(unixTimestamp, timezoneID));
            String todayStr = Long.toString(normalizeDate(System.currentTimeMillis() / 1000, timezoneID));

            //Log.d("date debug", "getReadableDateString: " + todayStr + " " + dateStr + " " + timezoneID);
            date = compareFormat.parse(dateStr);
            todayDate = compareFormat.parse(todayStr);
            long dayDiff = (date.getTime() - todayDate.getTime()) / MILLISECONDS_IN_DAY;
            //return "today" in readable string for today
            if (dayDiff == 0) {
                String today = "Today";
                return today + ", " + getMonthDayFormat(unixTimestamp, timezoneID);
            }

            if (dayDiff == 1) {
                return "Tomorrow";
            }

            if (dayDiff > 1 && dayDiff < 7) {
                return getWeekTimeFormat(unixTimestamp, timezoneID);
            }

        } catch (ParseException e) {
            return readableFormat.format(date);
        }
        return readableFormat.format(date);
    }

    public static String getWeekTimeFormat (long unixTimestamp, String timezoneID) {
        long timeInMilliseconds = unixTimestamp * 1000;
        Date date = new Date(timeInMilliseconds);
        SimpleDateFormat dayTimeFormat = new SimpleDateFormat(WEEK_FORMAT, Locale.US);
        dayTimeFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
        return dayTimeFormat.format(date);
    }

    public static String getMonthDayFormat (long unixTimestamp, String timezoneID) {
        long timeInMilliseconds = unixTimestamp * 1000;
        Date date = new Date(timeInMilliseconds);
        SimpleDateFormat dayTimeFormat = new SimpleDateFormat(MONTH_DAY_FORMAT, Locale.US);
        dayTimeFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
        return dayTimeFormat.format(date);
    }

    public static long normalizeDate (long unixTimestamp, String timezoneID) {
        long timeInMilliseconds = unixTimestamp * 1000;
        Date date = new Date(timeInMilliseconds);
        SimpleDateFormat normalizedFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        normalizedFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
        return Long.parseLong(normalizedFormat.format(date));
    }

    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    public static String formatTemperature(Context context, double temperature) {
        // Data stored in Celsius by default.  If user prefers to see in Fahrenheit, convert
        // the values here.
        String suffix = "\u00B0";
        if (!isMetric(context)) {
            temperature = (temperature * 1.8) + 32;
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        return String.format(context.getString(R.string.format_temperature), temperature);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_rain;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }

    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activityNetwork = cm.getActiveNetworkInfo();
        return activityNetwork != null && activityNetwork.isConnectedOrConnecting();
    }

    /**
     *
     * @param c Context used to get the SharedPreferences
     * @return the location status integer type
     */
    @SuppressWarnings("ResourceType")
    static public @SunshineSyncAdapter.LocationStatus
    int getLocationStatus(Context c){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_location_status_key), SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
    }
}
