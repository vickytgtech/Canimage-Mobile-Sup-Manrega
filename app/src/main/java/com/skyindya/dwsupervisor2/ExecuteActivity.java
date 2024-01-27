package com.skyindya.dwsupervisor2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.constraintlayout.*;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static okhttp3.MediaType.parse;

public class ExecuteActivity extends AppCompatActivity {

    TextView villageName, brand, width, height, printNo, selectremark;
    MultiSelectionSpinner remark;
    ImageView postNearPic, postFarPic;
    Button submit;
    boolean isNear, isFar;
    String IMAGE_DIRECTORY_NAME = "/DWPainting";
    private final int nearCode = 23, farCode = 24;
    String nearPath = "", farPath = "", id = "", tempNearPath = "", tempFarPath = "";
    String nearText = "", farText = "", nearExt = "", farExt = "";
    Uri tempURI;
    Activity act;
    private GPSTracker gps;
    private ProgressDialog pd;
    private DatabaseHelper db;
    JSONArray uploadJArray;
    JSONObject printObj;
    ArrayList<String> remarksList, remarkIdList;
    String remarks = "";
    String mCurrentPhotoPath = "";
    MediaPlayer mp;
    APIInterface apiInterface;


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("tempuri", "" + tempURI + "\nrequestCode:" + requestCode);
        Log.d("inside", "imageCapture");
        if (resultCode == RESULT_OK) {
            try {
                switch (requestCode) {
                    case nearCode:
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                            nearPath = tempURI.getPath();
                        showImage(nearCode, nearPath);
                        break;
                    case farCode:
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                            farPath = tempURI.getPath();
                        showImage(farCode, farPath);
                        break;
                }
            } catch (Exception e) {
                saveErrorFile("onActivityResult ExecuteActivity : " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            if (requestCode == nearCode) {
                nearPath = tempNearPath;
            } else if (requestCode == farCode) {
                farPath = tempFarPath;
            }
            Log.d("tempNearPath", "" + tempNearPath + "\n" + tempNearPath);
            Log.d("tempFarPath", "" + tempFarPath);
            Log.d("nearPath", "" + nearPath + "\n" + tempNearPath);
            Log.d("farPath", "" + farPath + "\n" + nearPath);
            Log.d("capture ", "cancel");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);
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
            act = this;
            mp = MediaPlayer.create(act, R.raw.tone);
            gps = new GPSTracker(act);
            pd = new ProgressDialog(act, R.style.AppTheme_Dark_Dialog);
            printObj = new JSONObject(getIntent().getStringExtra("json"));
            selectremark = (TextView) findViewById(R.id.selectremark);
            villageName = (TextView) findViewById(R.id.villageName);
            brand = (TextView) findViewById(R.id.brand);
            width = (TextView) findViewById(R.id.width);
            height = (TextView) findViewById(R.id.height);
            printNo = (TextView) findViewById(R.id.printNo);
            remark = (MultiSelectionSpinner) findViewById(R.id.remark);
            postNearPic = (ImageView) findViewById(R.id.nearPic);
            postFarPic = (ImageView) findViewById(R.id.farPic);
            submit = (Button) findViewById(R.id.submit);

            apiInterface = APIClient.getClient().create(APIInterface.class);

            if (savedInstanceState != null) {

                nearPath = savedInstanceState.getString("nearPath");
                if (nearPath != null && !nearPath.equals(""))
                    showImage(nearCode, nearPath);
                farPath = savedInstanceState.getString("farPath");
                if (farPath != null && !farPath.equals(""))
                    showImage(farCode, farPath);
                if (savedInstanceState.getString("printObj") != null)
                    printObj = new JSONObject(savedInstanceState.getString("printObj"));

            }

            db = new DatabaseHelper(act);
            Cursor res = db.getVillageName(printObj.getString("VillageCode"));
            if (res.getCount() > 0) {
                res.moveToNext();
                villageName.setText(res.getString(3));
            }
            res.close();
            Cursor cus = db.viewRemarks(printObj.getString("projectcode"));
            remarksList = new ArrayList<String>();
            remarkIdList = new ArrayList<String>();
            if (cus.getCount() > 0) {

                while (cus.moveToNext()) {
                    remarksList.add(cus.getString(2));
                    remarkIdList.add(cus.getString(1));
                }
            } else {
                remarksList.add("Select Remarks");
            }
            remark.setItems(remarksList);

//            remarksList = new ArrayList<String>();
//            remarksList.add("Not proper");
//            remarksList.add("Bad site selection");
//            remarksList.add("Near by prints");
//            remarksList.add("Not proper");

            cus.close();
            db.close();
            id = printObj.getString("id");
            brand.setText(printObj.getString("brand"));
            width.setText(printObj.getString("Width"));
            height.setText(printObj.getString("Height"));
            printNo.setText(printObj.getString("Printno"));

            remark.setListener(new MultiSelectionSpinner.OnMultipleItemsSelectedListener() {
                @Override
                public void selectedIndices(List<Integer> indices) {
                    try {
                        if (indices.size() > 0) {
                            int sz = indices.size();
                            for (int i = 0; i < sz; i++) {
                                if (remarkIdList.size() > 0) {
                                    if (i == (sz - 1))
                                        remarks = remarks + remarkIdList.get(indices.get(i));
                                    else
                                        remarks = remarks + remarkIdList.get(indices.get(i)) + ",";
                                } else {
                                    break;
                                }
                            }
                            selectremark.setVisibility(View.GONE);
                        } else {
                            remarks = "";
                            selectremark.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        saveErrorFile("remark.setListener onCreate ExecuteActivity : " + e.getMessage());
                        e.printStackTrace();
                    }
//                    Toast.makeText(act, remarks, Toast.LENGTH_LONG).show();
                }

                @Override
                public void selectedStrings(List<String> strings) {
//                    Toast.makeText(act, strings.toString(), Toast.LENGTH_LONG).show();
                }
            });

            postNearPic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        openCamera(nearCode);
                    } catch (Exception e) {
                        saveErrorFile("postNearPic onCreate ExecuteActivity : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            postFarPic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        openCamera(farCode);
                    } catch (Exception e) {
                        saveErrorFile("postFarPic onCreate ExecuteActivity : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mp.start();
                    if (remarks.equals("")) {
                        Toast.makeText(act, "Please select Remarks", Toast.LENGTH_SHORT).show();
                    } else if (!isNear) {
                        Toast.makeText(act, "Please click picture of print from near", Toast.LENGTH_SHORT).show();
                    } else if (!isFar) {
                        Toast.makeText(act, "Please click picture of print from far", Toast.LENGTH_SHORT).show();
                    } else {
                        submit.setEnabled(false);
                        updateData();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("onCreate ExecuteActivity : " + e.getMessage());
                uploadException(e.getMessage(), "onCreate", "DWP S ExecuteActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

    }

    public Uri getOutputMediaFileUri(int type, int code) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return Uri.fromFile(getOutputMediaFile(type, code));
        } else {
            return FileProvider.getUriForFile(act,
                    BuildConfig.APPLICATION_ID + ".provider",
                    getOutputMediaFile(type, code));
        }
    }

    private File getOutputMediaFile(int type, int code) {
        if (code == nearCode)
            tempNearPath = nearPath;
        else if (code == farCode)
            tempFarPath = farPath;
        // External sdcard location
        File imgDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!imgDir.exists()) {
            if (!imgDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(imgDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                mCurrentPhotoPath = "file:" + mediaFile.getAbsolutePath();
                if (code == nearCode)
                    nearPath = mCurrentPhotoPath;
                else if (code == farCode)
                    farPath = mCurrentPhotoPath;
            }
//            path = Environment.getExternalStorageDirectory().getAbsolutePath() + IMAGE_DIRECTORY_NAME + File.separator
//                    + "IMG_" + timeStamp + ".jpg";
        } else {
            return null;
        }

        return mediaFile;
    }

    public void showImage(int code, String path) {
        if (path != null || !(path.equals(""))) {
            try {
                Bitmap bigBitmap;
                ConvertImage ci = new ConvertImage();
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
//                    bigBitmap = ci.decodeFile(path);
                    //resizing bitmap to load in view
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 6;//calculateInSampleSize(options, 500,500);
                    Log.d("insample size = ", "" + options.inSampleSize);
                    options.inJustDecodeBounds = false;
                    //BitmapFactory.decodeFile(path, options);

                    bigBitmap = BitmapFactory.decodeFile(path, options);//ci.decodeFile(path);
                    if (code == nearCode) {
                        postNearPic.setImageBitmap(bigBitmap);
                        isNear = true;
                        nearText = imageToString(path);
                    } else if (code == farCode) {
                        postFarPic.setImageBitmap(bigBitmap);
                        isFar = true;
                        farText = imageToString(path);
                    }
                } else {
                    path = path.replace("file:", "");
                    Uri tempURI = Uri.parse(path);
                    //resizing bitmap to load in view
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 6;//calculateInSampleSize(options, 500,500);
                    Log.d("insample size = ", "" + options.inSampleSize);
                    options.inJustDecodeBounds = false;
                    BitmapFactory.decodeFile(tempURI.getPath(), options);
                    bigBitmap = BitmapFactory.decodeFile(tempURI.getPath(), options);//ci.decodeFile(tempURI.getPath());
                    if (code == nearCode) {
//                        setPic(postNearPic, path);
                        postNearPic.setImageBitmap(bigBitmap);
                        isNear = true;
                        nearText = imageToString(path);
                    } else if (code == farCode) {
//                        setPic(postFarPic, path);
                        postFarPic.setImageBitmap(bigBitmap);
                        isFar = true;
                        farText = imageToString(path);
                    }
                }

            } catch (Exception e) {
                saveErrorFile("showImage ExecuteActivity : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void openCamera(int captureCode) {
        try {
            tempURI = getOutputMediaFileUri(MEDIA_TYPE_IMAGE, captureCode);
            Intent photoCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            photoCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempURI);
            startActivityForResult(photoCaptureIntent, captureCode);
        } catch (Exception e) {
            Log.d("camera error :::", " " + e.toString());
            e.printStackTrace();
            try {
                saveErrorFile("openCamera ExecuteActivity : " + e.getMessage());
                uploadException(e.getMessage(), "openCamera", "DWP S ExecuteActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Toast.makeText(act, "Error ocuured while opening camera.", Toast.LENGTH_SHORT).show();
        }
    }

    public String imageToString(String path) {

        String imageText = "";
        try {

//        fileName = FilenameUtils.removeExtension(FilenameUtils.getName(path));
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                File file = new File(path);
//                Bitmap bitmap = new CompressImage().getBitmapFromFile(file);
                Bitmap bitmap = new ConvertImage().decodeFile(path);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
                byte[] byteArray = stream.toByteArray();
                imageText = Base64.encodeToString(byteArray, Base64.DEFAULT);
            } else {
                tempURI = Uri.parse(path);
                File file = new File(tempURI.getPath());
//                Bitmap bitmap = new CompressImage().getBitmapFromFile(file);
                Bitmap bitmap = new ConvertImage().decodeFile(tempURI.getPath());
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
                byte[] byteArray = stream.toByteArray();
                imageText = Base64.encodeToString(byteArray, Base64.DEFAULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("imageToString ExecuteActivity : " + e.getMessage());
                uploadException(e.getMessage(), "imageToString", "DWP S ExecuteActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        return imageText;
    }

    public void updateData() {
        try {
            final double lat = gps.getLatitude();
            final double lng = gps.getLongitude();
            db = new DatabaseHelper(act);
            uploadJArray = new JSONArray();
            JSONObject jo = new JSONObject();
            jo.put("id", printObj.getString("id"));
            jo.put("Planid", printObj.getString("PlanID"));
            jo.put("VillageCode", printObj.getString("VillageCode"));
            jo.put("Remark", remarks);
            jo.put("NearImage", nearText);
            jo.put("FarImage", farText);
            jo.put("ExecutionDate", "" + Common.getDate());
            jo.put("UploadDate", "" + Common.getDate());
            jo.put("imei", Common.getImei(act));
            uploadJArray.put(jo);
            Log.d("uploadJArray", "" + uploadJArray.toString());
            JSONObject tempJo = new JSONObject();
            tempJo.put("id", printObj.getString("id"));
            tempJo.put("Planid", printObj.getString("PlanID"));
            tempJo.put("VillageCode", printObj.getString("VillageCode"));
            tempJo.put("Remark", remarks);
            tempJo.put("NearImage", nearPath);
            tempJo.put("FarImage", farPath);
            tempJo.put("ExecutionDate", "" + Common.getDate());
            tempJo.put("UploadDate", "" + Common.getDate());
            tempJo.put("imei", Common.getImei(act));
            savePrints(tempJo);
            long res = db.addData(printObj.getString("id"), printObj.getString("PlanID"), printObj.getString("VillageCode"), remarks, nearPath, farPath, "" + lat, "" + lng, "", Common.getDate(), printObj.getString("Printno"));
            if (res != -1) {
                db.updateBalance(printObj.getString("PlanID"), printObj.getString("VillageCode"));
                boolean isUpdated1 = db.updatePrintFlag(printObj.getString("localid"), "1");
                if (new Common().isNetworkAvailable(act)) {
                    uploadData("" + res);
                } else {
                    Intent i = new Intent(act, MainActivity.class);
                    startActivity(i);
//                    finish();
                }
            } else {
                Toast.makeText(act, "Error occurred while adding data.", Toast.LENGTH_SHORT).show();
            }

            db.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("updateData ExecuteActivity : " + e.getMessage());
                uploadException(e.getMessage(), "updateData", "DWP S ExecuteActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

    }

    public void uploadData(final String id) {
//                File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/DWPSJson.txt");
//        if (myDir.exists()) {
//
//            if (myDir.delete()) {
//                Log.d("inside myDir", "deleted");
//            }
//        }
//        try {
//            FileWriter fw;
//            BufferedWriter bw = null;
//            String content = uploadJArray.toString();
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
                pd.setMessage("Uploading...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                StringRequest stringRequest = new StringRequest(Request.Method.POST, Common.site + "postSupervisorDetails",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    pd.dismiss();
                                    Log.d("server response", "" + response);
                                    if (response == null || response.trim().isEmpty() || response.trim().equals("")) {
                                        response = "{\"Status\":\"Response Error\",\"Message\":\"Fail\",\"Status_Code\":\"301\"}";
                                    }
                                    if (!(isJSONValid(response.trim()))) {
                                        response = "{\"Status\":\"Response Error\",\"Message\":\"Fail\",\"Status_Code\":\"301\"}";
                                    }
                                    saveAccessLog("Response : " + response + " for PrintNo : " + printObj.getString("Printno") + ", PlanID : " + printObj.getString("PlanID") + ", ServerID : " + printObj.getString("id")+", imei : "+Common.getImei(act));
                                    JSONObject jobj = new JSONObject(response.trim());
                                    if (jobj.getString("Status_Code").equals("115")) {
                                        db = new DatabaseHelper(act);
                                        boolean isUpdated = db.updateDataFlag(id, "1");

                                        db.close();
                                        Log.d("flag updated", "" + isUpdated);
                                        Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                        printNo.setText("");
                                        isFar = false;
                                        isNear = false;
                                        postNearPic.setImageBitmap(null);
                                        postFarPic.setImageBitmap(null);
                                        Intent i = new Intent(act, MainActivity.class);
                                        startActivity(i);
//                                        finish();
                                        //
                                    } else {
                                        if (jobj.getString("Status_Code").equals("116")) {
                                            Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
//                                            db.deleteSingleData(id);
                                        } else if (jobj.getString("Status_Code").equals("117")) {
                                            Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
//                                            db.deleteSingleData(id);
                                        } else if (jobj.getString("Status_Code").equals("118") || jobj.getString("Status_Code").equals("118")) {
                                            Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
//                                                db.deleteSingleData(chkObj.getString("id"));
//                                            fileArray.put(uploadArray.getJSONObject(0));
                                        } else {
                                            Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
//                                            db.deleteSingleData(id);
                                        }
//                                        Toast.makeText(act, "Error Occured while uploading.\n"+jobj.getString("Message"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    saveErrorFile("onErrorResponse uploadData ExecuteActivity : " + e.getMessage());
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
                                    saveErrorFile("onErrorResponse uploadData ExecuteActivity : " + error.getMessage());
                                    Toast.makeText(act, "Network error or Server error." + error.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    Log.d("error:->", "NullPointerException " + error.getMessage());
                                }
                                finish();
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("json", uploadJArray.toString());
                        return map;
                    }
                };
                VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    saveErrorFile("onErrorResponse ExecuteActivity : " + e.getMessage());
                    uploadException(e.getMessage(), "uploadData", "DWP S ExecuteActivity");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {

            Toast.makeText(act, "No internet connection.\n" +
                    "", Toast.LENGTH_SHORT).show();
        }
    }

    public void uploadData1(final String id)
    {
        try {
            if (new Common().isNetworkAvailable(act)) {
                pd.setMessage("Uploading...");
                pd.setIndeterminate(true);
                pd.show();
                pd.setCancelable(false);
                File nearFile = null,farFile = null;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    nearFile = new File(nearPath);
                    farFile = new File(farPath);
                }
                else
                {
                    tempURI = Uri.parse(nearPath);
                    nearFile = new File(tempURI.getPath());
                    tempURI = Uri.parse(farPath);
                    farFile = new File(tempURI.getPath());
                }
                int compressionRatio = 2; //1 == originalImage, 2 = 50% compression, 4=25% compress
                try {
                    Bitmap nearbitmap = BitmapFactory.decodeFile (nearFile.getPath ());
                    Bitmap nearbmp1 = Common.getProperImage(nearbitmap, nearFile.getPath ());
                    nearbmp1.compress (Bitmap.CompressFormat.JPEG, 40, new FileOutputStream(nearFile));

                    Bitmap farbitmap = BitmapFactory.decodeFile (farFile.getPath ());
                    Bitmap farbmp1 = Common.getProperImage(farbitmap, farFile.getPath ());
                    farbmp1.compress (Bitmap.CompressFormat.JPEG, 40, new FileOutputStream(farFile));
                }
                catch (Throwable t) {
                    Log.e("ERROR", "Error compressing file." + t.toString ());
                    t.printStackTrace ();
                }
                RequestBody serverid = RequestBody.create(parse("text/plain"), printObj.getString("id"));
                RequestBody planid = RequestBody.create(parse("text/plain"), printObj.getString("PlanID"));
                RequestBody villageCode = RequestBody.create(parse("text/plain"), printObj.getString("VillageCode"));
                RequestBody printNotxt = RequestBody.create(parse("text/plain"), printObj.getString("Printno"));
                RequestBody remks = RequestBody.create(parse("text/plain"), remarks);
                RequestBody execDate = RequestBody.create(parse("text/plain"), Common.getDate());
                RequestBody upldDate = RequestBody.create(parse("text/plain"), Common.getDate());
                RequestBody imei = RequestBody.create(parse("text/plain"), Common.getImei(act));

                MultipartBody.Part nearFilePart = MultipartBody.Part.createFormData("file", nearFile.getName(), RequestBody.create(parse("*/*"), nearFile));
                MultipartBody.Part farFilePart = MultipartBody.Part.createFormData("file", farFile.getName(), RequestBody.create(parse("*/*"), farFile));
                Call<ResponseObj> call1 = apiInterface.uploadImages(nearFilePart,farFilePart,serverid,planid,villageCode,remks,execDate,upldDate,imei,printNotxt);
                call1.enqueue(new Callback<ResponseObj>() {
                    @Override
                    public void onResponse(Call<ResponseObj> call, retrofit2.Response<ResponseObj> response1) {
                        try {
                            Log.d("server response", "" + response1.toString());
                            ResponseObj res = response1.body();
                            JSONObject jobj = new JSONObject();
                            if(res == null)
                            {
                                res.setID("");
                                res.setMessage("Fail");
                                res.setPlanID("");
                                res.setPrintNo("");
                                res.setStatus("Response Error");
                                res.setStatusCode("301");
                            }
                            jobj.put("Status",""+res.getStatus());
                            jobj.put("PlanID",""+res.getPlanID());
                            jobj.put("PrintNo",""+res.getPrintNo());
                            jobj.put("ID",""+res.getID());
                            jobj.put("Message",""+res.getMessage());
                            jobj.put("Status_Code",""+res.getStatusCode());
                            pd.dismiss();
                            saveAccessLog("Response : " + jobj.toString() + " for PrintNo : " + printObj.getString("Printno") + ", PlanID : " + printObj.getString("PlanID") + ", ServerID : " + printObj.getString("id")+", imei : "+Common.getImei(act));
                            if (jobj.getString("Status_Code").equals("115")) {
                                db = new DatabaseHelper(act);
                                boolean isUpdated = db.updateDataFlag(id, "1");

                                db.close();
                                Log.d("flag updated", "" + isUpdated);
                                Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                printNo.setText("");
                                isFar = false;
                                isNear = false;
                                postNearPic.setImageBitmap(null);
                                postFarPic.setImageBitmap(null);
                                finish();
                                //
                            } else {
                                if (jobj.getString("Status_Code").equals("116")) {
                                    Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
    //                                            db.deleteSingleData(id);
                                } else if (jobj.getString("Status_Code").equals("117")) {
                                    Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
    //                                            db.deleteSingleData(id);
                                } else if (jobj.getString("Status_Code").equals("118") || jobj.getString("Status_Code").equals("118")) {
                                    Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
    //                                                db.deleteSingleData(chkObj.getString("id"));
    //                                            fileArray.put(uploadArray.getJSONObject(0));
                                } else {
                                    Toast.makeText(act, "error code : " + jobj.getString("Status_Code") + ", " + jobj.getString("Message"), Toast.LENGTH_LONG).show();
    //                                            db.deleteSingleData(id);
                                }
    //                                        Toast.makeText(act, "Error Occured while uploading.\n"+jobj.getString("Message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseObj> call, Throwable t) {
                        Log.d("inside retro",""+t.getMessage());
                        pd.dismiss();
                        call.cancel();
                        Log.d("error:->", "error Occured" + t.getMessage());
                        try {
                            saveErrorFile("onErrorResponse uploadData ExecuteActivity : " + t.getMessage());
                            Toast.makeText(act, "Network error or Server error." + t.getMessage(), Toast.LENGTH_SHORT).show();
                        } catch (NullPointerException e) {
                            Log.d("error:->", "NullPointerException " + t.getMessage());
                        }
                        finish();
                    }
                });
            } else {
                Toast.makeText(act, "No internet connection.\n" +
                        "", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPic(ImageView iv, String path) {
        try {
            // Get the dimensions of the View
            int targetW = iv.getWidth();
            int targetH = iv.getHeight();

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            Uri ur = Uri.parse(path);
            BitmapFactory.decodeFile(ur.getPath(), bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeFile(ur.getPath(), bmOptions);
            iv.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                saveErrorFile("setPic ExecuteActivity : " + e.getMessage());
                uploadException(e.getMessage(), "setPic", "DWP S ExecuteActivity");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bd) {
        super.onSaveInstanceState(bd);
        bd.putString("nearPath", nearPath);
        bd.putString("farPath", farPath);
        bd.putString("printObj", printObj.toString());
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

    public void savePrints(JSONObject arr) {
        try {
            File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/AllPrintsSup.txt");
//            if (myDir.exists()) {
//
//                if (myDir.delete()) {
//                    Log.d("inside myDir", "deleted");
//                }
//            }
            FileWriter fw;
            BufferedWriter bw = null;
            String content = arr.toString();
            Log.d("content", "$$$$$$$" + content);
            fw = null;
            fw = new FileWriter(myDir, true);
            bw = new BufferedWriter(fw);
            bw.write("\n\n" + Calendar.getInstance().getTime() + " -- " + content);
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
