package com.skyindya.dwsupervisor2;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.javiersantos.appupdater.AppUpdater;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Button submit, help, seePlans, vendorplans;
    Activity act;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private GPSTracker gps;
    private Common cmn;
    private ProgressDialog pd;
    JSONArray syncArray, uploadArray, fileArray;
    private DatabaseHelper db;
    SharedPreferences checkuser;
    SharedPreferences.Editor editor;
    int count = 0, position = 0, total = 0;
    MediaPlayer mp;

    String imei ="";
    static final private String ALPHABET = "0123456789";
    final private Random rng = new SecureRandom();
    String uniqueId;
    String IMAGE_DIRECTORY_NAME = "/DWPainting";

    protected LocationManager locationManager;
    // flag for GPS status
    boolean isGPSEnabled = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sync) {

            try {
                mp.start();
                Intent i = new Intent(act,SyncActivity.class);
                startActivity(i);
//            dbToJson();
            } catch (Exception e) {
                saveErrorFile("onOptionsItemSelected MainActivity : " + e.getMessage());
                e.printStackTrace();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        act = this;
        pd = new ProgressDialog(act, R.style.AppTheme_Dark_Dialog);
        try {
            mp = MediaPlayer.create(act, R.raw.tone);
            AppUpdater appUpdater = new AppUpdater(this).setButtonDoNotShowAgain(null);
            appUpdater.start();
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
            checkuser = getSharedPreferences("checkuser", Context.MODE_PRIVATE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//        }
            if (this.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
                    && this.checkCallingOrSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED
                    && this.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                // All Permissions Granted
                initializeVars();
            } else {
                getPermissions();
            }

        } catch (Exception e) {
            saveErrorFile("onCreate MainActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showSettingsAlert() {
        try {
            Log.i("inside","showSettingsAlert");
            android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(act);

            // Setting Dialog Title
            alertDialog.setTitle("GPS settings");

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
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, MyService.class));
        if (isMyServiceRunning(new MyService().getClass())) {
            Log.d("My service ", "running");
        } else {
            Log.d("My service ", " not running");
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initializeVars() {
        try {
            submit = (Button) findViewById(R.id.submit);
            help = (Button) findViewById(R.id.help);
            seePlans = (Button) findViewById(R.id.seePlans);
            vendorplans = (Button) findViewById(R.id.vendorplans);
            gps = new GPSTracker(act);
            cmn = new Common();
            Log.d("latittude ", "" + gps.getLatitude());
            Log.d("longitude ", "" + gps.getLongitude());
            Log.d("imei  ", "" + Common.getImei(act));

            imei = Common.getImei(act);

            if (checkuser.getBoolean("isVerified", false)) {
                submit.setEnabled(true);
                seePlans.setEnabled(true);
                vendorplans.setEnabled(true);
            } else
                verifyUser();

            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mp.start();
                    Intent i = new Intent(act, MapsActivity.class);
                    startActivity(i);
                }
            });
            seePlans.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mp.start();
                    Intent i = new Intent(act, PendingListActivity.class);
                    startActivity(i);
                }
            });

            vendorplans.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mp.start();
                    Intent i = new Intent(act, VendorUserPendingPlans.class);
                    startActivity(i);
                }
            });

            help.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mp.start();
                    Intent i = new Intent(act, HelpActivity.class);
                    startActivity(i);
                }
            });
            try {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String bestProvider = locationManager.getBestProvider(criteria, true);
                Log.d("bestProvider", bestProvider);
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                Log.i("isGPSEnabled",""+isGPSEnabled);
                Log.d("isGPSEnabled",""+isGPSEnabled);
                if (!isGPSEnabled) {
                    Log.i("inside","isGPSEnabled");
                    // no network provider is enabled
                    showSettingsAlert();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            saveErrorFile("initializeVars MainActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void getPermissions() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("Read or write External storage");
        if (!addPermission(permissionsList, android.Manifest.permission.CAMERA))
            permissionsNeeded.add("Access Camera");
        if (!addPermission(permissionsList, android.Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("Access Location");
        if (!addPermission(permissionsList, android.Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionsNeeded.add("Access Location");
        if (!addPermission(permissionsList, android.Manifest.permission.READ_PHONE_STATE))
            permissionsNeeded.add("Access Phone State");
        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

            return;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(act)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initializeVars();
                } else {
                    // Permission Denied
                    Toast.makeText(act, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void showWrongIMEI() {
        try {
            Log.d("Brahma", "showWrongIMEI: " + imei);
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(act);
            alertDialogBuilder.setMessage("You are not registered.Please contact to your Ad Agency Team"+" "+imei)
                    .setCancelable(false);
            alertDialogBuilder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();
                        }
                    });
            android.app.AlertDialog a = alertDialogBuilder.create();
            a.show();
        } catch (Exception e) {
            saveErrorFile("showWrongIMEI MainActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void verifyUser() {
        if (new Common().isNetworkAvailable(act)) {

            try {
                pd.setMessage("Wait...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "ValidateImei?vImeiNumber=" + Common.getImei(act),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    pd.dismiss();
                                    Log.d("response ----", "" + response.trim());
                                    JSONArray ja = new JSONArray(response.trim());
                                    Log.d("ja ----", "" + ja.toString());
                                    if (ja.getJSONObject(0).getString("Response").equalsIgnoreCase("Success")) {
                                        submit.setEnabled(true);
                                        seePlans.setEnabled(true);
                                        vendorplans.setEnabled(true);
                                        editor = checkuser.edit();
                                        editor.putBoolean("isVerified", true);
                                        editor.commit();
                                    } else {
                                        showWrongIMEI();
                                    }

                                } catch (Exception e) {
                                    saveErrorFile("onResponse verifyUser MainActivity : " + e.getMessage());
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
                                    saveErrorFile("onErrorResponse verifyUser MainActivity : " + error.getMessage());
                                    Toast.makeText(act, "Network error.", Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {

                        Map<String, String> map = new HashMap<String, String>();
                        map.put("vImeiNumber", Common.getImei(act));
    //                    map.put("vImeiNumber", "464BVV66sbbd665");
                        return map;
                    }
                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
//            if (Common.getImei(act).equals(IMEI)) {
//
//            } else {
//                showWrongIMEI();
//            }
            } catch (Exception e) {
                saveErrorFile("verifyUser MainActivity : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    public void readFile() {
        try {
            String jsonStr = "";

            File readFile = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/HM.txt");
            if (readFile.exists()) {
                Log.d("inside", "file Exist");
                //Read file
                FileInputStream stream = new FileInputStream(readFile);
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

                jsonStr = Charset.defaultCharset().decode(bb).toString();
                Log.d("jsonstr", "*******" + jsonStr);
                stream.close();
                StringBuilder text = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(readFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                    }
                    br.close();
                    syncArray = new JSONArray(text.toString());
                } catch (Exception e) {
                    saveErrorFile("readFile MainActivity : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            saveErrorFile("readFile MainActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveFile(JSONArray arr) {
        try {
            File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/HM.txt");
            if (myDir.exists()) {

                if (myDir.delete()) {
                    Log.d("inside myDir", "deleted");
                }
            }

            FileWriter fw;
            BufferedWriter bw = null;
            String content = arr.toString();
            Log.d("content", "$$$$$$$" + content);
            fw = null;
            fw = new FileWriter(myDir);
            bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dbToJson() {
        try {
            String localID = "";
            db = new DatabaseHelper(act);
            syncArray = new JSONArray();
            fileArray = new JSONArray();
            Cursor cs = db.viewData("");
            count = cs.getCount();
            total = cs.getCount();
            Log.d("count data = ", "" + count);
            if (cs.getCount() > 0) {
                while (cs.moveToNext()) {
                    JSONObject jo = new JSONObject();
                    localID = cs.getString(0);
                    jo.put("localid", cs.getString(0));
                    jo.put("id", cs.getString(1));
                    jo.put("Planid", cs.getString(2));
                    jo.put("VillageCode", cs.getString(3));
                    jo.put("Remark", cs.getString(4));
                    jo.put("NearImage", cs.getString(5));
                    jo.put("FarImage", cs.getString(6));
                    jo.put("ExecutionDate", cs.getString(10));
                    jo.put("UploadDate", "" + Common.getDate());
                    jo.put("Printno", "" + cs.getString(11));
                    syncArray.put(jo);
                }
//                saveFile(syncArray);
                callUpload();
            } else {
                readFile();
                if (syncArray.length() > 0) {
                    total = syncArray.length();
                    callUpload();
                } else {
                    Toast.makeText(act, "No data to sync.", Toast.LENGTH_SHORT).show();
                }
            }
            cs.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callUpload() {
        try {

            count = count - 1;
            String localId = "";
            if (syncArray.length() > 0) {
                JSONObject jo = syncArray.getJSONObject(position);
                uploadArray = new JSONArray();
                JSONObject jo1 = new JSONObject();
                localId = jo.getString("localid");
                Log.d("local id upload = ", localId);
                jo1.put("id", jo.getString("id"));
                jo1.put("Planid", jo.getString("Planid"));
                jo1.put("VillageCode", jo.getString("VillageCode"));
                jo1.put("Remark", jo.getString("Remark"));
                if (jo.getString("NearImage").toLowerCase().contains("DWPainting".toLowerCase()))
                    jo1.put("NearImage", imageToString(jo.getString("NearImage")));
                else
                    jo1.put("NearImage", jo.getString("NearImage"));
                if (jo.getString("FarImage").toLowerCase().contains("DWPainting".toLowerCase()))
                    jo1.put("FarImage", imageToString(jo.getString("FarImage")));
                else
                    jo1.put("FarImage", jo.getString("FarImage"));
                jo1.put("ExecutionDate", jo.getString("ExecutionDate"));
                jo1.put("UploadDate", "" + Common.getDate());
                jo1.put("Printno", "" + jo.getString("Printno"));
                jo1.put("imei", Common.getImei(act));
                uploadArray.put(jo1);
//                syncArray.remove(position);
                Log.d("syncarray length = ", "" + syncArray.length());
                position = position + 1;
                Log.d("uploadarray = ", "" + uploadArray.toString());
                uploadData(localId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("callUpload MainActivity : " + e.getMessage());
                uploadException(e.getMessage(),"callUpload","DWP S MainActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void uploadData(final String localId) {
//        File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/HM.txt");
//        if (myDir.exists()) {
//
//            if (myDir.delete()) {
//                Log.d("inside myDir", "deleted");
//            }
//        }
//        try {
//            FileWriter fw;
//            BufferedWriter bw = null;
//            String content = uploadArray.toString();
//            Log.d("content", "$$$$$$$" + content);
//            fw = null;
//            fw = new FileWriter(myDir);
//            bw = new BufferedWriter(fw);
//            bw.write(content);
//            bw.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        if (new Common().isNetworkAvailable(act)) {
            try {
                pd.setMessage("Uploading entry " + position + " of " + total);
                if (position == 1) {
                    pd.setIndeterminate(true);
                    pd.show();
                    pd.setCancelable(false);
                }
                final JSONObject chkObj = uploadArray.getJSONObject(0);

                if (!(chkObj.getString("NearImage").equals(""))
                        && !(chkObj.getString("FarImage").equals(""))) {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, Common.site + "postSupervisorDetails",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Log.d("server response", "" + response);
                                        JSONObject jobj = new JSONObject(response.trim());
                                        if (jobj.getString("Status_Code").equals("115")) {
                                            Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                            db = new DatabaseHelper(act);
                                            boolean isUpdated = db.updateDataFlag(localId, "1");
        //                                    boolean isUpdated1 = db.updatePrintFlag(printid,"1");
                                            Log.d("updateDataFlag ", "" + isUpdated);
                                            db.close();
                                            if (count > 0) {
                                                callUpload();
                                            } else {
                                                pd.dismiss();
                                                position = 0;
                                                Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                                File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/HM.txt");
                                                if (myDir.exists()) {

                                                    if (myDir.delete()) {
                                                        Log.d("inside myDir", "deleted");
                                                    }
                                                }
                                                saveFile(fileArray);
                                            }
                                        } else {
        //                                    Toast.makeText(act, "Entry " + position + " upload " +jobj.getString("Message") + " status code = " + jobj.getString("Status_Code"), Toast.LENGTH_SHORT).show();
                                            if(jobj.getString("Status_Code").equals("116"))
                                            {
                                                Toast.makeText(act, "error code : "+jobj.getString("Status_Code")+", "+jobj.getString("Message") + " for print no: "+chkObj.getString("Printno"), Toast.LENGTH_LONG).show();
                                                db.deleteSingleData(chkObj.getString("id"));
                                            }
                                            else if(jobj.getString("Status_Code").equals("117"))
                                            {
                                                Toast.makeText(act, "error code : "+jobj.getString("Status_Code")+", "+jobj.getString("Message") + " for print no: "+chkObj.getString("Printno"), Toast.LENGTH_LONG).show();
                                                db.deleteSingleData(chkObj.getString("id"));
                                            }
                                            else if(jobj.getString("Status_Code").equals("118") || jobj.getString("Status_Code").equals("118"))
                                            {
                                                Toast.makeText(act, "error code : "+jobj.getString("Status_Code")+", "+jobj.getString("Message") + " for print no: "+chkObj.getString("Printno"), Toast.LENGTH_LONG).show();
//                                                db.deleteSingleData(chkObj.getString("id"));
                                                fileArray.put(uploadArray.getJSONObject(0));
                                            }
                                            else
                                            {
                                                Toast.makeText(act, "error code : "+jobj.getString("Status_Code")+", "+jobj.getString("Message") + " for print no: "+chkObj.getString("Printno"), Toast.LENGTH_LONG).show();
                                                db.deleteSingleData(chkObj.getString("id"));
                                            }
                                            if (count > 0) {
                                                callUpload();
//                                                fileArray.put(uploadArray.getJSONObject(0));
                                            } else {
                                                pd.dismiss();
                                                position = 0;
//                                                fileArray.put(uploadArray.getJSONObject(0));
                                                saveFile(fileArray);
//                                                Toast.makeText(act, "Some entries may have not been uploaded.", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    } catch (Exception e) {
                                        position = 0;
                                        e.printStackTrace();
                                        Log.d("exception occured", "" + e.toString());
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
//                                    pd.dismiss();
                                    Log.d("error:->", "error Occured" + error.getMessage());
                                    try {
                                        if (count > 0) {
                                            callUpload();
                                        }
                                        else
                                        {
                                            pd.dismiss();
                                        }
//                                        position = 0;
                                        try {
                                            byte[] htmlBodyBytes = error.networkResponse.data;
                                            Log.e("", "" + new String(htmlBodyBytes), error);
                                            Toast.makeText(act, "Html error = " + new String(htmlBodyBytes), Toast.LENGTH_SHORT).show();
                                            try {
                                                uploadException(error.getMessage()+", "+new String(htmlBodyBytes),"uploadData VolleyError","DWP S MainActivity");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                        Toast.makeText(act, "Network error. Or Server Error" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    } catch (NullPointerException e) {
                                        Log.d("error:->", "NullPointerException " + error.getMessage());
                                    }
                                }
                            }) {
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("json", uploadArray.toString());
                            return map;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
                } else {
                    Toast.makeText(act, "Image not found or deleted for "+chkObj.getString("Printno"), Toast.LENGTH_SHORT).show();
                    if (count > 0) {
                        callUpload();
                    } else {
                        pd.dismiss();
                        position = 0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    saveErrorFile("uploadata MainActivity : " + e.getMessage());
                    uploadException(e.getMessage(),"uploadData","DWP S MainActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            position = 0;
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    public String imageToString(String path) {

        String imageText = "";
        try {
//        fileName = FilenameUtils.removeExtension(FilenameUtils.getName(path));
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                File file = new File(path);
                if (file.exists()) {
                    Bitmap bitmap = new CompressImage().getBitmapFromFile(file);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    imageText = Base64.encodeToString(byteArray, Base64.DEFAULT);
                }
                if (imageText.equals(""))
                    Log.d("imageText", " empty");
//                    Toast.makeText(act, "Error converting image", Toast.LENGTH_SHORT).show();
                else
                    Log.d("imageText", "not empty");
            } else {
                path = path.replace("file:", "");
                Uri tempURI = Uri.parse(path);
                File file = new File(tempURI.getPath());
                if (file.exists()) {
                    Bitmap bitmap = new CompressImage().getBitmapFromFile(file);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    imageText = Base64.encodeToString(byteArray, Base64.DEFAULT);
                }
                if (imageText.equals(""))
                    Log.d("imageText", " empty");
//                    Toast.makeText(act, "Error converting image", Toast.LENGTH_SHORT).show();
                else
                    Log.d("imageText", "not empty");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("imageToString MainActivity : " + e.getMessage());
                uploadException(e.getMessage(),"imageToString","DWP S MainActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return imageText;
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
                                        Log.d("CatchException response",""+response);
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
                                        Toast.makeText(act, "Network error while uploading exception."+error.getMessage(), Toast.LENGTH_SHORT).show();
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
