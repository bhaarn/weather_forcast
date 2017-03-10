package com.wipro.weatherforecast;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by BHAARN on 3/10/2017.
 */

public class WeatherActivity extends AppCompatActivity implements
        WeatherFeedFragment.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.weather_content, new WeatherFeedFragment()).commit();
    }

    @Override
    public void onWeatherPageBackArrowClicked() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
