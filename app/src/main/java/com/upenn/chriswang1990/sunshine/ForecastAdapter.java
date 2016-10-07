/*
 * Copyright (C) 2016 The Android Open Source Project
 * 1990chriswang1990@gmail.com
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
import android.database.DataSetObserver;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.upenn.chriswang1990.sunshine.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link RecyclerView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_SELECTED = 2;
    private boolean mTwoPane = false;

    private int selectedPosition = 0;

    private Context mContext;
    private Cursor mCursor;
    private boolean mDataValid;
    private int mRowIdColumn;
    private DataSetObserver mDataSetObserver;


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface UriCallback {
        /**
         * DetailFragment Callback for when an item has been selected, implemented in MainActivity
         */
        void onItemSelected(Uri dateUri);
    }

    public interface PositionCallback {
        /**
         * DetailFragment Callback for when an item has been selected.
         */
        void onItemSelected(int pos);
    }

    public ForecastAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
        mDataValid = cursor != null;
        mRowIdColumn = mDataValid ? mCursor.getColumnIndex("_id") : -1;
        mDataSetObserver = new ForecastAdapter.NotifyingDataSetObserver();
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mRowIdColumn);
        }
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     */
    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            mRowIdColumn = -1;
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView dateView;
        private final TextView descriptionView;
        private final TextView highTempView;
        private final TextView lowTempView;
        private final TextView cityNameView;

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
        int layoutId;
        if (viewType == VIEW_TYPE_TODAY) {
            layoutId = R.layout.list_item_forecast_today;
        } else if (viewType == VIEW_TYPE_SELECTED) {
            layoutId = R.layout.list_item_forecast;
        } else {
            layoutId = R.layout.list_item_forecast;
        }
        View view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);
        if (viewType == VIEW_TYPE_SELECTED) {
            view.setBackgroundResource(R.drawable.touch_selector_activated);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        mCursor.moveToPosition(viewHolder.getAdapterPosition());
        final int normalizedDate = mCursor.getInt(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedPosition(viewHolder.getAdapterPosition());
                String locationSetting = Utility.getPreferredLocation(mContext);
                ((UriCallback) mContext).onItemSelected(WeatherContract.WeatherEntry.
                        buildWeatherLocationWithDate(locationSetting, normalizedDate));
                ((PositionCallback) mContext).onItemSelected(viewHolder.getAdapterPosition());
            }
        });
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int currentPos = mCursor.getPosition();
        int viewType = getItemViewType(currentPos);
        String cityName = mCursor.getString(ForecastFragment.COL_CITY_NAME);
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
        long unixDate = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE_UNIX);
        String timezoneID = mCursor.getString(ForecastFragment.COL_TIMEZONE_ID);
        viewHolder.dateView.setText(Utility.getReadableDateString(unixDate, timezoneID));

        //get and set description string
        String description = Utility.getStringForWeatherCondition(mContext, weatherId);
        viewHolder.descriptionView.setText(description);
        viewHolder.descriptionView.setContentDescription(mContext.getString(R.string.a11y_forecast, description));

        //set image description for accessibility
        viewHolder.iconView.setContentDescription(description);

        // Read high temperature from cursor
        String high = Utility.formatTemperature(
                mContext, mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        viewHolder.highTempView.setText(high);
        viewHolder.highTempView.setContentDescription(mContext.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(
                mContext, mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        viewHolder.lowTempView.setText(low);
        viewHolder.lowTempView.setContentDescription(mContext.getString(R.string.a11y_low_temp, low));
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;
        if (position == 0 && !mTwoPane) {
            viewType = VIEW_TYPE_TODAY;
        } else if (selectedPosition == position && mTwoPane) {
            viewType = VIEW_TYPE_SELECTED;
        } else {
            viewType = VIEW_TYPE_FUTURE_DAY;
        }
        return viewType;
    }

    public void setSelectedPosition(int position) {
        notifyItemChanged(selectedPosition);
        selectedPosition = position;
        notifyItemChanged(selectedPosition);
    }
}
