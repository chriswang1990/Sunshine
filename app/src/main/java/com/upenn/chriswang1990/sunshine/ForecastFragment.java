package com.upenn.chriswang1990.sunshine;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> forecastAdapter;

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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ArrayList<String> forecastData = new ArrayList<>();
        forecastData.add("Today - Sunny - 20/10");
        forecastData.add("Tomorrow - Rainy - 15/9");
        forecastData.add("Wednesday - Sunny - 22/10");
        forecastData.add("Thursday - Cloudy - 15/8");
        forecastData.add("Friday - Sunny - 23/13");
        forecastData.add("Saturday - Sunny - 26/16");
        forecastData.add("Sunny - Sunny - 25/15");

        forecastAdapter = new ArrayAdapter<>(getActivity(), R.layout
                .list_item_forecast, R.id.list_item_forecast_textview, forecastData);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastList.setAdapter(forecastAdapter);
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        new FetchWeatherTask().execute();
        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<Void, Void, Void> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        protected Void doInBackground(Void... urls) {
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                String baseUrl = "http://api.openweathermap.org/data/2" +
                        ".5/forecast/daily?q=Sunnyvale,%20" +
                        "US&units=metric&cnt=7";
                String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
                URL url = new URL(baseUrl.concat(apiKey));
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder builder = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    builder.append(line);
                    builder.append("\n");
                }
                if (builder.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = builder.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }
    }
}

