package com.piyushagade.meteo;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather mCurrentWeather;
    private DailyWeather mDailyWeather;
    private  TextView mTimeLabel, mTempLabel, mHumidityValue, mPrecipLabel,mSummary, mLocationLabel, mDaily;
    private ImageView mIcon, mRefresh;
    ProgressBar mProgressBar;
    RelativeLayout bg;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI Components
        mTimeLabel = (TextView) findViewById(R.id.timeLable);
        mTempLabel = (TextView)  findViewById(R.id.tempLabel);
        mHumidityValue = (TextView)findViewById (R.id.humidityValue);
        mPrecipLabel = (TextView) findViewById(R.id.precipValue);
        mLocationLabel = (TextView)findViewById (R.id.locationLabel);
        mSummary = (TextView)findViewById (R.id.summary);
        mIcon = (ImageView) findViewById(R.id.iconImage);
        bg = (RelativeLayout) findViewById(R.id.bg);
        mRefresh = (ImageView) findViewById(R.id.refreshImage);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mDaily = (TextView) findViewById(R.id.dailytv);

        //Animations
        Animation floatAnim = AnimationUtils.loadAnimation(this, R.anim.float_anim);
        bg.startAnimation(floatAnim);

        Animation prakatAnim = AnimationUtils.loadAnimation(this, R.anim.prakat);
        mTempLabel.startAnimation(prakatAnim);

        // Location
        final double latitude = 29.6516;
        final double longitude = -82.3248;

        mProgressBar.setVisibility(View.INVISIBLE);

        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
            }
        });

        getForecast(latitude, longitude);
    }

    private void getForecast(double latitude, double longitude) {
        if(isNetworkAvailable()) {
            toggleRefresh();

            // Http Connection
            String apiKey = "ae90bcca3fe03f309f261f05519b03f1";

            String url = "https://api.forecast.io/forecast/" + apiKey + "/" + latitude + "," + longitude;

            //Client Object
            OkHttpClient client = new OkHttpClient();
            //Request object
            Request r = new Request.Builder()
                    .url(url).
                            build();

            Call call = client.newCall(r);

            //Callback
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUser("Some Error occured.");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        if (response.isSuccessful()) {
                            String jsonData = response.body().string();
                            try {
                                mCurrentWeather = getCurrentDetails(jsonData);
                                mDailyWeather = getDailyDetails(jsonData);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateData();
                                    }
                                });
                            } catch (JSONException e) {
                                alertUser("JSON exception occurred.");
                                Log.e(TAG, "Error", e);
                            }
                            Log.v(TAG, jsonData);
                        } else
                            alertUser("Oops! Please try again later.");
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        }
        else
            alertUser("Network not available.");
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefresh.setVisibility(View.INVISIBLE);
        }
        else
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void updateData() {
        mTempLabel.setText(mCurrentWeather.getTemp() + "");
        mTimeLabel.setText("At "+mCurrentWeather.getFormattedTime()+" it will be");
        mPrecipLabel.setText(mCurrentWeather.getPrecepitation() + "");
        mHumidityValue.setText(mCurrentWeather.getHumidity()+"");
        mSummary.setText(mCurrentWeather.getSummary() + "");
        mLocationLabel.setText(mCurrentWeather.getTimezone() + "");
        Drawable drawable = getResources().getDrawable((mCurrentWeather.getIcon()));
        mIcon.setImageDrawable(drawable);

        mDaily.setText(mDailyWeather.getSummary());
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException{
        JSONObject jo = new JSONObject(jsonData);
        String timezone = jo.getString("timezone");

        JSONObject currently = jo.getJSONObject("currently");
        CurrentWeather cw = new CurrentWeather();
        cw.setHumidity(currently.getDouble("humidity"));
        cw.setTime(currently.getInt("time"));
        cw.setIcon(currently.getString("icon"));
        cw.setPrecepitation(currently.getDouble("precipProbability"));
        cw.setTemp(currently.getDouble("temperature"));
        cw.setSummary(currently.getString("summary"));
        cw.setTimezone(timezone);

        return cw;
    }

    private DailyWeather getDailyDetails(String jsonData) throws JSONException{
        JSONObject jo = new JSONObject(jsonData);
        String timezone = jo.getString("timezone");

        JSONObject daily = jo.getJSONObject("daily");
        DailyWeather dw = new DailyWeather();
        dw.setSummary(daily.getString("summary"));

        return dw;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean state;
        if(ni != null && ni.isConnected())
            state = true;
        else
            state = false;

        return state;
    }

    private void alertUser(String data) {
        Snackbar.make(findViewById(android.R.id.content), data, Snackbar.LENGTH_LONG).show();
    }
}
