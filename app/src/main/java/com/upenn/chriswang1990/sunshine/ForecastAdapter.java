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

package com.upenn.chriswang1990.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.upenn.chriswang1990.sunshine.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean mTwoPane = false;
    CursorAdapter mCursorAdapter;
    Context mContext;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface UriCallback {
        /**
         * DetailFragment Callback for when an item has been selected.
         */
        void onItemSelected(Uri dateUri);
    }

    public interface PositionCallback {
        /**
         * DetailFragment Callback for when an item has been selected.
         */
        void onItemSelected(int pos);
    }

    public ForecastAdapter(Context context, Cursor c) {
        mContext = context;
        mCursorAdapter = new CursorAdapter(mContext, c, 0) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return null;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
            }

            @Override
            public Cursor swapCursor(Cursor newCursor) {
                Cursor oldCursor = super.swapCursor(newCursor);
                notifyDataSetChanged();
                return oldCursor;
            }
        };
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView iconView;
        private final TextView dateView;
        private final TextView descriptionView;
        private final TextView highTempView;
        private final TextView lowTempView;
        private final TextView cityNameView;
        private int normalizedDate;

        @Override
        public void onClick(View view) {
            String locationSetting = Utility.getPreferredLocation(mContext);
            ((UriCallback) mContext).onItemSelected(WeatherContract.WeatherEntry.
                    buildWeatherLocationWithDate(locationSetting, normalizedDate));
            ((PositionCallback) mContext).onItemSelected(getAdapterPosition());
        }

        public ViewHolder(View view) {
            super(view);
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            cityNameView = (TextView) view.findViewById(R.id.list_item_city_textview);
        }
    }

    public void setIsTwoPane(boolean isTwoPane) {
        mTwoPane = isTwoPane;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Cursor cursor = mCursorAdapter.getCursor();
        int pos = cursor.getPosition();
        viewType = getItemViewType(pos + 1);
        int layoutId;
        if (viewType == VIEW_TYPE_TODAY) {
            layoutId = R.layout.list_item_forecast_today;
        } else if (viewType == VIEW_TYPE_FUTURE_DAY) {
            layoutId = R.layout.list_item_forecast;
        } else {
            layoutId = R.layout.list_item_forecast;
        }
        View view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Cursor cursor = mCursorAdapter.getCursor();
        cursor.moveToPosition(position);
        viewHolder.normalizedDate = cursor.getInt(ForecastFragment.COL_WEATHER_DATE);
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int currentPos = cursor.getPosition();
        int viewType = getItemViewType(currentPos);
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
        String description = Utility.getStringForWeatherCondition(mContext, weatherId);
        viewHolder.descriptionView.setText(description);
        viewHolder.descriptionView.setContentDescription(mContext.getString(R.string.a11y_forecast, description));

        //set image description for accessibility
        viewHolder.iconView.setContentDescription(description);

        // Read high temperature from cursor
        String high = Utility.formatTemperature(
                mContext, cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        viewHolder.highTempView.setText(high);
        viewHolder.highTempView.setContentDescription(mContext.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(
                mContext, cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        viewHolder.lowTempView.setText(low);
        viewHolder.lowTempView.setContentDescription(mContext.getString(R.string.a11y_low_temp, low));
    }

    @Override
    public int getItemCount() {
        return mCursorAdapter.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = (position == 0 && !mTwoPane) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
        return viewType;
    }
}
