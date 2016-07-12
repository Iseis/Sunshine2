package com.apps.andrew.sunshine;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    public ArrayAdapter<String> weatherAdapter;

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    static final String EXTRA_WEATHER = "com.andrew.EXTRA_WEATHER";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //Handle action bar clicks here
        int id = item.getItemId();
        if(id == R.id.action_refresh)
        {
            new FetchWeatherTask().execute("97402");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/40",
                "Weds - Sunny - 80/64",
                "Thurs - Help Frogs - 40/32",
                "Fri - Slimy - 50/55",
                "Sat - Good - 90/55",
                "Sun - Not Good - 40/22"
        };

        List<String> weekForecast = new ArrayList<>(Arrays.asList(forecastArray));

        weatherAdapter = new ArrayAdapter<String>(
                getActivity(), R.layout.list_item_forcast, R.id.list_item_forecast_textview, weekForecast
        );

        //Get a refrence to the list view in the fragment xml
        final ListView listView = (ListView) view.findViewById(R.id.listview_forcast);

        listView.setAdapter(weatherAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String weather = weatherAdapter.getItem(position);

                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(EXTRA_WEATHER, weather);
                startActivity(intent);
            }
        });



        return view;
    }


    public class  FetchWeatherTask extends AsyncTask<String, Void, String[]>
    {

        protected String[] doInBackground(String... params)
        {
            //Need these now so we can close them later
            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;

            //Used for the raw json
            String forecastJsonStr = null;

            //Build the uri for the weather query
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("api.openweathermap.org")
                    .appendPath("data")
                    .appendPath("2.5")
                    .appendPath("forecast")
                    .appendPath("daily")
                    .appendQueryParameter("q", params[0])
                    .appendQueryParameter("mode", "json")
                    .appendQueryParameter("units", "metric")
                    .appendQueryParameter("cnt", "7")
                    .appendQueryParameter("APPID", "2b4da47eabf82fcdc57bfa38f886e13d");


            try {
                //Construct the url for the openweathermaq query
                //Possible parameters are available at http://openweathermap.org/API#forecast
                URL url = new URL(builder.toString());

                //Create the request  to openweathermap.org and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                //Read the input stream
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer stringBuffer = new StringBuffer();
                if(inputStream == null)
                {
                    //nothing to do
                    return null;
                }

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while((line = bufferedReader.readLine()) != null)
                {
                    stringBuffer .append(line + "\n");
                }

                if(stringBuffer.length() == 0)
                {
                    return null;
                }

                forecastJsonStr = stringBuffer.toString();
            } catch (IOException e)
            {
                Log.e(LOG_TAG, "Error", e);

                return null;
            } finally
            {
                if(urlConnection != null)
                {
                    urlConnection.disconnect();
                }
                if(bufferedReader != null)
                {
                    try
                    {
                        bufferedReader.close();
                    } catch (final IOException e)
                    {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try{
                return getWeatherDataFromJson(forecastJsonStr, 7);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @TargetApi(11)
        @Override
        protected void onPostExecute(String[] strings) {
            if(strings != null) {
                List<String> weekForecast = new ArrayList<>(Arrays.asList(strings));
                weatherAdapter.clear();
                weatherAdapter.addAll(weekForecast);
            }
        }
    }


    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);


        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            //create a Gregorian Calendar, which is in current date
            GregorianCalendar gc = new GregorianCalendar();
            //add i dates to current date of calendar
            gc.add(GregorianCalendar.DATE, i);
            //get that date, format it, and "save" it on variable day
            Date time = gc.getTime();
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            day = shortenedDateFormat.format(time);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;

    }


}
