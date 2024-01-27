package com.skyindya.dwsupervisor2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PendingListActivity extends AppCompatActivity {

    TableLayout listTable;
    Activity act;
    JSONArray jarr, printsArray, remarksArray;
    private ProgressDialog pd;
    DatabaseHelper db;
    int count = 0;
    int percent = 0;
    int cal = 0, tot = 0;
    String strCharacters = "";
    MediaPlayer mp;
    Button srchBtn;
    EditText villageCode;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pending_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.reloadplans) {
//            Intent i = new Intent(act,VendorUserPendingPlans.class);
//            startActivity(i);
            try {
                mp.start();
                db = new DatabaseHelper(act);
                Cursor crs = db.viewData("");
                if (crs.getCount() > 0) {
                    ShowAlert();
                }
                 else {
                    getData(0);
                }
                crs.close();
                db.close();
            } catch (Exception e) {
                saveErrorFile("onOptionsItemSelected PendingListActivity : " + e.getMessage());
                e.printStackTrace();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_list);
        act = this;
        try {
                mp = MediaPlayer.create(act, R.raw.tone);
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
                pd = new ProgressDialog(act, R.style.AppTheme_Dark_Dialog);
                listTable = (TableLayout) findViewById(R.id.listTable);
                villageCode = (EditText) findViewById(R.id.villageCode);
                srchBtn = (Button) findViewById(R.id.srchBtn);

                db = new DatabaseHelper(act);
                Cursor crs = db.viewData("");
                if (crs.getCount() > 0) {
                    ShowAlert();
                }
                crs.close();
                Cursor cs1 = db.viewPlans();
                if (cs1.getCount() > 0) {
                    dbToJson();
                    setTable(0);
    //            plansJsonToDB();
                } else {
                    getData(1);
                }
                cs1.close();
                db.close();

                villageCode.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        Log.d("inside","onTextChanged s = "+s);
                        if(s.toString().isEmpty())
                        {
                            Log.d("inside"," s = ''  "+s);
                            dbToJson();
                            setTable(0);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                srchBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if(villageCode.getText().toString().equals(""))
                            {
                                Toast.makeText(act, "Enter valid village code", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                db = new DatabaseHelper(act);
                                jarr = new JSONArray();
                                Cursor cvs = db.viewVillage(villageCode.getText().toString());
                                if (cvs.getCount() > 0) {
                                    while (cvs.moveToNext()) {
                                        JSONObject jo = new JSONObject();
                                        jo.put("planid", cvs.getString(1));
                                        jo.put("VillageCode", cvs.getString(2));
                                        jo.put("VillageName", cvs.getString(3));
                                        jo.put("Tehsil", cvs.getString(4));
                                        jo.put("DistrictName", cvs.getString(5));
                                        jo.put("stateName", cvs.getString(6));
                                        jo.put("balance", cvs.getString(7));
                                        jarr.put(jo);
                                    }
                                    setTable(0);
                                } else {
                                    Toast.makeText(act, "Village not found in pending list.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            saveErrorFile("onCreate PendingListActivity : " + e.getMessage());
                            e.printStackTrace();
                        }

                    }
                });


        } catch (Exception e) {
            saveErrorFile("onCreate PendingListActivity : " + e.getMessage());
            e.printStackTrace();
            try {
                uploadException(e.getMessage(), "onCreate", "DWP S PendingListActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void setTable(int st) {
        try {
            while (listTable.getChildCount() > 1)
                listTable.removeView(listTable.getChildAt(listTable.getChildCount() - 1));
        } catch (Exception e) {
            saveErrorFile("setTable PendingListActivity : " + e.getMessage());
            e.printStackTrace();
        }

        try {
//            jarr = new JSONArray(js);
            Log.d("inside","setTable");
            int len = jarr.length();
            if (len > 0) {
                String planName = "",prjName = "";
                for (int i = 0; i < len; i++) {
                    final JSONObject jo = jarr.getJSONObject(i);
                    final TableRow tr = new TableRow(act);
                    TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                    tp.weight = 1;
                    Log.d("PlanName",""+jo.optString("PlanName"));
                    if(!planName.equals(jo.optString("PlanName")))
                    {
                        Log.d("inside","if PlanName= "+planName);
                        final TableRow tr1 = new TableRow(act);
                        TableRow.LayoutParams tp1 = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                        tp1.weight = 1;
                        TextView tvPlanName = new TextView(act);
                        tvPlanName.setText(jo.optString("ProjectName")+" , Plan : "+jo.optString("PlanName"));
                        Common.setRow(tp1, tvPlanName, tr1, act);
                        listTable.addView(tr1);
                        planName = jo.optString("PlanName");
                    }

                    TextView tvID = new TextView(act);
                    TextView tvCode = new TextView(act);
                    TextView tvVillage = new TextView(act);
                    TextView tvTehsil = new TextView(act);
                    TextView tvBal = new TextView(act);
//                    ImageView imAction = new ImageView(act);

                    tvID.setText(jo.getString("planid"));
                    tvCode.setText(jo.getString("VillageCode"));
                    tvVillage.setText(jo.getString("VillageName"));
                    tvTehsil.setText(jo.getString("Tehsil"));
                    tvBal.setText(jo.getString("balance"));
//                    imAction.setImageDrawable(getResources().getDrawable(R.drawable.no_execution));

                    tvID.setVisibility(View.GONE);
                    Common.setRow(tp, tvID, tr, act);
                    Common.setRow(tp, tvCode, tr, act);
                    Common.setRow(tp, tvVillage, tr, act);
                    Common.setRow(tp, tvTehsil, tr, act);
                    Common.setRow(tp, tvBal, tr, act);
//                    imAction.setLayoutParams(tp);
//                    imAction.setPadding(2, 2, 2, 2);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//                        imAction.setBackground(act.getResources().getDrawable(R.drawable.border));
//                    else
//                        imAction.setBackgroundDrawable(act.getResources().getDrawable(R.drawable.border));
//                    tr.addView(imAction);
                    listTable.addView(tr);

                    tr.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {

                                //Commented by Brahma Nishad -- 14-12-2022

//                                Intent i = new Intent(act, ArtworkActivity.class);
//                                i.putExtra("code", jo.getString("VillageCode"));
//                                Log.d("VillageCode ", "" + jo.getString("VillageCode"));
//                                startActivity(i);
//                                Commented by Brahma Nishad -- 14-12-2022 end

                            } catch (Exception e) {
                                saveErrorFile("tr.setOnClickListener PendingListActivity : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    });

//                    imAction.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            askDelete(tr);
//                        }
//                    });
                }
                if (st == 1)
                    getExecutedData();
            } else {
                final TableRow tr = new TableRow(act);
                TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                tp.weight = 1;
                TextView tvID = new TextView(act);
                tvID.setText("No Data.");
                Common.setRow(tp, tvID, tr, act);
                listTable.addView(tr);
            }

        } catch (Exception e) {
            saveErrorFile("setTable PendingListActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getData(int df) {
        if (new Common().isNetworkAvailable(act)) {
            try {
                pd.setMessage("Loading Villages... 0 %");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "GetExecutedPlanperImei?vImeiNumber="+ Common.getImei(act),//7251742129883195",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.d("response = ", "" + response.trim());
                                    jarr = new JSONArray(response.trim());
                                    if (jarr.length() > 0)
                                        plansJsonToDB();
                                    else {
                                        db = new DatabaseHelper(act);
                                        db.deletePlans();
                                        db.deletePrints();
                                        db.close();
                                        Toast.makeText(act, "No Data for villages...", Toast.LENGTH_SHORT).show();
                                        pd.dismiss();
                                    }
                                } catch (Exception e) {
                                    saveErrorFile("onResponse getData PendingListActivity : " + e.getMessage());
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
                                    Toast.makeText(act, "Network error or Server error." + error.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<String, String>();
                        //                    map.put("imei", Common.getImei(act));
                        return map;
                    }
                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
            } catch (Exception e) {
                saveErrorFile("getData PendingListActivity : " + e.getMessage());
                e.printStackTrace();
                try {
                    uploadException(e.getMessage(), "getData", "DWP S PendingListActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            try {
                Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
                if (df == 1) {
                    dbToJson();
                    setTable(0);
                }
            } catch (Exception e) {
                saveErrorFile("no internet else getData PendingListActivity : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void getExecutedData() {
        if (new Common().isNetworkAvailable(act)) {
            try {
                pd.setMessage("Loading Data... 0 %");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "GetExecutedPlanDetailsPerimei?vImeiNumber="+ Common.getImei(act),//7251742129883195",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.d("response = ", "" + response.trim());
                                    printsArray = new JSONArray(response.trim());
                                    if (printsArray.length() > 0)
                                        printsJsonToDB();
                                    else {
                                        Toast.makeText(act, "No Data for artwork...", Toast.LENGTH_SHORT).show();
                                        getRemarks();
                                        pd.dismiss();
                                    }
                                } catch (Exception e) {
                                    saveErrorFile("onResponse getExecutedData PendingListActivity : " + e.getMessage());
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
                                    Toast.makeText(act, "Network error or Server error." + error.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<String, String>();
                        //                    map.put("imei", Common.getImei(act));
                        return map;
                    }
                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
            } catch (Exception e) {
                saveErrorFile("getExecutedData PendingListActivity : " + e.getMessage());
                e.printStackTrace();
                try {
                    uploadException(e.getMessage(), "getExecutedData", "DWP S PendingListActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    public void getRemarks() {
        if (new Common().isNetworkAvailable(act)) {
            try {
                pd.setMessage("Loading Remarks...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "GetRemarks",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.d("response = ", "" + response.trim());
                                    remarksArray = new JSONArray(response.trim());
                                    if (remarksArray.length() > 0)
                                        remarksJsonToDB();
                                    else {
                                        Toast.makeText(act, "No Data for Remarks...", Toast.LENGTH_SHORT).show();
                                        pd.dismiss();
                                    }
                                } catch (Exception e) {
                                    pd.dismiss();
                                    saveErrorFile("onResponse getRemarks PendingListActivity : " + e.getMessage());
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
                                    Toast.makeText(act, "Network error or Server error." + error.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("imei", Common.getImei(act));
                        return map;
                    }
                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    saveErrorFile("getRemarks PendingListActivity : " + e.getMessage());
                    uploadException(e.getMessage(), "getRemarks", "DWP S PendingListActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    public void plansJsonToDB() {
        try {
            db = new DatabaseHelper(act);
            db.deletePlans();
            db.deletePrints();
            cal = 0;
            percent = 0;
//            jarr = new JSONArray(js);
            int len = jarr.length();
            count = 0;
            tot = len;

            getAddressLocation();

//            for (int i = 0; i < len; i++) {
//                final JSONObject jo = jarr.getJSONObject(i);
//                new AsyncTask<Void, Void, Location>() {
//                    @Override
//                    protected Location doInBackground(Void... voids) {
//                        Location location = new Location("dummyprovider");
//                        try {
//                            percent = cal * 100;
//                            percent = percent / tot;
//                            Geocoder coder = new Geocoder(act);
//                            List<Address> address;
//                            address = coder.getFromLocationName(jo.getString("VillageName") + ", " + jo.getString("stateName"), 2);
//                            if (address != null && address.size() > 0) {
//                                Log.d("address ", "" + address.toString());
//                                Address addr = address.get(0);
//                                location.setLatitude(addr.getLatitude());
//                                location.setLongitude(addr.getLongitude());
//                                Log.d("inside ", "" + addr.getLatitude() + "  " + addr.getLongitude());
//                            } else {
//                                return null;
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        return location;
//                    }
//
//                    public void onPostExecute(Location location) {
//                        try {
//                            count--;
//                            cal++;
//                            strCharacters = "Loading villages... " + percent + " %";
//                            pd.setMessage(strCharacters);
//                            if (location != null) {
//                                db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
//                                        jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
//                                        jo.getString("balance"), "" + location.getLatitude(), "" + location.getLongitude(), jo.getString("Range"));
//                            } else {
//                                db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
//                                        jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
//                                        jo.getString("balance"), "", "", jo.getString("Range"));
//                            }
//                            if (count == 0) {
//                                pd.dismiss();
//                                setTable(1);
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            pd.dismiss();
//                        }
//                    }
//                }.execute();
////                Geocoder coder = new Geocoder(this);
////                List<Address> address;
////                address = coder.getFromLocationName(jo.getString("VillageName") + "," + jo.getString("DistrictName") + "," + jo.getString("stateName"), 2);
////                if (address != null && address.size()>0) {
////                    Log.d("address ",""+address.toString());
////                    Address location = address.get(0);
////                    location.getLatitude();
////                    location.getLongitude();
////                    db.addPlans(jo.getString("PlanID"), jo.getString("VillageCode"), jo.getString("VillageName"),
////                            jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
////                            jo.getString("NofBalance"),""+location.getLatitude(),""+location.getLongitude());
////                }
////                else
////                {
////                    db.addPlans(jo.getString("PlanID"), jo.getString("VillageCode"), jo.getString("VillageName"),
////                            jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
////                            jo.getString("NofBalance"),"","");
////                }
//            }
//            db.close();
        } catch (Exception e) {
            saveErrorFile("plansJsonToDB PendingListActivity : " + e.getMessage());
            e.printStackTrace();

        }
    }

    public void getAddressLocation() {
        try {
            if (tot > 0) {
//                final JSONObject jo = jarr.getJSONObject(count);
                percent = cal * 100;
                percent = percent / tot;
                addAdddressToDB1(jarr.getJSONObject(count));
            }
        } catch (Exception e) {
            saveErrorFile("getAddressLocation PendingListActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addAdddressToDB1(final JSONObject jo)
    {
        try {

            new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... voids) {
                    try {
                        count++;
                        cal++;
                        db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                jo.getString("balance"), "0.0", "0.0", jo.getString("Range"),
                                jo.getString("ProjectName"),jo.getString("PlanName"));
                    } catch (Exception e) {
                        saveErrorFile("doInBackground printsJsonToDB PendingListActivity : " + e.getMessage());
                        e.printStackTrace();
                        pd.dismiss();
                    }
                    return 1;
                }

                public void onPostExecute(Integer location) {
                    try {
                        strCharacters = "Loading villages... " + percent + " %";
                        pd.setMessage(strCharacters);
                        if (count == tot) {
                            pd.dismiss();
                            setTable(1);
                            db.close();
                        } else {
                            getAddressLocation();
                        }
                    } catch (Exception e) {
                        saveErrorFile("onPostExecute printsJsonToDB PendingListActivity : " + e.getMessage());
                        e.printStackTrace();
                        pd.dismiss();
                    }
                }

            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAdddressToDB(final JSONObject jo) {
        try {
            StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://maps.googleapis.com/maps/api/geocode/json?address=" + jo.getString("VillageName").replaceAll("\\s", "%20") + "," + jo.getString("stateName").replaceAll("\\s", "%20") + "&sensor=false&language=en",
                    new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            try {
                                count++;
                                cal++;
                                strCharacters = "Loading villages... " + percent + " %";
                                pd.setMessage(strCharacters);
                                Log.d("maps api ", "response ");
                                JSONObject ja = new JSONObject(response);
                                if (ja.optString("status").equalsIgnoreCase("OK") && ja.has("results")) {
                                    Log.d("maps api ", "status ok ");
                                    JSONArray objarr = ja.getJSONArray("results");
                                    if (objarr.length() > 0) {
                                        JSONObject geo = objarr.optJSONObject(0).optJSONObject("geometry");
                                        JSONObject loc = geo.optJSONObject("location");
                                        db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                                jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                                jo.getString("balance"), loc.optString("lat"), loc.optString("lng"), jo.getString("Range"),
                                                jo.getString("ProjectName"),jo.getString("PlanName"));
                                    }
                                    if (count == tot) {
                                        pd.dismiss();
                                        setTable(1);
                                        db.close();
                                    } else {
                                        getAddressLocation();
                                    }
                                } else {
                                    Log.d("maps api ", "status not ok ");
                                    db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                            jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                            jo.getString("balance"), "0.0", "0.0", jo.getString("Range"),
                                            jo.getString("ProjectName"),jo.getString("PlanName"));
                                    if (count == tot) {
                                        pd.dismiss();
                                        setTable(1);
                                        db.close();
                                    } else {
                                        getAddressLocation();
                                    }
                                }

                            } catch (Exception e) {
                                saveErrorFile("onResponse addAdddressToDB PendingListActivity : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("error:->", "error Occured" + error.getMessage());
                            try {
                                Toast.makeText(act, "Could not get location for "+jo.getString("VillageName"), Toast.LENGTH_SHORT).show();
                                count++;
                                cal++;
                                strCharacters = "Loading villages... " + percent + " %";
                                pd.setMessage(strCharacters);
                                Log.d("maps api ", "status not ok ");
                                db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                        jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                        jo.getString("balance"), "", "", jo.getString("Range"),
                                        jo.getString("ProjectName"),jo.getString("PlanName"));
                                if (count == tot) {
                                    pd.dismiss();
                                    setTable(1);
                                    db.close();
                                } else {
                                    getAddressLocation();
                                }
                            } catch (Exception e) {
                                saveErrorFile("onErrorResponse addAdddressToDB PendingListActivity : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    Map<String, String> map = new HashMap<String, String>();
                    return map;
                }
            };
            VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("addAdddressToDB PendingListActivity : " + e.getMessage());
                uploadException(e.getMessage(), "addAdddressToDB", "DWP S PendingListActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void secondAddressTry(final JSONObject jo) {
        Log.d("inside ", "second try for address");
        try {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://maps.googleapis.com/maps/api/geocode/json?address=" + jo.getString("VillageName").replaceAll("\\s", "%20") + "," + jo.getString("stateName").replaceAll("\\s", "%20") + "&sensor=false&language=en",
                                new Response.Listener<String>() {

                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Log.d("maps api ", "response ");
                                            JSONObject ja = new JSONObject(response);
                                            if (ja.optString("status").equalsIgnoreCase("OK") && ja.has("results")) {
                                                Log.d("maps api ", "status ok ");
                                                JSONArray objarr = ja.getJSONArray("results");
                                                if (objarr.length() > 0) {
                                                    JSONObject geo = objarr.optJSONObject(0).optJSONObject("geometry");
                                                    JSONObject loc = geo.optJSONObject("location");
                                                    db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                                            jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                                            jo.getString("balance"), loc.optString("lat"), loc.optString("lng"), jo.getString("Range"),
                                                            jo.getString("ProjectName"),jo.getString("PlanName"));
                                                }
                                            } else {
                                                Log.d("maps api ", "status not ok ");
                                                db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                                        jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                                        jo.getString("balance"), "", "", jo.getString("Range"),
                                                        jo.getString("ProjectName"),jo.getString("PlanName"));
                                            }
                                            if (count == tot) {
                                                pd.dismiss();
                                                setTable(1);
                                                db.close();
                                            } else {
                                                getAddressLocation();
                                            }
                                        } catch (Exception e) {
                                            saveErrorFile("onResponse secondAddressTry PendingListActivity : " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("error:->", "error Occured" + error.getMessage());
                                        try {
                                            Toast.makeText(act, "Could not get location for "+jo.getString("VillageName"), Toast.LENGTH_SHORT).show();
                                            count++;
                                            cal++;
                                            strCharacters = "Loading villages... " + percent + " %";
                                            pd.setMessage(strCharacters);
                                            Log.d("maps api ", "status not ok ");
                                            db.addPlans(jo.getString("planid"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                                    jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                                    jo.getString("balance"), "", "", jo.getString("Range"),
                                                    jo.getString("ProjectName"),jo.getString("PlanName"));
                                            if (count == tot) {
                                                pd.dismiss();
                                                setTable(1);
                                                db.close();
                                            } else {
                                                getAddressLocation();
                                            }
                                        } catch (Exception e) {
                                            saveErrorFile("onErrorResponse secondAddressTry PendingListActivity : " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }) {
                            @Override
                            protected Map<String, String> getParams() throws AuthFailureError {

                                Map<String, String> map = new HashMap<String, String>();
                                return map;
                            }
                        };
                        VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
                    } catch (Exception e) {
                        saveErrorFile("secondAddressTry PendingListActivity : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }, 2000);
        } catch (Exception e) {
            saveErrorFile("secondAddressTry PendingListActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void printsJsonToDB() {
        try {
            db = new DatabaseHelper(act);
            final int len = printsArray.length();
            cal = 0;
            percent = 0;
            count = len;
            for (int i = 0; i < len; i++) {
                final JSONObject jo = printsArray.getJSONObject(i);
                new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... voids) {
                        try {
                            cal++;
                            percent = cal * 100;
                            percent = percent / len;
                            db.addPrints(jo.getString("id"), jo.getString("PlanID"), jo.getString("VillageCode"),
                                    jo.getString("brand"), jo.getString("Width"), jo.getString("Height"), jo.getString("Sqft"), jo.getString("language"),
                                    jo.getString("Printno"), jo.getString("Latitude"), jo.getString("Longitude"), jo.getString("Address"), jo.getString("Remarks"), jo.getString("projectCode"));
                        } catch (Exception e) {
                            saveErrorFile("doInBackground printsJsonToDB PendingListActivity : " + e.getMessage());
                            e.printStackTrace();
                            pd.dismiss();
                        }
                        return 1;
                    }

                    public void onPostExecute(Integer location) {
                        try {
                            count--;
                            pd.setMessage("Loading artworks... " + percent + "%");
                            if (count == 0) {
                                Log.d("inside", "count == 0");
                                if (db != null)
                                    db.close();
                                pd.dismiss();
                                getRemarks();
                            }
                        } catch (Exception e) {
                            saveErrorFile("onPostExecute printsJsonToDB PendingListActivity : " + e.getMessage());
                            e.printStackTrace();
                            pd.dismiss();
                        }
                    }

                }.execute();
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("printsJsonToDB PendingListActivity : " + e.getMessage());
                uploadException(e.getMessage(), "printsJsonToDB", "DWP S PendingListActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            pd.dismiss();
        }
    }

    public void remarksJsonToDB() {
        try {
            db = new DatabaseHelper(act);
            db.deleteRemarks();
            int len = remarksArray.length();
            for (int i = 0; i < len; i++) {
                final JSONObject jo = remarksArray.getJSONObject(i);
                db.addRemarks(jo.getString("Id"), jo.getString("Remarks"), jo.getString("ProjectName"));
            }
            db.close();
            pd.dismiss();
        } catch (Exception e) {
            saveErrorFile("remarksJsonToDB PendingListActivity : " + e.getMessage());
            e.printStackTrace();
            pd.dismiss();
        }
    }

    public void dbToJson() {
        try {
            db = new DatabaseHelper(act);
            jarr = new JSONArray();
            Cursor cs = db.viewPlans();
            if (cs.getCount() > 0) {
                while (cs.moveToNext()) {
                    JSONObject jo = new JSONObject();
                    jo.put("planid", cs.getString(1));
                    jo.put("VillageCode", cs.getString(2));
                    jo.put("VillageName", cs.getString(3));
                    jo.put("Tehsil", cs.getString(4));
                    jo.put("DistrictName", cs.getString(5));
                    jo.put("stateName", cs.getString(6));
                    jo.put("balance", cs.getString(7));
                    jo.put("ProjectName", cs.getString(11));
                    jo.put("PlanName", cs.getString(12));
                    jarr.put(jo);
                }
            } else {
                Toast.makeText(act, "No Pending List.", Toast.LENGTH_SHORT).show();
            }
            cs.close();
            db.close();
        } catch (Exception e) {
            saveErrorFile("dbToJson PendingListActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void ShowAlert() {
        try {
            Toast.makeText(act, "Please sync all data before getting new plans.", Toast.LENGTH_SHORT).show();
//            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(act);
//            alertDialogBuilder
//                    .setMessage("Please sync all data before getting new plans.")
//                    .setCancelable(false);
//
//            alertDialogBuilder.setPositiveButton("OK",
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            dialog.cancel();
//                            getData();
//                        }
//                    });
//            AlertDialog a = alertDialogBuilder.create();
//            a.show();
        } catch (Exception e) {
            saveErrorFile("ShowAlert PendingListActivity : " + e.getMessage());
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

//    public void askDelete(final TableRow tr) {
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(act);
//        alertDialogBuilder.setTitle("Are you Sure?")
//                .setMessage("You want to tag this as No Execution")
//                .setCancelable(false);
//
//        alertDialogBuilder.setPositiveButton("Yes",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        getRemark(tr);
//                        dialog.cancel();
//                    }
//                });
//        alertDialogBuilder.setNegativeButton("No",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                    }
//                });
//        AlertDialog a = alertDialogBuilder.create();
//        a.show();
//    }
//
//    public void getRemark(final TableRow tr) {
//        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(act);
//        LayoutInflater li = LayoutInflater.from(act);
//        final View formView = li.inflate(R.layout.remark_form, null);
//
//        final EditText remark = (EditText) formView.findViewById(R.id.remark);
//        final Button submit = (Button) formView.findViewById(R.id.submit);
//
//        alertDialogBuilder.setView(formView);
//        alertDialogBuilder.setTitle("Add Remark.");
//        final AlertDialog a = alertDialogBuilder.create();
//        a.setCancelable(false);
//        a.show();
//
//        submit.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (remark.getText().toString().equals("") || remark.getText().toString() == null) {
//                    remark.setError("Please fill this.");
//                    remark.requestFocus();
//                } else {
//                    listTable.removeView(tr);
//                    double lat, lng;
//                    lat = gps.getLatitude();
//                    lng = gps.getLongitude();
//                    a.dismiss();
//                }
//            }
//        });
//    }

}
