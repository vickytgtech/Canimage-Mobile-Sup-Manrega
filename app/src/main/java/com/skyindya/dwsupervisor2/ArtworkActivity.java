package com.skyindya.dwsupervisor2;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ArtworkActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    TableLayout printTable;
    DatabaseHelper db;
    Activity act;
    JSONArray jarr;
    int len = 0;
    String code = "";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork);
        act = this;
        printTable = (TableLayout) findViewById(R.id.printTable);

        try {
            androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            ImageView imageView = new ImageView(actionBar.getThemedContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(R.drawable.actionlogo);
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT, Gravity.LEFT
                    | Gravity.CENTER_VERTICAL);
            layoutParams.rightMargin = 40;
            imageView.setLayoutParams(layoutParams);
            actionBar.setCustomView(imageView);
            code = getIntent().getStringExtra("code");

            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionsToRequest = permissionsToRequest(permissions);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(
                            new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
                }
            }

            // we build google api client
            googleApiClient = new GoogleApiClient.Builder(this).
                    addApi(LocationServices.API).
                    addConnectionCallbacks(this).
                    addOnConnectionFailedListener(this).build();
            displayData();
//            getLocation();

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        } catch (Exception e) {
            saveErrorFile("onCreate ArtworkActivity : " + e.getMessage());
            e.printStackTrace();
            try {
                uploadException(e.getMessage(), "onCreate", "DWP S ArtworkActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
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
            Toast.makeText(act, "latitude = " + latitude + "  longitude = " + longitude, Toast.LENGTH_SHORT).show();
        }

        startLocationUpdates();
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
//            Toast.makeText(act, ""+latitude+","+longitude, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
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

    private void displayData(){
        try {
        jarr = new JSONArray();
        db = new DatabaseHelper(act);
        Cursor cs = db.viewPrints(code);
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
                jarr.put(jo);
            }
        } else {
            Toast.makeText(act, "No Prints.", Toast.LENGTH_SHORT).show();
        }
        cs.close();
        db.close();
        len = jarr.length();
        setTable();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("displayData ArtworkActivity : " + e.getMessage());
                uploadException(e.getMessage(), "displayData", "DWP S ArtworkActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void setTable() {
        while (printTable.getChildCount() > 1)
            printTable.removeView(printTable.getChildAt(printTable.getChildCount() - 1));

        try {
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    final JSONObject jo = jarr.getJSONObject(i);
                    final TableRow tr = new TableRow(act);
                    TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                    tp.weight = 1;

                    TextView tvArtwork = new TextView(act);
                    TextView tvPrintNo = new TextView(act);
                    TextView tvSQFT = new TextView(act);
                    TextView tvRemarks = new TextView(act);

                    tvArtwork.setText(jo.getString("brand") + "\n" + jo.getString("Width") + "x" + jo.getString("Height") + "\n" + jo.getString("language"));
                    if (jo.getString("Printno").equals("")) {
                        tvPrintNo.setText("---");
                        tvRemarks.setText(jo.getString("Remarks"));
                    } else {
                        tvPrintNo.setText(jo.getString("Printno"));
                        tvRemarks.setText("---");
                        tr.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
//                                    getLocation();
                                    Location locationA = new Location("previous");
                                    locationA.setLatitude(latitude);
                                    locationA.setLongitude(longitude);
                                    Location locationB = new Location("current");
                                    locationB.setLatitude(jo.getDouble("Latitude"));
                                    locationB.setLongitude(jo.getDouble("Longitude"));
                                    float distance = locationA.distanceTo(locationB);
                                    Log.d("distance = ", "" + distance);
//                                    if (distance <= 100) {
                                        Intent i = new Intent(act, ExecuteActivity.class);
                                        i.putExtra("json", jo.toString());
                                        startActivity(i);
//                                    } else {
//                                        Toast.makeText(act, "Location does not match!", Toast.LENGTH_SHORT).show();
//                                    }
                                } catch (Exception e) {
                                    saveErrorFile("tr.setOnClickListener setTable ArtworkActivity : " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    tvSQFT.setText(jo.getString("Sqft"));

                    Common.setRow(tp, tvArtwork, tr, act);
                    Common.setRow(tp, tvPrintNo, tr, act);
                    Common.setRow(tp, tvSQFT, tr, act);
                    Common.setRow(tp, tvRemarks, tr, act);

                    printTable.addView(tr);
                }
            } else {
                final TableRow tr = new TableRow(act);
                TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                tp.weight = 1;
                TextView tvID = new TextView(act);
                tvID.setText("No Data.");
                Common.setRow(tp, tvID, tr, act);
                printTable.addView(tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("setTable ArtworkActivity : " + e.getMessage());
                uploadException(e.getMessage(), "setTable", "DWP S ArtworkActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
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
            saveErrorFile("showSettingsAlert ArtworkActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (isMyServiceRunning(new MyService().getClass()))
//        {
//            Log.d("My service ","running");
//        }
//        else
//        {
//            startService(new Intent(this, MyService.class));
//            Log.d("My service "," not running");
//        }
        if (!code.equals("")) {
            try {
                displayData();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private void uploadException(final String message, final String fname, final String executeActivity) {
        try {
            if (new Common().isNetworkAvailable(act)) {
                try {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, Common.site + "CatchException",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Log.d("CatchException response", "" + response);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Log.d("exception occured", "" + e.toString());
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                    Log.d("error:->", "error Occured" + error.getMessage());
                                    try {
                                        Toast.makeText(act, "Network error while uploading exception." + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    } catch (NullPointerException e) {
                                        Log.d("error:->", "NullPointerException " + error.getMessage());
                                    }
                                    finish();
                                }
                            }) {
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("Exception", message);
                            map.put("FunctionName", fname);
                            map.put("PageName", executeActivity);
                            return map;
                        }
                    };
                    VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
