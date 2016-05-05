package com.upenn.chriswang1990.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    String forecastStr;
    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {

    };    
    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        Intent detailIntent = getActivity().getIntent();
        if (detailIntent != null) {
            forecastStr = detailIntent.getDataString();
        }
        if (forecastStr != null) {
            ((TextView) rootView.findViewById(R.id.textview_detail)).setText(forecastStr);
        }
        return rootView;
    }
}
