package com.spreys.sunshine.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Date;

import com.spreys.sunshine.app.data.WeatherContract;
import com.spreys.sunshine.app.data.WeatherContract.LocationEntry;
import com.spreys.sunshine.app.data.WeatherContract.WeatherEntry;
import com.spreys.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Created with Android Studio
 * @author vspreys
 * Date: 7/22/14.
 * Project: Sunshine
 * Contact by: vlad@spreys.com
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private ForecastAdapter forecastArrayAdapter;
    private static final int FORECAST_LOADER = 0;
    private static final String POSITION_KEY = "position";
    private String mLocation;
    private int mPosition;
    private boolean mOnePaneLayout;

    public void setOnePaneLayout(boolean isOnePaneLayout){
        this.mOnePaneLayout = isOnePaneLayout;
        if(null != this.forecastArrayAdapter){
            this.forecastArrayAdapter.setOnePayLayout(mOnePaneLayout);
        }
    }

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
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
    public static final int COL_WEATHER_ICON_ID = 5;
    public static final int COL_LOCATION_SETTING = 6;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(POSITION_KEY, mPosition);
    }

    /**
     * Initiates the SunshineService to get the latest data
     */
    private void updateWeather(){

//        //Initiate a service
//        Intent intent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
//        intent.putExtra(
//                SunshineService.KEY_LOCATION_QUERY,
//                Utility.getPreferredLocation(getActivity())
//        );
//
//        //Initiate AlarmManager
//        AlarmManager alarmMgr = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
//        PendingIntent alarmIntent = PendingIntent.getBroadcast(
//                getActivity(),
//                0,
//                intent,
//                PendingIntent.FLAG_ONE_SHOT
//        );
//
//        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, alarmIntent);
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        if(savedInstanceState != null){
            mPosition = savedInstanceState.getInt(POSITION_KEY);
        }

        // The SimpleCursorAdapter will take data from the database through the
        // Loader and use it to populate the ListView it's attached to.
        forecastArrayAdapter = new ForecastAdapter(getActivity(),null,0);
        forecastArrayAdapter.setOnePayLayout(this.mOnePaneLayout);

        ListView listView = (ListView)rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ForecastAdapter adapter = (ForecastAdapter)parent.getAdapter();
                Cursor cursor = adapter.getCursor();

                mPosition = position;
                ((Callback) getActivity()).onItemSelected(cursor.getString(COL_WEATHER_DATE));
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(R.id.action_refresh == id){
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        forecastArrayAdapter.swapCursor(data);
        ListView listView = (ListView)getActivity().findViewById(R.id.listview_forecast);

        if(listView != null){
            listView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        forecastArrayAdapter.swapCursor(null);
    }
}
