package com.skyindya.dwsupervisor2;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback , GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    GPSTracker gps;
    Activity act;
    List<Marker> markerList = new ArrayList<>();
    List<String> idList = new ArrayList<>();
    ///new Distance Logic for distance
    List<Double> distanceList = new ArrayList<>();


    int len;
    JSONArray jarr;
    DatabaseHelper db;
    protected LocationManager locationManager;
    // flag for GPS status
    boolean isGPSEnabled = false;
    // flag for network status
    boolean isNetworkEnabled = false;
    // flag for GPS status
    boolean canGetLocation = false;
    Location location; // location
    double latitude; // latitude
    double longitude; // longitude
    boolean isFound = false;
    Button srchBtn;
    EditText printNo;

    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 10000, FASTEST_INTERVAL = 10000; // = 10 seconds
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    private ProgressDialog pd;
    Integer Distance=50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        act = this;
        pd = new ProgressDialog(act, R.style.AppTheme_Dark_Dialog);
        getDistance();
        Log.d("Brahma", "onCreate: ");

        try {
            printNo = (EditText) findViewById(R.id.printNo);
            srchBtn = (Button) findViewById(R.id.srchBtn);

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionsToRequest = permissionsToRequest(permissions);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(
                            new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
                }
            }

            srchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
//                        mp.start();
                        if (jarr != null && jarr.length() > 0) {
                            boolean isPresent = false;
                            for (int a = 0; a < len; a++) {
                                try {
                                    JSONObject vjo = jarr.optJSONObject(a);

                                    if (vjo.optString("Printno").equalsIgnoreCase(printNo.getText().toString())) {
                                        isPresent = true;
                                        markerList.get(a).showInfoWindow();
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(vjo.getDouble("Latitude"), vjo.getDouble("Longitude")), 15));
                                        mMap.animateCamera(CameraUpdateFactory.zoomIn());
                                        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                                        break;
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (!isPresent) {
                                Toast.makeText(act, "Enter valid Print no.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        saveErrorFile("MapsActivity error oncreate : "+e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // we build google api client
            googleApiClient = new GoogleApiClient.Builder(this).
                    addApi(LocationServices.API).
                    addConnectionCallbacks(this).
                    addOnConnectionFailedListener(this).build();
        } catch (Exception e) {
            saveErrorFile("onCreate MapsActivity : " + e.getMessage());
            e.printStackTrace();
        }

    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.d("Brahma", "onConnected: " +latitude+":"+longitude);
            Toast.makeText(act, "latitude = " + latitude + "  longitude = " + longitude, Toast.LENGTH_SHORT).show();
            if(mMap !=null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 18.0f));
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        }

        startLocationUpdates();
//            add by brahma

        setMarkers();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            if(mMap !=null && !isFound) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 18.0f));
                isFound = true;
            }
//            Toast.makeText(act, ""+latitude+","+longitude, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            if (googleApiClient != null) {
                googleApiClient.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (googleApiClient != null  &&  googleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                googleApiClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        stopService(new Intent(this, MyService.class));
    }

    private void getJson() {
        try {
            jarr = new JSONArray();
            db = new DatabaseHelper(act);

//            commented by brahma 14-12-2022
//            Cursor cs = db.viewPrints("");

            Cursor cs = db.viewPrintsForSupervisor("");



            if (cs.getCount() > 0) {

                while (cs.moveToNext()) {
                    JSONObject jo = new JSONObject();
                    jo.put("localid", cs.getString(0));
                    jo.put("id", cs.getString(1));
                    jo.put("PlanID", cs.getString(2));
                    jo.put("VillageCode", cs.getString(3));
                    jo.put("brand", cs.getString(4));
                    jo.put("Width", cs.getString(5));
                    jo.put("Height", cs.getString(6));
                    jo.put("Sqft", cs.getString(7));
                    jo.put("language", cs.getString(8));
                    jo.put("Printno", cs.getString(9));
                    jo.put("Latitude", cs.getString(10));
                    jo.put("Longitude", cs.getString(11));
                    jo.put("Address", cs.getString(12));
                    jo.put("Remarks", cs.getString(14));
                    jo.put("projectcode", cs.getString(15));
                    jo.put("villagename", cs.getString(16));
//                    if (db.verifyVillage(cs.getString(3)) > 0) {
//                        if (!cs.getString(9).equals(""))
                            jarr.put(jo);
//                    }
                }
            } else {
                Toast.makeText(act, "No Prints.", Toast.LENGTH_SHORT).show();
            }
            cs.close();
            db.close();
            Log.d("Brahma", "getJson: "+jarr.toString());
            len = jarr.length();
        }catch (Exception e)
        {
            saveErrorFile("getJson MapsActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // getting GPS status
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Log.d("bestProvider",bestProvider);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return location;
//            }
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, this);
            // getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled) {
                // no network provider is enabled
                showSettingsAlert();
            }
            this.canGetLocation = true;
            // First get location from Network Provider
//                if (isNetworkEnabled) {
            try {

                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(bestProvider);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
//                        Toast.makeText(act, "latitude = "+latitude+"  longitude = "+longitude, Toast.LENGTH_SHORT).show();
                    }
                    else if(location == null)
                    {
                        Log.d("Network", "Network");
                        bestProvider = locationManager.NETWORK_PROVIDER;
                        location = locationManager.getLastKnownLocation(bestProvider);
                        if(location != null)
                        {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
//                            Toast.makeText(act, "latitude = "+latitude+"  longitude = "+longitude, Toast.LENGTH_SHORT).show();
                        }
                    }
                    Log.d("Brahma", "getLocation: "+latitude+""+longitude);
                    Toast.makeText(act, "latitude = "+latitude+"  longitude = "+longitude, Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
//                }
            // if GPS Enabled get lat/long using GPS Services
//            if (isGPSEnabled) {
//                if (location == null) {
//                    try {
//                        Log.d("GPS Enabled", "GPS Enabled");
//                        if (locationManager != null) {
//                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                            if (location != null) {
//                                latitude = location.getLatitude();
//                                longitude = location.getLongitude();
//                            }
//                        }
//                    } catch (SecurityException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSettingsAlert() {
        try {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(act);

            // Setting Dialog Title
            alertDialog.setTitle("GPS is settings");

            // Setting Dialog Message
            alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

            // On pressing Settings button
            alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alertDialog.show();
        } catch (Exception e) {
            saveErrorFile("showSettingsAlert MapsActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyServiceRunning(new MyService().getClass()))
        {
            Log.d("My service ","running");
        }
        else
        {
            startService(new Intent(this, MyService.class));
            Log.d("My service "," not running");
        }
        getJson();
        if(mMap != null)
        {
            mMap.clear();
            markerList.clear();
            idList.clear();
            distanceList.clear();

//            commented by brahma
//            setMarkers();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }

        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
//            getLocation();
            mMap = googleMap;

            //added by brahma 01-12-2022

            if(mMap!=null)
            {
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View row=getLayoutInflater().inflate(R.layout.custom_mapdetails,null);
                        TextView txtTitle=(TextView)row.findViewById(R.id.maptitle);
                        TextView txtPrintNo=(TextView)row.findViewById(R.id.AllInfo);

                        txtTitle.setText(marker.getTitle());
                        txtPrintNo.setText(marker.getSnippet());

                        return row;
                    }
                });

            }


            // Add a marker in Sydney and move the camera
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 18.0f));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
//            commented by brahma
//            setMarkers();

        } catch (Exception e) {
            saveErrorFile("onMapReady MapsActivity : " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void setMarkers() {
        try {
            DecimalFormat format = new DecimalFormat("0.00");

//            gps = new GPSTracker(act);
            Log.d("Brahma ", "latittude " +latitude);
            Log.d("Brahma ", "longitude" +longitude);
            Log.d("Brahma", "finalDist: "+Distance);
            for (int i = 0; i < len; i++) {
                JSONObject jo = jarr.getJSONObject(i);
                Log.d("json   = ", "" + jo.toString());
                //added by brahma
                Log.d("Brahma ", "longitude" +jo.getDouble("Latitude")+" "+jo.getDouble("Longitude"));

                Log.d("Brahma", "setMarkers: "+ distance(latitude,longitude,jo.getDouble("Latitude"),jo.getDouble("Longitude")));
                double distLatLong=distance(latitude,longitude,jo.getDouble("Latitude"),jo.getDouble("Longitude"));
                if(distLatLong<Distance)
                {
                    idList.add(jo.getString("id"));
                    distanceList.add(distLatLong);
                    markerList.add(mMap.addMarker(new MarkerOptions().position(new LatLng(jo.getDouble("Latitude"), jo.getDouble("Longitude")))
                            .title("Print No : " + jo.getString("Printno"))
                            .snippet(
                            "Lat-Long:" + jo.getString("Latitude")+"," + jo.getString("Longitude")+"\n"+
                            "Village Name : "+jo.getString("villagename")+"\n"+
                            "Distance : "+ format.format(distLatLong) + "Km \n"+
                            "Size: "+ format.format(jo.getDouble("Width")) + "W x" + format.format(jo.getDouble("Height"))+"H \n"
                            +"Artwork Name : "+jo.getString("brand")+" \n"

                            )
                    ));

//                    Log.d("Brahma", "setMarkers: "+jo.getString("Address").replace(",,",","));
                }



            }
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    try {
                        boolean isSame = false;

                        //code changes---

                       // Intent i = new Intent(act, ExecuteActivity.class);
                        Intent i = new Intent(act, ViewPostReccaImg.class);

                        String id = "";
                        double distanceMarker = 0;
                        for (int m = 0; m < len; m++) {
                            Log.d("inside ", " m = " + m);
                            if (marker.equals(markerList.get(m))) {
                                Log.d("inside ", " equal marker");
                                id = idList.get(m);
                                distanceMarker=distanceList.get(m);

                                Log.d("Brahma", "Distance: "+distanceMarker);
                                Log.d("inside ", " equal marker id = " + id);
                                break;
                            }
                        }
                        Log.d("inside ", " id = " + id);
                        for (int n = 0; n < len; n++) {
                            JSONObject jo = jarr.getJSONObject(n);
                            if (id == jo.getString("id")) {
//                                getLocation();
                                i.putExtra("json", jo.toString());
                                Location locationA = new Location("previous");
                                locationA.setLatitude(latitude);
                                locationA.setLongitude(longitude);
                                Location locationB = new Location("current");
                                locationB.setLatitude(jo.getDouble("Latitude"));
                                locationB.setLongitude(jo.getDouble("Longitude"));
                                float distance = locationA.distanceTo(locationB);
                                Log.d("distance = ", "" + distance);
//                                if (distance <= 100) {
                                    isSame = true;
//                                }
                                break;
                            }
                        }
                        if (isSame)
                        {
                            DecimalFormat format = new DecimalFormat("0.00");
                            i.putExtra("distanceMarker", format.format(distanceMarker*1000.00));
//                            Log.d("Brahma", "Distance:After start "+format.format(distanceMarker*1000.00));
                            startActivity(i);
                        }

                        else
                            Toast.makeText(act, "Location does not match!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        saveErrorFile("setOnInfoWindowClickListener setMarkers MapsActivity : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });



        }
        catch (Exception e)
        {
            saveErrorFile("setMarkers MapsActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveErrorFile(String er) {
        try {
            File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/DWPSError.txt");
//            if (myDir.exists()) {
//
//                if (myDir.delete()) {
//                    Log.d("inside myDir", "deleted");
//                }
//            }

            FileWriter fw;
            BufferedWriter bw = null;
            String content = er.toString();
            Log.d("content", "$$$$$$$" + content);
            fw = null;
            fw = new FileWriter(myDir, true);
            bw = new BufferedWriter(fw);
            bw.write("\n" + Calendar.getInstance().getTime() + " -- " + content);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    created by brahma 02-11-2022

    private double distance(double lat1, double lon1, double lat2, double lon2) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515 * 1.60934;
        //in km
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private double milesToKms (double miles) {
        return miles * 1.60934;
    }


    public void getDistance() {

        if (new Common().isNetworkAvailable(act)) {

            try {
                pd.setMessage("Wait...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "GetDistance",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    pd.dismiss();
                                    Log.d("Brahma", "" + response.trim());
                                    JSONArray ja = new JSONArray(response.trim());
                                    Log.d("ja ----", "" + ja.toString());
                                    if (ja.getJSONObject(0).getString("Response").equalsIgnoreCase("Success")) {
                                        Distance = Integer.parseInt(ja.getJSONObject(0).getString("Distance"));
                                    }

                                } catch (Exception e) {
                                    saveErrorFile("onResponse getDistance MapsActivity : " + e.getMessage());
                                    e.printStackTrace();
                                    Log.d("exception occured", "" + e.toString());
                                }
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                pd.dismiss();
                                Log.d("error:->", "error Occured" + error.getMessage());
                                try {
                                    saveErrorFile("onErrorResponse getDistance getImageUrl : " + error.getMessage());
                                    Toast.makeText(act, "Network error.", Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {

                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
//
            } catch (Exception e) {
                saveErrorFile("getDistance MapsActivity : " + e.getMessage());
                e.printStackTrace();
                Log.d("Brahma", "Exception: " + e);

            }
        } else {

            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();

        }
    }



}
