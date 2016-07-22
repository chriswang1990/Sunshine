package com.upenn.chriswang1990.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_COUNT = 2;
    private boolean mTwoPane = false;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;
        public final TextView cityNameView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            cityNameView = (TextView) view.findViewById(R.id.list_item_city_textview);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        if (viewType == VIEW_TYPE_TODAY) {
            layoutId = R.layout.list_item_forecast_today;
        } else if (viewType == VIEW_TYPE_FUTURE_DAY) {
            layoutId = R.layout.list_item_forecast;
        }
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int viewType = getItemViewType(cursor.getPosition());
        String cityName = cursor.getString(ForecastFragment.COL_CITY_NAME);

        switch (viewType) {
            case VIEW_TYPE_TODAY:
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                //get and set city name in today list item view
                viewHolder.cityNameView.setText(cityName);
                break;
            default:
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
                break;
        }

        //get date with get readable date method
        long unixDate = cursor.getLong(ForecastFragment.COL_WEATHER_DATE_UNIX);
        String timezoneID = cursor.getString(ForecastFragment.COL_TIMEZONE_ID);
        viewHolder.dateView.setText(Utility.getReadableDateString(unixDate, timezoneID));

        //get and set description string
        String description = Utility.getStringForWeatherCondition(context, weatherId);
        viewHolder.descriptionView.setText(description);
        viewHolder.descriptionView.setContentDescription(context.getString(R.string.a11y_forecast, description));

        //set image description for accessibility
        viewHolder.iconView.setContentDescription(description);

        // Read high temperature from cursor
        String high = Utility.formatTemperature(
                context, cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        viewHolder.highTempView.setText(high);
        viewHolder.highTempView.setContentDescription(context.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(
                context, cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        viewHolder.lowTempView.setText(low);
        viewHolder.lowTempView.setContentDescription(context.getString(R.string.a11y_low_temp, low));
    }

    public void setIsTwoPane(boolean isTwoPane) {
        mTwoPane = isTwoPane;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && !mTwoPane) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}
