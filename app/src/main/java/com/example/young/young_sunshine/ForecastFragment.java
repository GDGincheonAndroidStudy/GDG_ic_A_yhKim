package com.example.young.young_sunshine;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
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

/**
 * Created by apple on 2016-01-21.
 */
public class ForecastFragment extends Fragment {
    //private static ArrayAdapter<String> mForecastAdapter = null;
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();

            return true;
        }/*else if(id == R.id.action_setting){
            Intent it = new Intent(getActivity(), SettingActivity.class );
            startActivity(it);
            return true;
        }*/
        if(id ==R.id.action_map){
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void openPreferredLocationInMap(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = sharedPrefs.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
    }


    public void updateWeather(){
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String strLocation = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        weatherTask.execute(strLocation);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        /*String strForecastArray[] = {
                "Today-sunny - 88/63 "
                , "Tomorrow-foggy - 70/46"
                , "Weds-Cloudy - 72/63"
                , "Thurs-Rainy - 64/51"
                , "Fri-Foggy - 70/46"
                , "Say-Sunny - 76/68"};

        List<String> weekForecast = new ArrayList<String>(Arrays.asList(strForecastArray));
*/
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>()
        );


        ListView listForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        listForecast.setAdapter(mForecastAdapter);
        listForecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);
                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                Intent it = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(it);

            }
        });


        return rootView;

    }


    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {



        @Override
        protected void onPostExecute(String[] result){
            if(result != null){
                mForecastAdapter.clear();
                for(String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
            mForecastAdapter.notifyDataSetChanged();

        }

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                //URL url = new URL("http://api.openweather.org/data/2.5/forecast/daily?g=94043&mode=json&units=metric&ent=7&app_key=http://api.openweathermap.org/data/2.5/forecast/city?id=524901");
//                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
//                String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
//                URL url = new URL(baseUrl.concat(apiKey));


                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Built URI " + builtUri.toString());


                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Forecast JSON String" + forecastJsonStr);





            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                //e.printStackTrace();
                forecastJsonStr = null;
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

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(),e);
                e.printStackTrace();
            }


            return null;
        }

        private String getReadableDateString(long time){
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM DD");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high, double low){
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh +"/" + roundedLow;
            return highLowStr;
        }
        private String formatHighLows(double high, double low, String unitType){
            if(unitType.equals(getString(R.string.pref_temperature_imperial))){
                high = (high *1.8) +32;
                low = (low*1.8) +32;
            }else if(!unitType.equals(getString(R.string.pref_temperature_metric))){
                Log.d(LOG_TAG,"Unit type not found:" +unitType);
            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh +"/"+ roundedLow;
            return highLowStr;
        }


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException{

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();
            String[] resultStrs = new String[numDays];

            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPrefs.getString(
                    getString(R.string.pref_temperature_key),
                    getString(R.string.pref_temperature_metric));


            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, unitType);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;
        }

    }

}
