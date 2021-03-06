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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.upenn.chriswang1990.sunshine.sync.SunshineSyncAdapter;

public class MainActivity extends AppCompatActivity implements ForecastAdapter.UriCallback, ForecastAdapter.PositionCallback {

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private String mLocation;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getLastLocation(this);
        if (!Utility.isLocationSet(this)) {
            Utility.setLocationStatus(this, SunshineSyncAdapter.LOCATION_STATUS_NOT_SET);
        } else if (Utility.getLocationStatus(this) != SunshineSyncAdapter.LOCATION_STATUS_OK) {
            SunshineSyncAdapter.syncImmediately(this);
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        if (findViewById(R.id.weather_detail_container) != null) {
            // Before we open the detail page on hand-held device, the detail container view will
            // be present only in the large-screen layouts (res/layout-sw600dp). If this view is
            // present, then the activity should be in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                DetailFragment detailFragment = new DetailFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, detailFragment, DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast));
        forecastFragment.setIsTwoPane(mTwoPane);
        SunshineSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        // update the location in our second pane using the fragment manager
        ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById
                (R.id.fragment_forecast);
        if (location != null && !location.equals(mLocation)) {
            if (ff != null) {
                ff.onLocationChanged();
            }
            if (mTwoPane) {
                DetailFragment detailFragment = new DetailFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, detailFragment, DETAILFRAGMENT_TAG)
                        .commit();
            }
            mLocation = location;
            Utility.setLastLocation(this, location);
        }
    }

    @Override
    public void onItemSelected(Uri dateUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, dateUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(dateUri);
            startActivity(intent);
        }
    }

    @Override
    public void onItemSelected(int pos) {
        ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById
                (R.id.fragment_forecast);
        ff.setPosition(pos);
    }
}
