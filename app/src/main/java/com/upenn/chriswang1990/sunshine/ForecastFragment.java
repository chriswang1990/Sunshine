package com.upenn.chriswang1990.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> forecastAdapter;
    SharedPreferences userPreferences;

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
        final ArrayList<String> forecastData = new ArrayList<>();
        /*
        //comment out the fake data
        forecastData.add("Today - Sunny - 20/10");
        forecastData.add("Tomorrow - Rainy - 15/9");
        forecastData.add("Wednesday - Sunny - 22/10");
        forecastData.add("Thursday - Cloudy - 15/8");
        forecastData.add("Friday - Sunny - 23/13");
        forecastData.add("Saturday - Sunny - 26/16");
        forecastData.add("Sunny - Sunny - 25/15");
        */
        forecastAdapter = new ArrayAdapter<>(getActivity(), R.layout
              .list_item_forecast, R.id.list_item_forecast_textview, forecastData);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastList.setAdapter(forecastAdapter);
        forecastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /*
        //toast for testing
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), (String)
            parent.getItemAtPosition(position), Toast.LENGTH_SHORT);
        toast.show();
        */
                String forecastStr = forecastAdapter.getItem(position);
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class).putExtra
                      (Intent.EXTRA_TEXT, forecastStr);
                userPreferences = PreferenceManager.getDefaultSharedPreferences
                      (getActivity());
                SharedPreferences.Editor editor = userPreferences.edit();
                editor.putString("forecastStr", forecastStr);
                editor.apply();
                startActivity(detailIntent);
            }
        });
        return rootView;
    }

    public void updateWeather() {
        FetchWeatherTask getData = new FetchWeatherTask(getActivity(), forecastAdapter);
        userPreferences = PreferenceManager.getDefaultSharedPreferences
              (getActivity());
        String location = userPreferences.getString(getString(R.string.pref_location_key),
              getString(R.string.pref_location_default));
        getData.execute(location);
    }
}


