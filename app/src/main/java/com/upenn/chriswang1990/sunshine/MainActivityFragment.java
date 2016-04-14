package com.upenn.chriswang1990.sunshine;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ArrayList<String> forecastData = new ArrayList<>();
        forecastData.add("Today - Sunny - 20/10");
        forecastData.add("Tomorrow - Rainy - 15/9");
        forecastData.add("Wednesday - Sunny - 22/10");
        forecastData.add("Thursday - Cloudy - 15/8");
        forecastData.add("Friday - Sunny - 23/13");
        forecastData.add("Saturday - Sunny - 26/16");
        forecastData.add("Sunny - Sunny - 25/15");
        ArrayAdapter<String> forecastAdapter = new ArrayAdapter<>(getActivity(), R.layout
                .list_item_forecast, R.id.list_item_forecast_textview, forecastData);
        ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastList.setAdapter(forecastAdapter);
        return rootView;
    }
}
