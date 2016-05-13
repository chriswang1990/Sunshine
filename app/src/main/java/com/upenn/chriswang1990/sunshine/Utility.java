package com.upenn.chriswang1990.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utility {
    public static final String DATE_FORMAT = "yyyyMMdd";
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
                SimpleDateFormat todayFormat = new SimpleDateFormat("MMM d", Locale.US);
                todayFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
                return today + " " + todayFormat.format(date);
            }

            if (dayDiff == 1) {
                return "Tomorrow";
            }

            if (dayDiff > 1 && dayDiff < 7) {
                SimpleDateFormat weekFormat = new SimpleDateFormat("EEEE", Locale.US);
                weekFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
                return weekFormat.format(date);
            }

        } catch (ParseException e) {
            return readableFormat.format(date);
        }
        return readableFormat.format(date);
    }

    public static long normalizeDate (long unixTimestamp, String timezoneID) {
        long timeInMilliseconds = unixTimestamp * 1000;
        Date date = new Date(timeInMilliseconds);
        SimpleDateFormat normalizedFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        normalizedFormat.setTimeZone(TimeZone.getTimeZone(timezoneID));
        return Long.parseLong(normalizedFormat.format(date));
    }

    static String formatTemperature(double temperature, boolean isMetric) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return String.format(Locale.US, "%.0f", temp);
    }

}
