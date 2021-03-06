/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.utilities;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.constants.Constants;
import cc.softwarefactory.lokki.android.errors.AddPlaceError;
import cc.softwarefactory.lokki.android.services.DataService;


public class ServerApi {

    private static final String TAG = "ServerApi";
    private static String ApiUrl = Constants.API_URL;

    public static void signUp(Context context, AjaxCallback<JSONObject> signUpCallback) {

        Log.d(TAG, "Sign up");
        AQuery aq = new AQuery(context);
        String url = ApiUrl + "signup";

        String userAccount = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ACCOUNT);
        String deviceId = PreferenceUtils.getString(context, PreferenceUtils.KEY_DEVICE_ID);
        Map<String, Object> params = new HashMap<>();
        params.put("email", userAccount);
        params.put("device_id", deviceId);

        if (!Utils.getLanguage().isEmpty()) {
            params.put("language", Utils.getLanguage());
        }

        aq.ajax(url, params, JSONObject.class, signUpCallback);
        Log.d(TAG, "Sign up - email: " + userAccount + ", deviceId: " + deviceId + ", language: " + Utils.getLanguage());
    }


    public static void getDashboard(final Context context) {
        Log.d(TAG, "getDashboard");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        final String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl  + "user/" + userId + "/dashboard";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "dashboardCallback");

                if (status.getCode() == 401) {
                    Log.e(TAG, "Status login failed. App should exit.");
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_AUTH_TOKEN, "");
                    Intent intent = new Intent("EXIT");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                } else if (json != null){
                    Log.d(TAG, "json returned: " + json);
                    MainApplication.dashboard = json;
                    PreferenceUtils.setString(context, PreferenceUtils.KEY_DASHBOARD, json.toString());
                    Intent intent = new Intent("LOCATION-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                } else {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                }
            }
        };
        cb.header("authorizationtoken", authorizationToken);
        aq.ajax(url, JSONObject.class, cb);
    }

    public static void getPlaces(final Context context) {

        Log.d(TAG, "getPlaces");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/places";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "placesCallback");

                if (json == null) {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                    return;
                }
                Log.d(TAG, "json returned: " + json);
                MainApplication.places = json;
                PreferenceUtils.setString(context, PreferenceUtils.KEY_PLACES, json.toString());
                Intent intent = new Intent("PLACES-UPDATE");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        };
        cb.header("authorizationtoken", authorizationToken);
        aq.ajax(url, JSONObject.class, cb);
    }

    public static void allowPeople(final Context context, String email) throws JSONException {

        Log.d(TAG, "allowPeople");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/allow";

        JSONArray JSONemails = new JSONArray();
        JSONemails.put(email);

        JSONObject JSONdata = new JSONObject()
                .put("emails", JSONemails);

        Log.d(TAG, "Emails to be alloweed: " + JSONdata);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "allowPeople result code: " + status.getCode());
                Log.d(TAG, "allowPeople result message: " + status.getMessage());
                Log.e(TAG, "allowPeople ERROR: " + status.getError());
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new dashboard");
                    DataService.getDashboard(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void disallowUser(final Context context, String email) {

        Log.d(TAG, "disallowUser");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/allow/";
        String targetId = Utils.getIdFromEmail(context, email);

        if (targetId == null) {
            return;
        }
        url += targetId;
        Log.d(TAG, "Email to be disallowed: " + email + ", userIdToDisallow: " + targetId);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "disallowUser result code: " + status.getCode());
                Log.d(TAG, "disallowUser result message: " + status.getMessage());
                Log.e(TAG, "disallowUser ERROR: " + status.getError());
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new dashboard");
                    DataService.getDashboard(context);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.delete(url, String.class, cb);
    }

    public static void sendLocation(Context context, Location location) throws JSONException {

        Log.d(TAG, "sendLocation");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/location";

        JSONObject JSONlocation = new JSONObject()
                .put("lat", location.getLatitude())
                .put("lon", location.getLongitude())
                .put("acc", location.getAccuracy());

        JSONObject JSONdata = new JSONObject()
                .put("location", JSONlocation);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "sendLocation result code: " + status.getCode());
                Log.d(TAG, "sendLocation result message: " + status.getMessage());
                Log.e(TAG, "sendLocation ERROR: " + status.getError());
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void sendGCMToken(Context context, String GCMToken) throws JSONException {

        Log.d(TAG, "sendGCMToken");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/gcmToken";

        JSONObject JSONdata = new JSONObject()
                .put("gcmToken", GCMToken);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "sendGCMToken result code: " + status.getCode());
                Log.d(TAG, "sendGCMToken result message: " + status.getMessage());
                Log.e(TAG, "sendGCMToken ERROR: " + status.getError());
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void requestUpdates(Context context) throws JSONException {

        Log.d(TAG, "requestUpdates");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/update/locations";

        JSONObject JSONdata = new JSONObject()
                .put("item", "");

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "requestUpdates result code: " + status.getCode());
                Log.d(TAG, "requestUpdates result message: " + status.getMessage());
                Log.e(TAG, "requestUpdates ERROR: " + status.getError());
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, String.class, cb);
    }

    public static void setVisibility(Context context, Boolean visible) throws JSONException {

        Log.d(TAG, "setVisibility");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/visibility";

        JSONObject JSONdata = new JSONObject()
                .put("visibility", visible);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "setVisibility result code: " + status.getCode());
                Log.d(TAG, "setVisibility result message: " + status.getMessage());
                Log.e(TAG, "setVisibility ERROR: " + status.getError());
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.put(url, JSONdata, String.class, cb);
    }

    public static void addPlace(final Context context, String name, LatLng latLng, int radius) throws JSONException {

        Log.d(TAG, "addPlace");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/place";


        String cleanName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();

        JSONObject JSONdata = new JSONObject()
                .put("lat", latLng.latitude)
                .put("lon", latLng.longitude)
                .put("rad", radius)
                .put("img", "")
                .put("name", cleanName);

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                Log.d(TAG, "addPlace result code: " + status.getCode());
                Log.d(TAG, "addPlace result message: " + status.getMessage());
                Log.e(TAG, "addPlace ERROR: " + status.getError());

                if (status.getError() != null) {
                    handleError(status);
                    return;
                }

                Log.d(TAG, "No error, place created.");
                Toast.makeText(context, context.getString(R.string.place_created), Toast.LENGTH_SHORT).show();
                DataService.getPlaces(context);
            }

            private void handleError(AjaxStatus status) {

                AddPlaceError ape = AddPlaceError.getEnum(status.getError());
                if (ape == null) {
                    return;
                }

                String toastMessage = context.getString(ape.getErrorMessage());
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.post(url, JSONdata, JSONObject.class, cb);
    }

    public static void removePlace(final Context context, final String placeId) {

        Log.d(TAG, "removePlace");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/place/" + placeId;

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "removePlace result code: " + status.getCode());
                Log.d(TAG, "removePlace result message: " + status.getMessage());
                Log.e(TAG, "removePlace ERROR: " + status.getError());
                if (status.getError() == null) {
                    Log.d(TAG, "No error, continuing deletion.");
                    MainApplication.places.remove(placeId);
                    Toast.makeText(context, context.getString(R.string.place_removed), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.delete(url, String.class, cb);
    }

    public static void renamePlace(final Context context, final String placeId,
                                   final String newName) throws JSONException {
        Log.d(TAG, "renamePlace");
        AQuery aq = new AQuery(context);

        String userId = PreferenceUtils.getString(context, PreferenceUtils.KEY_USER_ID);
        String authorizationToken = PreferenceUtils.getString(context, PreferenceUtils.KEY_AUTH_TOKEN);
        String url = ApiUrl + "user/" + userId + "/place/" + placeId;


        String cleanName = newName.substring(0, 1).toUpperCase() + newName.substring(1).toLowerCase();

        // Get place info
        if (MainApplication.places == null) { // Read them from cache
            if (PreferenceUtils.getString(context, PreferenceUtils.KEY_PLACES).isEmpty()) {
                return;
            }
            MainApplication.places = new JSONObject(PreferenceUtils.
                    getString(context, PreferenceUtils.KEY_PLACES));
        }
        JSONObject placeObj = MainApplication.places.getJSONObject(placeId);

        JSONObject JSONdata = new JSONObject()
                .put("lat", placeObj.getString("lat"))
                .put("lon", placeObj.getString("lon"))
                .put("rad", placeObj.getString("rad"))
                .put("img", placeObj.getString("img"))
                .put("name", cleanName);

        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                Log.d(TAG, "renamePlace result code: " + status.getCode());
                Log.d(TAG, "renamePlace result message: " + status.getMessage());
                Log.e(TAG, "renamePlace ERROR: " + status.getError());

                if (status.getError() == null) {
                    Log.d(TAG, "No error, place renamed.");
                    DataService.getPlaces(context);
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    Toast.makeText(context, R.string.place_renamed, Toast.LENGTH_SHORT).show();
                }
            }
        };

        cb.header("authorizationtoken", authorizationToken);
        aq.put(url, JSONdata, String.class, cb);
    }


    // For dependency injection
    public static void setApiUrl(String mockUrl) {
        ApiUrl = mockUrl;
    }

}


