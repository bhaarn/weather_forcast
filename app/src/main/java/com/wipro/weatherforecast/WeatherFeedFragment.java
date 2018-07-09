package com.wipro.weatherforecast;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wipro.weatherforecast.adapters.RecyclerViewAdapter;
import com.wipro.weatherforecast.databinding.WeatherFeedFragmentBinding;
import com.wipro.weatherforecast.entity.WeatherObject;
import com.wipro.weatherforecast.helpers.Helper;
import com.wipro.weatherforecast.models.FiveWeathers;
import com.wipro.weatherforecast.models.Forecast;
import com.wipro.weatherforecast.models.LocationMapObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by BHAARN on 3/10/2017.
 */

public class WeatherFeedFragment extends Fragment implements LocationListener {
    private WeatherFeedFragment.Listener listener;
    WeatherFeedFragmentBinding weatherFeedFragmentBinding;
    private RecyclerViewAdapter recyclerViewAdapter;
    private RequestQueue queue;

    private LocationMapObject locationMapObject;

    private LocationManager locationManager;

    private Location location;

    private final int REQUEST_LOCATION = 200;

    private String apiUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        weatherFeedFragmentBinding = DataBindingUtil.inflate(inflater,
                R.layout.weather_feed_fragment, container, false);
        setupToolbar();
        queue = Volley.newRequestQueue(getActivity());
        locationManager = (LocationManager) getActivity().getSystemService(Service.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 2, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(location != null) {
                    apiUrl = "http://api.openweathermap.org/data/2.5/weather?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&APPID=" + Helper.API_KEY + "&units=metric";
                    makeJsonObject(apiUrl);
                } else {
                    Toast.makeText(getContext(), "Unable to load weather due to network issues", Toast.LENGTH_SHORT).show();
                }
            }
        }

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 4);

        weatherFeedFragmentBinding.weatherDailyList.setLayoutManager(gridLayoutManager);
        weatherFeedFragmentBinding.weatherDailyList.setHasFixedSize(true);
        return weatherFeedFragmentBinding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (WeatherFeedFragment.Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getName() + " must implement "
                    + WeatherFeedFragment.Listener.class.getName());
        }
    }

    private void setupToolbar() {
        weatherFeedFragmentBinding.weatherToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        weatherFeedFragmentBinding.weatherToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onWeatherPageBackArrowClicked();
            }
        });
    }

    private void makeJsonObject(final String apiUrl) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                locationMapObject = gson.fromJson(response, LocationMapObject.class);
                if (null != locationMapObject) {
                    String city = locationMapObject.getName() + ", " + locationMapObject.getSys().getCountry();
                    String todayDate = getTodayDateInStringFormat();
                    Long tempVal = Math.round(Math.floor(Double.parseDouble(locationMapObject.getMain().getTemp())));
                    String weatherTemp = String.valueOf(tempVal) + "°";
                    String weatherDescription = Helper.capitalizeFirstLetter(locationMapObject.getWeather().get(0).getDescription());
                    String windSpeed = locationMapObject.getWind().getSpeed();
                    String humidityValue = locationMapObject.getMain().getHumudity();
                    String sunRise = locationMapObject.getSys().getSunrise();
                    String sunSet = locationMapObject.getSys().getSunset();

                    Toast.makeText(getActivity(), "Sun Rise : " + getFormattedTime(sunRise) + " Sun Set : " + getFormattedTime(sunSet), Toast.LENGTH_LONG).show();

                    weatherFeedFragmentBinding.cityCountry.setText(Html.fromHtml(city));
                    weatherFeedFragmentBinding.currentDate.setText(Html.fromHtml(todayDate));
                    weatherFeedFragmentBinding.weatherResult.setTitleText(Html.fromHtml(weatherTemp).toString());
                    weatherFeedFragmentBinding.weatherResult.setSubtitleText(Html.fromHtml(weatherDescription).toString());
                    weatherFeedFragmentBinding.windResult.setText(Html.fromHtml(windSpeed) + " km/h");
                    weatherFeedFragmentBinding.humidityResult.setText(Html.fromHtml(humidityValue) + " %");

                    fiveDaysApiJsonObjectCall(locationMapObject.getName());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Bharani", error.toString());
            }
        });
        queue.add(stringRequest);
    }

    private Date getFormattedTime(String value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(value)*1000);
        return calendar.getTime();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 2, this);
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        apiUrl = "http://api.openweathermap.org/data/2.5/weather?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&APPID=" + Helper.API_KEY + "&units=metric";
                        makeJsonObject(apiUrl);
                    } else {
                        apiUrl = "http://api.openweathermap.org/data/2.5/weather?lat=51.5074&lon=0.1278&APPID=" + Helper.API_KEY + "&units=metric";
                        makeJsonObject(apiUrl);
                    }
                }
            } else {
                Toast.makeText(getActivity(), getString(R.string.permission_notice), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledAlertToUser();
        }
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private String getTodayDateInStringFormat() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("E, d MMMM", Locale.getDefault());
        return df.format(c.getTime());
    }

    private void fiveDaysApiJsonObjectCall(String city) {
        String apiUrl = "http://api.openweathermap.org/data/2.5/forecast?q=" + city + "&APPID=" + Helper.API_KEY + "&units=metric";
        final List<WeatherObject> daysOfTheWeek = new ArrayList<WeatherObject>();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                Forecast forecast = gson.fromJson(response, Forecast.class);
                if (null != forecast) {
                    int[] everyday = new int[]{0, 0, 0, 0, 0, 0, 0};
                    List<FiveWeathers> weatherInfo = forecast.getList();
                    if (null != weatherInfo) {
                        for (int i = 0; i < weatherInfo.size(); i++) {
                            String time = weatherInfo.get(i).getDt_txt();
                            String shortDay = convertTimeToDay(time);
                            String temp = weatherInfo.get(i).getMain().getTemp();
                            String tempMin = weatherInfo.get(i).getMain().getTemp_min();

                            if (convertTimeToDay(time).equals("Mon") && everyday[0] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[0] = 1;
                            }
                            if (convertTimeToDay(time).equals("Tue") && everyday[1] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[1] = 1;
                            }
                            if (convertTimeToDay(time).equals("Wed") && everyday[2] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[2] = 1;
                            }
                            if (convertTimeToDay(time).equals("Thu") && everyday[3] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[3] = 1;
                            }
                            if (convertTimeToDay(time).equals("Fri") && everyday[4] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[4] = 1;
                            }
                            if (convertTimeToDay(time).equals("Sat") && everyday[5] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[5] = 1;
                            }
                            if (convertTimeToDay(time).equals("Sun") && everyday[6] < 1) {
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[6] = 1;
                            }
                            recyclerViewAdapter = new RecyclerViewAdapter(getContext(), daysOfTheWeek);
                            weatherFeedFragmentBinding.weatherDailyList.setAdapter(recyclerViewAdapter);
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        queue.add(stringRequest);
    }

    private String convertTimeToDay(String time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SSSS", Locale.getDefault());
        String days = "";
        try {
            Date date = format.parse(time);
            System.out.println("Our time " + date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            days = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
            System.out.println("Our time " + days);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return days;
    }

    public interface Listener {
        void onWeatherPageBackArrowClicked();
    }
}

