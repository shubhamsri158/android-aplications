package com.example.digitalgreen;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.MediaRouteButton;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//TODO: Route dismiss on Marker change

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private final int LOCATION_REQUEST_CODE = 100;
    Context mContext;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private ArrayList<LatLng> mMapPoins = null;
    private Button startTripBtn;
    private Button StopTripBtn;
    private Polyline mPolyline;
    private JSONArray geoCoordinates;
    LocationTrack locationTrack;
    //List<List<HashMap<String, String>>> routes = null;
    // private NavigationMapRoute navigationMapRoute;
    private String TAG = "MapActivity";
    private List<List<HashMap<String, String>>> routes;
    private Button stopTripBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mContext = this;
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapPoins = new ArrayList<>();
        startTripBtn = findViewById(R.id.button);
        startTripBtn.setOnClickListener(this);
        stopTripBtn = findViewById(R.id.stop_trip);
        stopTripBtn.setOnClickListener(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        isLocationEnabled();
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mMapPoins.size() == 2) {
                    mMapPoins.clear();
                    mMap.clear();
                }
                mMapPoins.add(latLng);
                MarkerOptions markerOpt = new MarkerOptions();
                markerOpt.position(latLng);
                if (mMapPoins.size() == 1) {
                    markerOpt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    markerOpt.title("Village");
                    startTripBtn.setVisibility(View.GONE);
                } else {
                    markerOpt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    markerOpt.title("Market");
                }
                markerOpt.draggable(true);
                Marker locationMarker = mMap.addMarker(markerOpt);
                locationMarker.setDraggable(true);
                locationMarker.showInfoWindow();
                if (mMapPoins.size() == 2) {
                    //show route and start button as enabled
                    showRoute();
                }

            }
        });
    }

    private void showRoute() {
        startTripBtn.setVisibility(View.VISIBLE);
        String origin = "origin=" + mMapPoins.get(0).latitude + "," + mMapPoins.get(0).longitude;
        String dest = "destination=" + mMapPoins.get(1).latitude + "," + mMapPoins.get(1).longitude;
        String key = "key="+getResources().getString(R.string.google_maps_key);
        String params = origin + "&" + dest + "&" +key;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+params;
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(new String[]{url});

    }

    private void isLocationEnabled() {
        LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(mContext)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            mContext.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.Cancel, null)
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap == null) {
            mapFragment.getMapAsync(this);
        } else {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        if (mMapPoins != null && startTripBtn != null && mMapPoins.size() != 2) {
            startTripBtn.setVisibility(View.GONE);
        }
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception on download", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.button):
                if (geoCoordinates != null)
                    geoCoordinates = null;
                geoCoordinates = new JSONArray();
                startTripBtn.setVisibility(View.GONE);
                stopTripBtn.setVisibility(View.VISIBLE);
                try {
                    captureCoordinates();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stop_trip:
                //sent geocoordinates to srever and handle its response and show as toast
                locationTrack.stopListener();
                Log.i(TAG,"geoCoordinates : "+geoCoordinates.toString());
                stopTripBtn.setVisibility(View.GONE);
                startTripBtn.setVisibility(View.VISIBLE);
                geoCoordinates = null;
                mMapPoins.clear();
                mMap.clear();
                mapFragment.getMapAsync(this);
        }
    }

    private void captureCoordinates() throws JSONException {
        locationTrack = new LocationTrack(MapsActivity.this);


        if (locationTrack.canGetLocation()) {


            double longitude = locationTrack.getLongitude();
            double latitude = locationTrack.getLatitude();
            JSONObject obj = new JSONObject();
            obj.put("latitude",latitude);
            obj.put("longitude",longitude);
            obj.put("timestamp",locationTrack.getTime());
            geoCoordinates.put(obj);
           // Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
        } else {

            locationTrack.showSettingsAlert();
        }
    }

    public void notifyLocationChanged(Double lat, Double lng, long time) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("latitude",lat);
        obj.put("longitude",lng);
        obj.put("timestamp",time);
        geoCoordinates.put(obj);
    }




    /**
     * A class to download data from Google Directions URL
     */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("DownloadTask", "DownloadTask : " + data);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Directions in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;


            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.BLUE);
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                if (mPolyline != null) {
                    mPolyline.remove();
                }
                mPolyline = mMap.addPolyline(lineOptions);

            } else
                Toast.makeText(getApplicationContext(), "No route is found", Toast.LENGTH_LONG).show();
        }
    }
}







