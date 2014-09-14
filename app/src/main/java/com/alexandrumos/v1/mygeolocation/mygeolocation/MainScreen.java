package com.alexandrumos.v1.mygeolocation.mygeolocation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainScreen extends Activity implements LocationListener {

    private static final String TAG = "MainScreen";
    MapView map;
    float displayHeightInDp, displayWidthInDp;
    double latitude, longitude, altitude, accuracy, detLat, detLon;
    String address;
    TextView textLatitude, textLongitude, textAltitude, textAccuracy, textAddress, splashText;
    Button splashCheckAgain;
    Menu abMenu;
    LocationManager locationManager;
    MapController mapController;
    Integer locationUpdateInterval = 250; // 0.25s
    Integer locationUpdateCounter = 0;
    Integer locationUpdateLimit = 4;
    Boolean firstDetection = true;
    Boolean initComplete = false;
    Boolean internetDetected = false;
    Boolean locationDetectionInProgress = false;
    Integer screenOrientation = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // to permit HTTP requests from main thread (I know isn't nice...)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        screenOrientation = getScreenOrientation();

        if (initComplete.equals(false)) {
            initSplashScreen();
        } else {
            initMainScreen();
            updateMainLayoutView();
        }
    }

    // shows the splash layout and starts the internet connection test
    public void initSplashScreen() {
        setContentView(R.layout.entry);

        splashText = (TextView) findViewById(R.id.spashText);
        splashText.setText(R.string.text_splash_check_connection);

        splashCheckAgain = (Button) findViewById(R.id.buttonCheckAgain);

        if (internetDetected.equals(true)) {
            // we know that internet is accessible
            splashFirstDetection();
        } else {
            // adding a delay in order to have the layout drawn and check label displayed
            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        checkInternetConnectivity();
                    }
                },
            700);
        }
    }

    public void checkInternetConnectivity() {
        if (checkInternetConnection()) {
            // the Google API request and respons was successfully sent and received
            internetDetected = true;
            splashFirstDetection();
        } else {
            // showing the internet connection check retry button
            splashText.setVisibility(View.GONE);
            splashCheckAgain.setVisibility(View.VISIBLE);
        }
    }

    // handles the action: click on the retry button
    public void buttonCheckInternetAgain(View v) {
        checkInternetConnectivity();
    }

    // shows the location waiting message and starts the location manager update request
    public void splashFirstDetection() {
        splashText.setText(R.string.text_splash_obtaining_loc);

        // places the location manager update request
        getMyLocation();
    }

    // returns the layout type (portrait or landscape)
    private int detectMainLayoutType() {
        switch (screenOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return R.layout.main_portrait;
            case Configuration.ORIENTATION_LANDSCAPE:
                return R.layout.main_landscape;
            default:
                return R.layout.main_portrait;
        }
    }

    // handles the marker icon into the action bar (semi-transp or default)
    public void setObtainAddressIconStatus(Boolean status) {

        MenuItem marker = abMenu.getItem(0);
        int iconId;

        if (status.equals(true)) {
            iconId = R.drawable.ic_fa_map_marker;
        } else {
            iconId = R.drawable.ic_fa_map_marker_transp;
        }

        marker.setIcon(getResources().getDrawable(iconId));
    }

    // initializes the layout for main screen
    public void initMainScreen() {
        setContentView(detectMainLayoutType());

        textLatitude = (TextView) findViewById(R.id.textLatitude);
        textLongitude = (TextView) findViewById(R.id.textLongitude);
        textAccuracy = (TextView) findViewById(R.id.textAccuracy);

        textAddress = (TextView) findViewById(R.id.textAddress);

        retrieveDisplaySizeInDp();

        map = (MapView) findViewById (R.id.mapview);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setBuiltInZoomControls(true);
        map.canZoomIn();
        map.canZoomOut();
        map.setUseDataConnection(true);

        mapController = (MapController) map.getController();
        mapController.setZoom(22);
    }

    // does a request to Google Geocode API to obtain the address for the detected latitude and longitude
    public String reverseGeocodeLocation(double lat, double lng) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + String.format("%.7f", lat) + "," + String.format("%.7f", lng);
        String address = "";
        String response = HttpRequest.get(url).body();

        try {
            JSONObject obj = new JSONObject(response);
            String status = obj.getString("status");

            if (status.equals("OK")) {
                JSONObject result = obj.getJSONArray("results").getJSONObject(0);
                address = result.getString("formatted_address");
            } else {
                if (status.equals("ZERO_RESULTS")) {
                    address = getString(R.string.text_main_no_reverse_geocode_results);
                } else {
                    String errorToastMessage = null;
                    if (status.equals("OVER_QUERY_LIMIT")) {
                        errorToastMessage = "You exceeded the reverse geocoding query limit for today. Please try again after a few hours.";
                    }

                    if (status.equals("REQUEST_DENIED")) {
                        errorToastMessage = "Reverse geocoding request was denied.";
                    }

                    if (status.equals("INVALID_REQUEST")) {
                        errorToastMessage = "Reverse geocoding request is invalid.";
                    }

                    if (status.equals("UNKNOWN_ERROR")) {
                        errorToastMessage = "Unknown error trying to reverse geocode the current address. Please try again after a few moments.";
                    }

                    Toast.makeText(getApplicationContext(), errorToastMessage + " (" + status + ")", Toast.LENGTH_LONG).show();
                }

            }

        } catch (Exception e) {
            Log.v(TAG, "reverseGeocodeLocation exception: " + e.getMessage());
        }

        return address;
    }

    // not used for now...
    public void retrieveDisplaySizeInDp() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = getResources().getDisplayMetrics().density;
        displayHeightInDp = outMetrics.heightPixels / density;
        displayWidthInDp  = outMetrics.widthPixels / density;

        Log.v("MainScreen", "H:" + displayHeightInDp + "dp W:" + displayWidthInDp + "dp");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_screen, menu);
        abMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (initComplete.equals(true)) {
            // handling the action bar icons select only into the main screen, not in splash screen too
            switch (item.getItemId()) {
                // about icon was pressed
                case R.id.action_about:
                    showAboutScreen();
                    break;

                // detection icon was pressed
                case R.id.action_locate:
                    Log.v(TAG, "action_locate");
                    if (locationDetectionInProgress.equals(false)) {
                        getMyLocation();
                    }

                    break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // starts the about screen intent
    public void showAboutScreen() {
        Intent intent = new Intent(this, AboutScreen.class);
        startActivity(intent);
    }

    // starts the location listener request and handles the layout
    public void getMyLocation() {
        if (firstDetection.equals(false)) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_locating), Toast.LENGTH_LONG).show();
        }

        locationDetectionInProgress = true;

        setObtainAddressIconStatus(false);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdateInterval, 0, this);
    }

    // location update received
    @Override
    public void onLocationChanged(Location location) {
        if (locationUpdateCounter <= locationUpdateLimit) {
            locationUpdateCounter += 1;
        } else {
            if (firstDetection.equals(true)) {
                initComplete = true;
                firstDetection = false;

                initMainScreen();
            }

            locationManager.removeUpdates(this);
            locationUpdateCounter = 0;

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            altitude = location.getAltitude();
            accuracy = location.hasAccuracy() ? location.getAccuracy() : 0.0;

            detLat = latitude;
            detLon = longitude;

            address = reverseGeocodeLocation(latitude, longitude);

            updateMainLayoutView();
            setObtainAddressIconStatus(true);

            locationDetectionInProgress = false;
        }
    }

    private void updateMainLayoutView() {
        map.getOverlays().clear();

        textLatitude.setText(String.format("%.6f", latitude));
        textLongitude.setText(String.format("%.6f", longitude));
        textAccuracy.setText(String.format("%.0f", accuracy) + "m");

        textAddress.setText(address);

        GeoPoint currentLoc = new GeoPoint(latitude, longitude);

        ArrayList<OverlayItem> anotherOverlayItemArray = new ArrayList<OverlayItem>();

        OverlayItem marker = new OverlayItem("", "", currentLoc);
        marker.setMarker(getResources().getDrawable(R.drawable.marker));
        anotherOverlayItemArray.add(marker);

        ItemizedIconOverlay<OverlayItem> anotherItemizedIconOverlay = new ItemizedIconOverlay<OverlayItem>(this, anotherOverlayItemArray, null);
        map.getOverlays().add(anotherItemizedIconOverlay);

        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    GeoPoint currentLoc = new GeoPoint(latitude, longitude);
                    mapController.setCenter(currentLoc);
                }
            },
        100);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");

        if (initComplete.equals(true)) {
            updateMainLayoutView();
            setObtainAddressIconStatus(true);

            locationDetectionInProgress = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        Log.v(TAG, "onPause");
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private int getScreenOrientation() {
        return getResources().getConfiguration().orientation;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        try {
            IGeoPoint center = map.getMapCenter();
            latitude = center.getLatitude();
            longitude = center.getLongitude();
        } catch (Exception e) {
            // ignoring this
        }

        screenOrientation = getScreenOrientation();

        if (initComplete.equals(true)) {
            initMainScreen();
            updateMainLayoutView();
        }
    }

    public Boolean checkInternetConnection() {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL("http://www.google.com").openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            //setContentView(R.layout.error_screen);
            return false;
        }
    }
}