/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.utilities.ServerApi;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;

import org.json.JSONException;
import org.json.JSONObject;


public class DataService extends Service {

    private static final String ALARM_TIMER = "ALARM_TIMER";
    private static final String TAG = "DataService";
    private static final String GET_PLACES = "GET_PLACES";

    private AlarmManager alarm;
    private PendingIntent alarmCallback;
    private static Boolean serviceRunning = false;


    public static void start(Context context) {

        Log.d(TAG, "start Service called");
        if (serviceRunning) { // If service is running, no need to start it again.
            Log.w(TAG, "Service already running...");
            return;
        }
        context.startService(new Intent(context, DataService.class));
    }

    public static void stop(Context context) {

        Log.d(TAG, "stop Service called");
        context.stopService(new Intent(context, DataService.class));
    }

    public static void getPlaces(Context context) {

        Log.d(TAG, "getPlaces");
        Intent placesIntent = new Intent(context, DataService.class);
        placesIntent.putExtra(GET_PLACES, 1);
        context.startService(placesIntent);
    }

    public static void getDashboard(Context context) {

        Log.d(TAG, "getDashboard");
        Intent placesIntent = new Intent(context, DataService.class);
        placesIntent.putExtra(ALARM_TIMER, 1);
        context.startService(placesIntent);
    }

    public static void updateDashboard(Location location) {

        Log.d(TAG, "updateDashboard");
        if (MainApplication.dashboard == null) {
            return;
        }
        try {
            // TODO: hardcoded keys
            JSONObject dashboardLocation = MainApplication.dashboard.getJSONObject("location");
            dashboardLocation.put("lat", location.getLatitude());
            dashboardLocation.put("lon", location.getLongitude());
            dashboardLocation.put("acc", location.getAccuracy());
            dashboardLocation.put("time", location.getTime());
            MainApplication.dashboard.put("location", dashboardLocation);
            Log.d(TAG, "new Dashboard: " + MainApplication.dashboard);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate");
        super.onCreate();
        setTimer();
        serviceRunning = true;
        try {
            MainApplication.dashboard = new JSONObject(PreferenceUtils.getString(this.getApplicationContext(), PreferenceUtils.KEY_DASHBOARD));
        } catch (JSONException e) {
            MainApplication.dashboard = null;
        }
        getPlaces();
    }

    private void setTimer() {

        alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, DataService.class);
        alarmIntent.putExtra(ALARM_TIMER, 1);
        alarmCallback = PendingIntent.getService(this, 0, alarmIntent, 0);
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 30 * 1000, alarmCallback);
        Log.d(TAG, "Timer created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand invoked");

        if (intent == null) {
            return START_STICKY;
        }
        // Check that intent isnt null, and service is connected to Google Play Services
        Bundle extras = intent.getExtras();

        if (extras == null) {
            return START_STICKY;
        }

        if (extras.containsKey(ALARM_TIMER)) {
            fetchDashboard();
        } else if (extras.containsKey(GET_PLACES)) {
            getPlaces();
        }
        return START_STICKY;
    }

    private void getPlaces() {

        Log.d(TAG, "getPlaces");
        ServerApi.getPlaces(this);
    }

    private void fetchDashboard() {

        Log.d(TAG, "alarmCallback");
        ServerApi.getDashboard(this);
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy");
        alarm.cancel(alarmCallback);
        serviceRunning = false;
        super.onDestroy();
    }
}
