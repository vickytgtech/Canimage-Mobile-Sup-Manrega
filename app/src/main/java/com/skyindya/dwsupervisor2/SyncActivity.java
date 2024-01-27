package com.skyindya.dwsupervisor2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

import static okhttp3.MediaType.parse;

public class SyncActivity extends AppCompatActivity {

    TableLayout syncTable;
    private DatabaseHelper db;
    private ProgressDialog pd;
    JSONArray syncArray, uploadArray, uploadedData;
    MediaPlayer mp;
    Activity act;
    APIInterface apiInterface;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sync_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sync) {
            mp.start();
            getAllUploadedData();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        try {
            act = this;
            pd = new ProgressDialog(SyncActivity.this, R.style.AppTheme_Dark_Dialog);
            syncTable = (TableLayout) findViewById(R.id.syncTable);
            mp = MediaPlayer.create(SyncActivity.this, R.raw.tone);
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
            apiInterface = APIClient.getClient().create(APIInterface.class);
            dbToJson();
        } catch (Exception e) {
            saveErrorFile("onCreate SyncActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getAllUploadedData() {
        try {

            db = new DatabaseHelper(SyncActivity.this);
            uploadedData = new JSONArray();
            Cursor csd = db.viewUploadedData();
            if (csd.getCount() > 0) {
                while (csd.moveToNext()) {
                    JSONObject upJo = new JSONObject();
                    upJo.put("localid", "" + csd.getString(0));
                    upJo.put("ID", "" + csd.getString(1));
                    upJo.put("planid", "" + csd.getString(2));
                    upJo.put("VillageCode", "" + csd.getString(3));
                    upJo.put("Printno", "" + csd.getString(4));
                    upJo.put("ImeiNo", "" + Common.getImei(SyncActivity.this));
                    uploadedData.put(upJo);
                }
            }
            csd.close();
            db.close();
            Log.d("uploaded data = ", "" + uploadedData.toString());
            checkUploadedData();
        } catch (Exception e) {
            saveErrorFile(e.getMessage() + "\n getAllUploadedData" + "\n DWP P SyncActivity");
            uploadException(e.getMessage(), "getAllUploadedData", "DWP P SyncActivity");
            e.printStackTrace();
        }
    }

    public void checkUploadedData() {
        if (new Common().isNetworkAvailable(act)) {
            try {
                pd.setMessage("Checking...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.POST, new Common().site + "CheckForMissingEntriesSup",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    pd.dismiss();
                                    Log.d("CatchException response", "" + response);
                                    JSONArray serverArr = new JSONArray(response.trim());
                                    db = new DatabaseHelper(SyncActivity.this);
                                    for (int s = 0, l = serverArr.length(); s < l; s++) {
                                        JSONObject jObj = serverArr.optJSONObject(s);
                                        db.updateDataFlagAgain("" + jObj.optString("localid"), "0", "" + jObj.optString("createdplanid"), "" + jObj.optString("planid"),
                                                "" + jObj.optString("printno"), "" + jObj.optString("ViilageCode"));
                                    }
                                    db.close();
                                    dbToJson();
                                } catch (Exception e) {
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
                                    Toast.makeText(SyncActivity.this, "Network error while Checking status." + error.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<String, String>();
                        try {
                            map.put("json", uploadedData.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return map;
                    }
                };
                VolleySingleton.getInstance(SyncActivity.this).addRequestQueue(stringRequest);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    saveErrorFile("checkUploadedData SyncActivity : " + e.getMessage());
                    uploadException(e.getMessage(), "checkUploadedData", "DWP S MainActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    public void dbToJson() {
        try {
            String localID = "";
            db = new DatabaseHelper(act);
            syncArray = new JSONArray();
            Cursor cs = db.viewData("");
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
            }
            cs.close();
            db.close();
//            readFile();
            setSyncTable();
        } catch (Exception e) {
            saveErrorFile("dbToJson SyncActivity : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setSyncTable() {

        while (syncTable.getChildCount() > 1) {
            syncTable.removeView(syncTable.getChildAt(syncTable.getChildCount() - 1));
        }

        try {
//            jarr = new JSONArray(js);
            int len = syncArray.length();
            Log.d("syncarray length", "" + len);
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    final JSONObject jo = syncArray.optJSONObject(i);
                    final TableRow tr = new TableRow(SyncActivity.this);
                    TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                    tp.weight = 1;

                    final TextView tvPrintno = new TextView(SyncActivity.this);
                    TextView tvPlan = new TextView(SyncActivity.this);
                    ImageButton imAction = new ImageButton(SyncActivity.this);

                    tvPrintno.setText(jo.optString("Printno"));
                    tvPlan.setText(jo.optString("Planid"));
                    imAction.setImageDrawable(getResources().getDrawable(R.drawable.upload));
//                    imAction.setBackgroundColor(getResources().getColor(R.color.colorAccent));

                    Common.setRow(tp, tvPrintno, tr, SyncActivity.this);
                    Common.setRow(tp, tvPlan, tr, SyncActivity.this);
                    imAction.setLayoutParams(tp);
                    imAction.setPadding(2, 2, 2, 2);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        imAction.setBackground(SyncActivity.this.getResources().getDrawable(R.drawable.border));
                    } else {
                        imAction.setBackgroundDrawable(SyncActivity.this.getResources().getDrawable(R.drawable.border));
                    }
                    tr.addView(imAction);
                    syncTable.addView(tr);

                    imAction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mp.start();
                            if (new Common().isNetworkAvailable(act)) {
                                pd.setMessage("Uploading...");
                                pd.setIndeterminate(true);
                                pd.show();
                                pd.setCancelable(false);
                                callUpload(jo, tr);
                            } else {
                                Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            } else {
                final TableRow tr = new TableRow(SyncActivity.this);
                TableRow.LayoutParams tp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                tp.weight = 1;
                TextView tvID = new TextView(SyncActivity.this);
                tvID.setText("No Data.");
                Common.setRow(tp, tvID, tr, SyncActivity.this);
                syncTable.addView(tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("setSyncTable SyncActivity : " + e.getMessage());
                uploadException(e.getMessage(), "setSyncTable", "DWP S SyncActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Toast.makeText(SyncActivity.this, "Error occurred while setting layout.", Toast.LENGTH_SHORT).show();
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
                    JSONArray tpja = new JSONArray(text.toString());
                    int jl = tpja.length();
                    if (jl > 0) {
                        int syln = syncArray.length();
                        for (int a = 0; a < jl; a++) {
                            JSONObject tjo = tpja.getJSONObject(a);
                            Log.d("tjo = ", "" + tjo.toString());
//                            syncArray.put(tjo);
                            if (syln > 0) {
                                for (int m = 0; m < syln; m++) {//id,planid,Printno,Latitude,Longitude
                                    JSONObject sjo = syncArray.getJSONObject(m);
                                    if (tjo.optString("id").equals(sjo.optString("id")) && tjo.optString("planid").equals(sjo.optString("planid")) &&
                                            tjo.optString("Printno").equals(sjo.optString("Printno"))) {
                                        Log.d("duplicate ", "json");
                                    } else {
                                        syncArray.put(tjo);
                                    }
                                }
                            } else {
                                syncArray.put(tjo);
                            }
                        }
                    }
                } catch (Exception e) {
                    saveErrorFile("readFile SyncActivity : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            saveErrorFile("readFile SyncActivity : " + e.getMessage());
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

    public void saveAccessLog(String res) {
        try {
            File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/DWPSLog.txt");
//            if (myDir.exists()) {
//
//                if (myDir.delete()) {
//                    Log.d("inside myDir", "deleted");
//                }
//            }

            FileWriter fw;
            BufferedWriter bw = null;
            String content = res.toString();
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

    public void callUpload(JSONObject jo, TableRow tr) {
        try {

            String localId = "";
//            if (syncArray.length() > 0) {
//                JSONObject jo = syncArray.getJSONObject(0);
            uploadArray = new JSONArray();
            JSONObject jo1 = new JSONObject();
            localId = jo.optString("localid");
            Log.d("local id upload = ", localId);
            jo1.put("id", jo.optString("id"));
            jo1.put("Planid", jo.optString("Planid"));
            jo1.put("VillageCode", jo.optString("VillageCode"));
            jo1.put("Remark", jo.optString("Remark"));

//            jo1.put("NearImage", jo.optString("NearImage"));
//            jo1.put("FarImage", jo.optString("FarImage"));

            if (jo.optString("NearImage").toLowerCase().contains("DWPainting".toLowerCase())) {
                Log.d("NearImage", "contains(\"DWPainting\"");
                jo1.put("NearImage", imageToString(jo.optString("NearImage")));
            } else {
                jo1.put("NearImage", jo.optString("NearImage"));
                Log.d("NearImage", "not contains(\"DWPainting\"");
            }
            if (jo.optString("FarImage").toLowerCase().contains("DWPainting".toLowerCase())) {
                jo1.put("FarImage", imageToString(jo.optString("FarImage")));
                Log.d("FarImage", "contains(\"DWPainting\"");
            } else {
                jo1.put("FarImage", jo.optString("FarImage"));
                Log.d("FarImage", " not contains(\"DWPainting\"");
            }

            jo1.put("ExecutionDate", jo.optString("ExecutionDate"));
            jo1.put("UploadDate", "" + Common.getDate());
            jo1.put("Printno", "" + jo.optString("Printno"));
            jo1.put("imei", Common.getImei(act));
            uploadArray.put(jo1);
//                syncArray.remove(position);
            Log.d("syncarray length = ", "" + syncArray.length());
            Log.d("uploadarray = ", "" + uploadArray.toString());
            uploadData(localId, tr);
//            }
        } catch (Exception e) {

            e.printStackTrace();
            try {
                saveErrorFile("callUpload SyncActivity : " + e.getMessage());
                uploadException(e.getMessage(), "callUpload", "DWP S SyncActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void uploadData1(final String localId, final TableRow tr) {
        try {
            if (new Common().isNetworkAvailable(act)) {
                pd.setMessage("Uploading");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                final JSONObject chkObj = uploadArray.getJSONObject(0);
                File nearFile = null, farFile = null;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    nearFile = new File(chkObj.optString("NearImage"));
                    farFile = new File(chkObj.optString("FarImage"));
                } else {
                    Uri tempURI = Uri.parse(chkObj.optString("NearImage"));
                    nearFile = new File(tempURI.getPath());
                    tempURI = Uri.parse(chkObj.optString("FarImage"));
                    farFile = new File(tempURI.getPath());
                }
                if (nearFile.exists() && farFile.exists()) {
                    int compressionRatio = 2; //1 == originalImage, 2 = 50% compression, 4=25% compress
                    try {
                        Bitmap nearbitmap = BitmapFactory.decodeFile(nearFile.getPath());
                        Bitmap nearbmp1 = Common.getProperImage(nearbitmap, nearFile.getPath());
                        nearbmp1.compress(Bitmap.CompressFormat.JPEG, 40, new FileOutputStream(nearFile));

                        Bitmap farbitmap = BitmapFactory.decodeFile(farFile.getPath());
                        Bitmap farbmp1 = Common.getProperImage(farbitmap, farFile.getPath());
                        farbmp1.compress(Bitmap.CompressFormat.JPEG, 40, new FileOutputStream(farFile));
                    } catch (Throwable t) {
                        Log.e("ERROR", "Error compressing file." + t.toString());
                        t.printStackTrace();
                    }
                    RequestBody serverid = RequestBody.create(parse("text/plain"), chkObj.getString("id"));
                    RequestBody planid = RequestBody.create(parse("text/plain"), chkObj.getString("PlanID"));
                    RequestBody villageCode = RequestBody.create(parse("text/plain"), chkObj.getString("VillageCode"));
                    RequestBody printNotxt = RequestBody.create(parse("text/plain"), chkObj.getString("Printno"));
                    RequestBody remks = RequestBody.create(parse("text/plain"), chkObj.getString("Printno"));
                    RequestBody execDate = RequestBody.create(parse("text/plain"), chkObj.getString("Printno"));
                    RequestBody upldDate = RequestBody.create(parse("text/plain"), chkObj.getString("Printno"));
                    RequestBody imei = RequestBody.create(parse("text/plain"), Common.getImei(act));

                    MultipartBody.Part nearFilePart = MultipartBody.Part.createFormData("file", nearFile.getName(), RequestBody.create(parse("*/*"), nearFile));
                    MultipartBody.Part farFilePart = MultipartBody.Part.createFormData("file", farFile.getName(), RequestBody.create(parse("*/*"), farFile));
                    Call<ResponseObj> call1 = apiInterface.uploadImages(nearFilePart, farFilePart, serverid, planid, villageCode, remks, execDate, upldDate, imei, printNotxt);
                    call1.enqueue(new Callback<ResponseObj>() {
                        @Override
                        public void onResponse(Call<ResponseObj> call, retrofit2.Response<ResponseObj> response1) {
                            try {
                                Log.d("server response", "" + response1.toString());
                                ResponseObj res = response1.body();
                                JSONObject jobj = new JSONObject();
                                if (res == null) {
                                    res.setID("");
                                    res.setMessage("Fail");
                                    res.setPlanID("");
                                    res.setPrintNo("");
                                    res.setStatus("Response Error");
                                    res.setStatusCode("301");
                                }
                                jobj.put("Status", "" + res.getStatus());
                                jobj.put("PlanID", "" + res.getPlanID());
                                jobj.put("PrintNo", "" + res.getPrintNo());
                                jobj.put("ID", "" + res.getID());
                                jobj.put("Message", "" + res.getMessage());
                                jobj.put("Status_Code", "" + res.getStatusCode());
                                pd.dismiss();
                                saveAccessLog("Response : " + jobj.toString() + " for PrintNo : " + chkObj.getString("Printno") + ", PlanID : " + chkObj.getString("PlanID") + ", ServerID : " + chkObj.getString("id") + ", imei : " + Common.getImei(act));
                                if (jobj.optString("Status_Code").equals("115")) {
                                    db = new DatabaseHelper(act);
                                    boolean isUpdated = db.updateDataFlag(localId, "1");
                                    Log.d("updateDataFlag ", "" + isUpdated);
                                    db.close();
                                    pd.dismiss();
                                    Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                    syncTable.removeView(tr);
                                } else {
                                    if (jobj.optString("Status_Code").equals("116")) {
                                        Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
                                    } else if (jobj.optString("Status_Code").equals("117")) {
                                        Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
                                    } else if (jobj.optString("Status_Code").equals("118") || jobj.optString("Status_Code").equals("118")) {
                                        Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
                                    }
                                    pd.dismiss();
                                }
                            } catch (Exception e) {
                                saveErrorFile("saveErrorFile(\"onResponse SyncActivity : \" + e.getMessage()); uploadData1 SyncActivity : " + e.getMessage());
                                e.printStackTrace();
                                Log.d("exception occured", "" + e.toString());
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseObj> call, Throwable t) {
                            Log.d("inside retro", "" + t.getMessage());
                            pd.dismiss();
                            call.cancel();
                            try {
                                pd.dismiss();
                                Toast.makeText(act, "Network error. Or Server Error," + t.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    try {
                                        saveErrorFile("onErrorResponse uploadData1 SyncActivity : " + t.getMessage() );
                                        uploadException(t.getMessage() , "uploadData1 VolleyError", "DWP S MainActivity");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }

                            } catch (NullPointerException e) {
                                Log.d("error:->", "NullPointerException " + t.getMessage());
                            }
                        }
                    });
                } else {
                    Toast.makeText(act, "Image not found or deleted for " + chkObj.optString("Printno"), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            } else {
                Toast.makeText(act, "No internet connection.\n" +
                        "", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadData(final String localId, final TableRow tr) {
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
                pd.setMessage("Uploading");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                final JSONObject chkObj = uploadArray.getJSONObject(0);

                if (!(chkObj.optString("NearImage").equals(""))
                        && !(chkObj.optString("FarImage").equals(""))) {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, new Common().site + "postSupervisorDetails",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Log.d("server response", "" + response);
                                        if (response == null || response.trim().isEmpty() || response.trim().equals("")) {
                                            response = "{\"Status\":\"Response Error\",\"Message\":\"Fail\",\"Status_Code\":\"301\"}";
                                        }
                                        if (!(isJSONValid(response.trim()))) {
                                            response = "{\"Status\":\"Response Error\",\"Message\":\"Fail\",\"Status_Code\":\"301\"}";
                                        }
                                        JSONObject jobj = new JSONObject(response.trim());
                                        saveAccessLog("Response : " + response + " for PrintNo : " + chkObj.optString("Printno") + ", PlanID : " + chkObj.optString("Planid") + ", ServerID : " + chkObj.optString("id") + ", imei : " + Common.getImei(act));
                                        if (jobj.optString("Status_Code").equals("115")) {
                                            db = new DatabaseHelper(act);
                                            boolean isUpdated = db.updateDataFlag(localId, "1");
                                            //                                    boolean isUpdated1 = db.updatePrintFlag(printid,"1");
                                            Log.d("updateDataFlag ", "" + isUpdated);
                                            db.close();
                                            pd.dismiss();
                                            Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                            syncTable.removeView(tr);
                                        } else {
                                            //                                    Toast.makeText(act, "Entry " + position + " upload " +jobj.optString("Message") + " status code = " + jobj.optString("Status_Code"), Toast.LENGTH_SHORT).show();
                                            if (jobj.optString("Status_Code").equals("116")) {
                                                Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
//                                                db.deleteSingleData(chkObj.optString("id"));
                                            } else if (jobj.optString("Status_Code").equals("117")) {
                                                Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
//                                                db.deleteSingleData(chkObj.optString("id"));
                                            } else if (jobj.optString("Status_Code").equals("118") || jobj.optString("Status_Code").equals("118")) {
                                                Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
//                                                db.deleteSingleData(chkObj.optString("id"));
                                            } else {
                                                Toast.makeText(act, "error code : " + jobj.optString("Status_Code") + ", " + jobj.optString("Message") + " for print no: " + chkObj.optString("Printno"), Toast.LENGTH_LONG).show();
//                                                db.deleteSingleData(chkObj.optString("id"));
                                            }

                                            pd.dismiss();
//                                                fileArray.put(uploadArray.getJSONObject(0));
//                                                Toast.makeText(act, "Some entries may have not been uploaded.", Toast.LENGTH_SHORT).show();


                                        }
                                    } catch (Exception e) {
                                        saveErrorFile("saveErrorFile(\"onResponse SyncActivity : \" + e.getMessage()); uploadData SyncActivity : " + e.getMessage());
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

                                        pd.dismiss();

//                                        position = 0;
                                        try {
                                            byte[] htmlBodyBytes = error.networkResponse.data;
                                            Log.e("", "" + new String(htmlBodyBytes), error);
                                            Toast.makeText(act, "Html error = " + new String(htmlBodyBytes), Toast.LENGTH_SHORT).show();
                                            try {
                                                saveErrorFile("onErrorResponse uploadData SyncActivity : " + error.getMessage() + ", " + new String(htmlBodyBytes));
                                                uploadException(error.getMessage() + ", " + new String(htmlBodyBytes), "uploadData VolleyError", "DWP S MainActivity");
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
                    Toast.makeText(act, "Image not found or deleted for " + chkObj.optString("Printno"), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    saveErrorFile("uploadData SyncActivity : " + e.getMessage());
                    uploadException(e.getMessage(), "uploadData", "DWP S MainActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
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
                    Log.d("M : file exists", " yes");
//                    Bitmap bitmap = new CompressImage().getBitmapFromFile(file);
                    Bitmap bitmap = new ConvertImage().decodeFile(path);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
                    byte[] byteArray = stream.toByteArray();
                    imageText = Base64.encodeToString(byteArray, Base64.DEFAULT);
                } else {
                    Log.d("M : file exists", " no");
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
                    Log.d("N : file exists", " yes");
//                    Bitmap bitmap = new CompressImage().getBitmapFromFile(file);
                    Bitmap bitmap = new ConvertImage().decodeFile(tempURI.getPath());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
                    byte[] byteArray = stream.toByteArray();
                    imageText = Base64.encodeToString(byteArray, Base64.DEFAULT);
                } else {
                    Log.d("N : file exists", " no");
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
                saveErrorFile("imageToString SyncActivity : " + e.getMessage());
                uploadException(e.getMessage(), "imageToString", "DWP S SyncActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return imageText;
    }

    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private void uploadException(final String message, final String fname, final String executeActivity) {
        try {
            if (new Common().isNetworkAvailable(act)) {
                try {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, new Common().site + "CatchException",
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
