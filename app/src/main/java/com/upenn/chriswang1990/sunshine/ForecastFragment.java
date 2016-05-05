package com.upenn.chriswang1990.sunshine;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.upenn.chriswang1990.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

//    ArrayAdapter<String> forecastAdapter;
//    SharedPreferences userPreferences;
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
          WeatherContract.LocationEntry.COLUMN_COORD_LONG
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

    private ForecastAdapter mForecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        final ArrayList<String> forecastData = new ArrayList<>();
//        forecastAdapter = new ArrayAdapter<>(getActivity(), R.layout
//              .list_item_forecast, R.id.list_item_forecast_textview, forecastData);

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastList.setAdapter(mForecastAdapter);
//        forecastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        /*
//        //toast for testing
//        Toast toast = Toast.makeText(getActivity().getApplicationContext(), (String)
//            parent.getItemAtPosition(position), Toast.LENGTH_SHORT);
//        toast.show();
//        */
//                String forecastStr = forecastAdapter.getItem(position);
//                Intent detailIntent = new Intent(getActivity(), DetailActivity.class).putExtra
//                      (Intent.EXTRA_TEXT, forecastStr);
//                userPreferences = PreferenceManager.getDefaultSharedPreferences
//                      (getActivity());
//                SharedPreferences.Editor editor = userPreferences.edit();
//                editor.putString("forecastStr", forecastStr);
//                editor.apply();
//                startActivity(detailIntent);
//            }
//        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, Utility.normalizeDate(System.currentTimeMillis()));
        return new CursorLoader(getActivity(), weatherForLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void updateWeather() {
//        FetchWeatherTask getData = new FetchWeatherTask(getActivity());
//        userPreferences = PreferenceManager.getDefaultSharedPreferences
//              (getActivity());
//        String location = userPreferences.getString(getString(R.string.pref_location_key),
//              getString(R.string.pref_location_default));
        FetchWeatherTask fetchWeather = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        fetchWeather.execute(location);
    }
}


