package com.skyindya.dwsupervisor2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class VendorUserPendingPlans extends AppCompatActivity {

    TableLayout listTable;
    Activity act;
    JSONArray jarr;
    private ProgressDialog pd;
    DatabaseHelper db;
    int percent = 0;
    int cal = 0, tot = 0;
    String strCharacters = "";
    int count = 0;
    MediaPlayer mp;

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
                getData();
            } catch (Exception e) {
                saveErrorFile("onOptionsItemSelected VendorUserPendingPlans : " + e.getMessage());
                e.printStackTrace();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_user_pending_plans);
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
            db = new DatabaseHelper(act);
            Cursor cs1 = db.viewPendingPlans();
            if(cs1.getCount()>0)
            {
                dbToJson();
                setTable();
            }
            else
            {
                getData();
            }
        }catch (Exception e)
        {
            saveErrorFile("onCreate VendorUserPendingPlans : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setTable() {
        while (listTable.getChildCount() > 1)
            listTable.removeView(listTable.getChildAt(listTable.getChildCount() - 1));

        try {
//            jarr = new JSONArray(js);
            int len = jarr.length();
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    final JSONObject jo = jarr.getJSONObject(i);
                    final TableRow tr = new TableRow(act);
                    TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                    tp.weight = 1;

                    TextView tvID = new TextView(act);
                    TextView tvCode = new TextView(act);
                    TextView tvVillage = new TextView(act);
                    TextView tvTehsil = new TextView(act);
                    TextView tvBal = new TextView(act);
                    TextView tvTotal = new TextView(act);
                    TextView tvVendor = new TextView(act);

                    tvID.setText(jo.getString("PlanID"));
                    tvCode.setText(jo.getString("VillageCode"));
                    tvVillage.setText(jo.getString("VillageName"));
                    tvTehsil.setText(jo.getString("Tehsil"));
                    tvBal.setText(jo.getString("NofBalance"));
                    tvTotal.setText(jo.getString("NoOfPrints"));
                    tvVendor.setText(jo.getString("vendorname"));
                    tvID.setVisibility(View.GONE);

                    Common.setRow(tp, tvID, tr, act);
                    Common.setRow(tp, tvCode, tr, act);
                    Common.setRow(tp, tvVillage, tr, act);
                    Common.setRow(tp, tvTehsil, tr, act);
                    Common.setRow(tp, tvBal, tr, act);
                    Common.setRow(tp, tvTotal, tr, act);
                    Common.setRow(tp, tvVendor, tr, act);

                    listTable.addView(tr);

                }
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
            saveErrorFile("setTable VendorUserPendingPlans : " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(act, "Error occurred while fetching location.", Toast.LENGTH_SHORT).show();
        }
    }

    public void getData() {
        try {
            if (new Common().isNetworkAvailable(act)) {
                pd.setMessage("Loading villages...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, Common.site + "GetVendorUserPendingPlan?vImeiNumber=" + Common.getImei(act),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    jarr = new JSONArray(response.trim());
                                    if (jarr.length() > 0)
                                        plansJsonToDB();
                                    else {
                                        db = new DatabaseHelper(act);
                                        db.deletePendingPlans();
                                        Toast.makeText(act, "No Data...", Toast.LENGTH_SHORT).show();
                                        pd.dismiss();
                                    }
                                } catch (Exception e) {
                                    saveErrorFile("onResponse getData VendorUserPendingPlans : " + e.getMessage());
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
                                    Toast.makeText(act, "Network error or Server error."+ error.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    saveErrorFile("onErrorResponse getData VendorUserPendingPlans : " + e.getMessage());
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
            } else {
                Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
    //            dbToJson();
    //            setTable();
            }
        } catch (Exception e) {
            saveErrorFile("getData VendorUserPendingPlans : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void plansJsonToDB() {
        try {
            db = new DatabaseHelper(act);
            db.deletePendingPlans();
            final int len = jarr.length();
            count = len;
            for (int i = 0; i < len; i++) {
                final JSONObject jo = jarr.getJSONObject(i);
                new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... voids) {
                        try {
                            cal++;
                            percent = cal * 100;
                            percent = percent / len;
                            db.addPendingPlans(jo.getString("PlanID"), jo.getString("VillageCode"), jo.getString("VillageName"),
                                    jo.getString("Tehsil"), jo.getString("DistrictName"), jo.getString("stateName"),
                                    jo.getString("NofBalance"),jo.getString("NoOfPrints"),jo.getString("vendorname"),jo.getString("Range"));
                        } catch (Exception e) {
                            saveErrorFile("doInBackground plansJsonToDB VendorUserPendingPlans : " + e.getMessage());
                            e.printStackTrace();
                            pd.dismiss();
                        }
                        return 1;
                    }

                    public void onPostExecute(Integer location) {
                        try {
                            count--;
                            pd.setMessage("Loading artworks... " + percent + "%");
                            if(count == 0) {
                                Log.d("inside","count == 0");
                                if(db!=null)
                                    db.close();
                                pd.dismiss();
                                setTable();
                            }
                        } catch (Exception e) {
                            saveErrorFile("onPostExecute plansJsonToDB VendorUserPendingPlans : " + e.getMessage());
                            e.printStackTrace();
                            pd.dismiss();
                        }
                    }

                }.execute();

            }
        } catch (Exception e) {
            saveErrorFile("plansJsonToDB VendorUserPendingPlans : " + e.getMessage());
            e.printStackTrace();
            pd.dismiss();
        }
    }

    public void dbToJson() {
        try {
            db = new DatabaseHelper(act);
            jarr = new JSONArray();
            Cursor cs = db.viewPendingPlans();
            if (cs.getCount() > 0) {
                while (cs.moveToNext()) {
                    JSONObject jo = new JSONObject();
                    jo.put("PlanID", cs.getString(1));
                    jo.put("VillageCode", cs.getString(2));
                    jo.put("VillageName", cs.getString(3));
                    jo.put("Tehsil", cs.getString(4));
                    jo.put("DistrictName", cs.getString(5));
                    jo.put("stateName", cs.getString(6));
                    jo.put("NofBalance", cs.getString(7));
                    jo.put("NoOfPrints", cs.getString(8));
                    jo.put("vendorname", cs.getString(9));
                    jarr.put(jo);
                }
            } else {
                Toast.makeText(act, "No Pending List.", Toast.LENGTH_SHORT).show();
            }
            cs.close();
            db.close();
        } catch (Exception e) {
            saveErrorFile("dbToJson VendorUserPendingPlans : " + e.getMessage());
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
}
