package com.spreys.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.spreys.sunshine.app.data.WeatherContract;
import com.spreys.sunshine.app.data.WeatherContract.LocationEntry;
import com.spreys.sunshine.app.data.WeatherContract.WeatherEntry;


/**
 * Created with Android Studio
 * @author vspreys
 * Date: 27/07/14.
 * Project: Sunshine
 * Contact by: vlad@spreys.com
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private String forecastData;
    private static final int FORECAST_LOADER = 0;
    private ShareActionProvider mShareActionProvider;
    private String mLocation;
    private static final String LOCATION_KEY = "location";
    public static final String ARG_DATE = "date";

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_LOCATION_SETTING
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_HUMIDITY = 5;
    public static final int COL_WEATHER_WIND_SPEED = 6;
    public static final int COL_WEATHER_PRESSURE = 7;
    public static final int COL_DEGREES = 8;
    public static final int COL_WEATHER_ICON_ID = 9;
    public static final int COL_LOCATION_SETTING = 10;

    public DetailFragment(){

    }

    public String getDate(){
        if(null != getArguments()){
            return getArguments().getString(ARG_DATE);
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_fragment, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);

        mShareActionProvider.setShareIntent(getShareIntent());

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        /*
         * Initializes the CursorLoader. The URL_LOADER value is eventually passed
         * to onCreateLoader().
         */
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);

        return rootView;
    }

    private Intent getShareIntent(){
        String messageToSend = forecastData + " #SunishineApp";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, messageToSend);

        return shareIntent;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LOCATION_KEY, mLocation);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        mLocation = Utility.getPreferredLocation(getActivity());
        String date = getDate();

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                mLocation, date);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(cursor.moveToFirst()){
            boolean isMetric = Utility.isMetric(getActivity());

            String forecastDay = Utility.getDayName(
                    getActivity(),
                    cursor.getString(COL_WEATHER_DATE)
            );

            String forecastDate = Utility.getFormattedMonthDay(
                    getActivity(),
                    cursor.getString(COL_WEATHER_DATE)
            );
            String forecastDesc = cursor.getString(COL_WEATHER_DESC);

            String forecastMinTemp = Utility.formatTemperature(
                    getActivity(),
                    cursor.getDouble(COL_WEATHER_MIN_TEMP),
                    isMetric
            );

            String forecastMaxTemp = Utility.formatTemperature(
                    getActivity(),
                    cursor.getDouble(COL_WEATHER_MAX_TEMP),
                    isMetric
            );

            String forecastHumidity = Utility.getFormattedHumidity(
                    getActivity(),
                    cursor.getFloat(COL_WEATHER_HUMIDITY)
            );
            String forecastWind = Utility.getFormattedWind(
                    getActivity(),
                    cursor.getFloat(COL_WEATHER_WIND_SPEED),
                    cursor.getFloat(COL_DEGREES)
            );

            String forecastPressure = Utility.getFormattedPressure(
                    getActivity(),
                    cursor.getFloat(COL_WEATHER_PRESSURE)
            );

            int weatherArtResourceId = Utility.getArtResourceForWeatherCondition(
                    cursor.getInt(COL_WEATHER_ICON_ID)
            );

            ((TextView)getActivity().findViewById(R.id.lblDay)).setText(forecastDay);
            ((TextView)getActivity().findViewById(R.id.lblDate)).setText(forecastDate);
            ((TextView)getActivity().findViewById(R.id.lblDesc)).setText(forecastDesc);
            ((TextView)getActivity().findViewById(R.id.lblHighTemp)).setText(forecastMinTemp);
            ((TextView)getActivity().findViewById(R.id.lblLowTemp)).setText(forecastMaxTemp);
            ((TextView)getActivity().findViewById(R.id.lblHumidity)).setText(forecastHumidity);
            ((TextView)getActivity().findViewById(R.id.lblWind)).setText(forecastWind);
            ((TextView)getActivity().findViewById(R.id.lblPressure)).setText(forecastPressure);
            ((ImageView)getActivity().findViewById(R.id.imgTempArt)).setImageResource(weatherArtResourceId);

            forecastData = String.format(
              "%s - %s - %s/%s", forecastDate, forecastDesc, forecastMinTemp, forecastMaxTemp
            );

            if(null != mShareActionProvider){
                mShareActionProvider.setShareIntent(getShareIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }
}
