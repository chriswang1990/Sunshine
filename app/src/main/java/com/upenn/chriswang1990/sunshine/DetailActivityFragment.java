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

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        TextView forecastTextView = (TextView) rootView.findViewById(R.id.textview_detail);
        Intent detailIntent = getActivity().getIntent();
        String forecastStr = detailIntent.getStringExtra(Intent.EXTRA_TEXT);
        forecastTextView.setText(forecastStr);
        return rootView;
    }
}
