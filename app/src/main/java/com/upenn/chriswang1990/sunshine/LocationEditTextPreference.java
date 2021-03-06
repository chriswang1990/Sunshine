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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

public class LocationEditTextPreference extends EditTextPreference {
    private static final int DEFAULT_MINIMUM_LOCATION_LENGTH = 2;
    private int mMinLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.LocationEditTextPreference,
                0,
                0);
        try {
            mMinLength = a.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MINIMUM_LOCATION_LENGTH);
        } finally {
            a.recycle();
        }

        // If this is false, we'll just carry on as though this
        // feature does not exist. If it is true, however, we can add a widget to our preference.

        if (Utility.isGooglePlayServiceAvailable(context)) {
            // Add the get current location widget to our location preference
            setWidgetLayoutResource(R.layout.pref_current_location);
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        if (Utility.isGooglePlayServiceAvailable(getContext())) {
            View currentLocation = view.findViewById(R.id.current_location);
            currentLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Launch the Place Picker so that the user can specify their location, and then
                    // return the result to SettingsActivity.
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                    // We are in a view right now, not an activity. So we need to get ourselves
                    // an activity that we can use to start our Place Picker intent. By using
                    // SettingsActivity in this way, we can ensure the result of the Place Picker
                    // intent comes to the right place for us to process it.
                    Context context = getContext();
                    Activity settingsActivity = (SettingsActivity) context;
                    try {
                        settingsActivity.startActivityForResult(
                                builder.build(settingsActivity), SettingsActivity.PLACE_PICKER_REQUEST);

                    } catch (GooglePlayServicesNotAvailableException
                            | GooglePlayServicesRepairableException e) {
                    }
                }
            });
        }
        return view;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        EditText et = getEditText();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Dialog d = getDialog();
                if (d instanceof AlertDialog) {
                    AlertDialog dialog = (AlertDialog) d;
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    // Check if the EditText is empty
                    if (editable.length() < mMinLength) {
                        // Disable OK button
                        positiveButton.setEnabled(false);
                    } else {
                        // Re-enable the button.
                        positiveButton.setEnabled(true);
                    }
                }
            }
        });
    }
}
