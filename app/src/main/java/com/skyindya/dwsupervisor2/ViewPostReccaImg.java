package com.skyindya.dwsupervisor2;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ViewPostReccaImg extends AppCompatActivity {

    JSONObject printObj;
    String id="";
    double distanceMarker;
    Button btnContinue;
    Activity act;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postreccimg);
        try {
            act = this;
            pd = new ProgressDialog(act, R.style.AppTheme_Dark_Dialog);
            printObj = new JSONObject(getIntent().getStringExtra("json"));
            distanceMarker=new Double(getIntent().getStringExtra("distanceMarker"));
            Log.d("Brahma",printObj.toString());
            btnContinue = (Button) findViewById(R.id.btnContinue);

            id=printObj.getString("id");
//            Log.d("Brahma", "Final distance: " + distanceMarker);
            if(distanceMarker>200)
            {
                btnContinue.setVisibility(View.INVISIBLE);
            }
            getImageUrl(id);
        } catch (Exception e) {

            e.printStackTrace();
            Log.d("Brahma", "Exception: "+e);
        }

    }

    public void getImageUrl(String vid) {

        if (new Common().isNetworkAvailable(act)) {

            try {
                pd.setMessage("Wait...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "GetImageUrl?createdplanVid=" + vid,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    pd.dismiss();
                                    Log.d("Brahma", "" + response.trim());
                                    JSONArray ja = new JSONArray(response.trim());
                                    Log.d("ja ----", "" + ja.toString());
                                    if (ja.getJSONObject(0).getString("Response").equalsIgnoreCase("Success")) {

                                        ImageView frontImgView = (ImageView)findViewById(R.id.frontPic);
                                        ImageView surroundImgView = (ImageView)findViewById(R.id.surroundPic);
                                        Picasso.get().load(ja.getJSONObject(0).getString("straight_view")).into(frontImgView);
                                        Picasso.get().load(ja.getJSONObject(0).getString("surrounding_view")).into(surroundImgView);


                                    }

                                } catch (Exception e) {
                                    saveErrorFile("onResponse getImageUrl ViewPostReccaImg : " + e.getMessage());
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
                                    saveErrorFile("onErrorResponse ViewPostReccaImg getImageUrl : " + error.getMessage());
                                    Toast.makeText(act, "Network error.", Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {
//                    @Override
//                    protected Map<String, String> getParams() throws AuthFailureError {
//
//                        Map<String, String> map = new HashMap<String, String>();
//                        map.put("createdplanVid", "3134");
//                        //                    map.put("vImeiNumber", "464BVV66sbbd665");
//                        return map;
//                    }
                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
//
            } catch (Exception e) {
                saveErrorFile("getImageUrl ViewPostReccaImg : " + e.getMessage());
                e.printStackTrace();
                Log.d("Brahma", "Exception: "+e);
            }
        } else {
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }

    }

    public void saveErrorFile(String er) {
        try {
            File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/DWPSError.txt");
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


    public void onContinue(View view)
    {
        Log.d("Brahma", "onContinue: ");
        Intent i = new Intent(act, ExecuteActivity.class);
        i.putExtra("json", printObj.toString());
        startActivity(i);

    }
}

