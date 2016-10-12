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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.upenn.chriswang1990.sunshine.data.WeatherContract;
import com.upenn.chriswang1990.sunshine.sync.SunshineSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link RecyclerView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private ForecastAdapter mForecastAdapter;
    private RecyclerView mRecyclerView;
    private boolean showToast;
    View rootView, emptyView;

    private int mPosition = RecyclerView.NO_POSITION;
    private static final String SELECTED_KEY = "selected_position";
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int FORECAST_LOADER = 0;
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_DATE_UNIX_TIMESTAMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG,
            WeatherContract.LocationEntry.COLUMN_TIMEZONE_ID,
            WeatherContract.LocationEntry.COLUMN_CITY_NAME
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DATE_UNIX = 2;
    static final int COL_WEATHER_DESC = 3;
    static final int COL_WEATHER_MAX_TEMP = 4;
    static final int COL_WEATHER_MIN_TEMP = 5;
    static final int COL_LOCATION_SETTING = 6;
    static final int COL_WEATHER_CONDITION_ID = 7;
    static final int COL_COORD_LAT = 8;
    static final int COL_COORD_LONG = 9;
    static final int COL_TIMEZONE_ID = 10;
    static final int COL_CITY_NAME = 11;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            if (!Utility.isLocationSet(getActivity())) {
                Toast.makeText(getActivity(), R.string.location_not_set_warning, Toast.LENGTH_LONG).show();
            } else {
                updateWeather();
                showToast = true;
                getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ForecastAdapter will take data from a source and
        // use it to populate the RecyclerView it's attached to.
        mForecastAdapter = new ForecastAdapter(getActivity(), null);
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        // Get a reference to the RecyclerView, and attach this adapter to it.
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);
        emptyView = rootView.findViewById(R.id.recyclerview_forecast_empty);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mForecastAdapter);
        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The RecyclerView probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        //update weather if it's more than one day since last weather sync
        //initializeData();
        showToast = false;
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged() {
        mPosition = 0;
        showToast = true;
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to RecyclerView.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != RecyclerView.NO_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.
        String locationSetting = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        String timezoneID = Utility.getTimezoneID(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithStartDate(locationSetting, Utility.normalizeDate(System
                        .currentTimeMillis() / 1000, timezoneID));
        Log.d(LOG_TAG, "weatherURI: " + weatherForLocationUri.toString());
        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if (data == null || !data.moveToFirst()) {
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            updateEmptyView();
        } else {
            emptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            if (showToast) {
                if (Utility.getLocationStatus(getActivity()) != SunshineSyncAdapter.LOCATION_STATUS_OK) {
                    Toast.makeText(getActivity(), R.string.location_not_updated_warning, Toast.LENGTH_LONG).show();
                } else if (!Utility.getTimezoneStatus(getActivity())) {
                    Toast.makeText(getActivity(), R.string.timezone_warning, Toast.LENGTH_LONG).show();
                }
            }
            if (mPosition != RecyclerView.NO_POSITION) {
                // If we don't need to restart the loader, and there's a desired position to restore
                // to, do so now.
                mRecyclerView.smoothScrollToPosition(mPosition);
                mForecastAdapter.setSelectedPosition(mPosition);
            }
        }
        showToast = false;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setIsTwoPane(boolean isTwoPane) {
        if (mForecastAdapter != null) {
            mForecastAdapter.setIsTwoPane(isTwoPane);
        }
    }

    public void setPosition(int pos) {
        mPosition = pos;
    }

    private void updateEmptyView() {
        TextView tv = (TextView) getView().findViewById(R.id.recyclerview_forecast_empty);
        if (null != tv) {
            int message;
            @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getActivity());
            switch (location) {
                case SunshineSyncAdapter.LOCATION_STATUS_NO_NETWORK:
                    message = R.string.empty_forecast_list_no_network;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                    message = R.string.empty_forecast_list_server_unknown;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                    message = R.string.empty_forecast_list_server_down;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                    message = R.string.empty_forecast_list_invalid;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_NOT_SET:
                    message = R.string.empty_forecast_welcome;
                    break;
                default:
                    message = R.string.empty_forecast_list;
            }

            tv.setText(message);
        }
    }

    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void initializeData() {
        long lastDataSync = Utility.getLastDataSync(getActivity());
        if (System.currentTimeMillis() - lastDataSync >= DAY_IN_MILLIS) {
            updateWeather();
        }
    }

    private void openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (null != mForecastAdapter) {
            Cursor c = mForecastAdapter.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Context context = getContext();
                    String warning = "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!";
                    Utility.showAsToast(context, warning);
                }
            }
        }
    }
}

