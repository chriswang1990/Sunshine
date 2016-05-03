package com.upenn.chriswang1990.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utility {
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

    static String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat readableFormat = new SimpleDateFormat("E, MMM d", Locale.US);
        return readableFormat.format(date);
    }

    public static long normalizeDate (long time) {
        Date date = new Date(time);
        SimpleDateFormat normalizedFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return Long.parseLong(normalizedFormat.format(date));
    }

    static String formatTemperature(double temperature, boolean isMetric) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return String.format("%.0f", temp);
    }

}
