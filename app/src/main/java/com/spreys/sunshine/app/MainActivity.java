package com.spreys.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.spreys.sunshine.app.data.WeatherContract;
import com.spreys.sunshine.app.sync.SunshineSyncAdapter;

import java.util.Date;


public class MainActivity extends ActionBarActivity implements Callback{
    private boolean mTwoPane = false;
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(null != findViewById(R.id.weather_detail_container)){
            mTwoPane = true;

            if(null == savedInstanceState){
                String startDate = WeatherContract.getDbDateString(new Date());

                DetailFragment fragment = new DetailFragment();

                Bundle args = new Bundle();
                args.putString(DetailFragment.ARG_DATE, startDate);

                fragment.setArguments(args);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, fragment)
                        .commit();
            }
        }

        ForecastFragment forecastFragment = (ForecastFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast);

        forecastFragment.setOnePaneLayout(!mTwoPane);
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
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String date) {
        Log.d(LOG_TAG, date);

        if(mTwoPane){
            DetailFragment fragment = new DetailFragment();

            Bundle args = new Bundle();
            args.putString(DetailFragment.ARG_DATE, date);

            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment)
                    .commit();
        } else {
            Intent detailActivityIntent = new Intent(this, DetailActivity.class)
                    .putExtra(Intent.EXTRA_TEXT, date);
            startActivity(detailActivityIntent);
        }
    }
}
