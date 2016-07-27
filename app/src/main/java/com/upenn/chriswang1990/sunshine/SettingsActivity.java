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

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.upenn.chriswang1990.sunshine.data.WeatherContract;
import com.upenn.chriswang1990.sunshine.sync.SunshineSyncAdapter;

import java.util.Locale;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    protected final static int PLACE_PICKER_REQUEST = 9090;
    PrefsFragment mPrefsFragment;
    public static ImageView mAttribution;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        mPrefsFragment = new PrefsFragment();
        mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
        mFragmentTransaction.commit();

        mAttribution = new ImageView(this);
        mAttribution.setImageResource(R.drawable.powered_by_google_light);
        if (!Utility.isLocationLatLonAvailable(this)) {
            mAttribution.setVisibility(View.GONE);
        }
        this.setListFooter(mAttribution);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener{
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));

        }

        @Override
        public void onResume() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.registerOnSharedPreferenceChangeListener(this);
            super.onResume();
        }

        @Override
        public void onPause() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        /**
         * Attaches a listener so the summary is always updated with the preference value.
         * Also fires the listener once, to initialize the summary (so it shows up before the value
         * is changed.)
         */
        private void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(this);
            // Trigger the listener immediately with the preference's
            // current value.
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            setPreferenceSummary(preference,value);
            return true;
        }

        private void setPreferenceSummary(Preference preference, Object value) {
            String stringValue = value.toString();
            String key = preference.getKey();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list (since they have separate labels/values).
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0) {
                    preference.setSummary(listPreference.getEntries()[prefIndex]);
                }
            } else if (key.equals(getString(R.string.pref_location_key))) {
                @SunshineSyncAdapter.LocationStatus int status = Utility.getLocationStatus(getActivity());
                switch (status) {
                    case SunshineSyncAdapter.LOCATION_STATUS_OK:
                        preference.setSummary(stringValue);
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                        preference.setSummary(getString(R.string.pref_location_unknown_description, value.toString()));
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                        preference.setSummary(getString(R.string.pref_location_error_description, value.toString()));
                        break;
                    default:
                        // Note --- if the server is down we still assume the value
                        // is valid
                        preference.setSummary(stringValue);
                }
            } else {
                // For other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }
        }

        // This gets called after the preference is changed, which is important because we
        // start our synchronization here
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ( key.equals(getString(R.string.pref_location_key)) ) {
                // we've changed the location
                // Wipe out any potential PlacePicker latlng values so that we can use this text entry.
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(getString(R.string.pref_location_latitude));
                editor.remove(getString(R.string.pref_location_longitude));
                editor.commit();

                // Remove attributions for our any PlacePicker locations.
                mAttribution.setVisibility(View.GONE);

                // Clear the location status
                Utility.resetLocationStatus(getActivity());
                SunshineSyncAdapter.syncImmediately(getActivity());
            } else if ( key.equals(getString(R.string.pref_units_key)) ) {
                // units have changed. update lists of weather entries accordingly
                getActivity().getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
            } else if ( key.equals(getString(R.string.pref_location_status_key)) ) {
                // our location status has changed.  Update the summary accordingly
                Preference locationPreference = findPreference(getString(R.string.pref_location_key));
                bindPreferenceSummaryToValue(locationPreference);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check to see if the result is from our Place Picker intent
        if (requestCode == PLACE_PICKER_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                String address = place.getAddress().toString();
                LatLng latLong = place.getLatLng();
                // If the provided place doesn't have an address, we'll form a display-friendly
                // string from the latlng values.
                if (TextUtils.isEmpty(address)) {
                    address = String.format(Locale.US, "(%.2f, %.2f)",latLong.latitude, latLong.longitude);
                }
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.pref_location_key), address);
                // Also store the latitude and longitude so that we can use these to get a precise
                // result from our weather service. We cannot expect the weather service to
                // understand addresses that Google formats.
                editor.putFloat(getString(R.string.pref_location_latitude),
                        (float) latLong.latitude);
                editor.putFloat(getString(R.string.pref_location_longitude),
                        (float) latLong.longitude);
                editor.commit();

                // Tell the SyncAdapter that we've changed the location, so that we can update
                // our UI with new values. We need to do this manually because we are responding
                // to the PlacePicker widget result here instead of allowing the
                // LocationEditTextPreference to handle these changes and invoke our callbacks.
                Preference locationPreference = mPrefsFragment.findPreference(getString(R.string.pref_location_key));
                mPrefsFragment.setPreferenceSummary(locationPreference, address);
                mAttribution.setVisibility(View.VISIBLE);
                Utility.resetLocationStatus(this);
                SunshineSyncAdapter.syncImmediately(this);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
