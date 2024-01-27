package com.skyindya.dwsupervisor2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableRow;
import android.widget.TextView;



/**
 * Created by skyindya on 8/16/2017.
 */

public class Common {


    ///Changes by brahma start

    public static String  DIRECTORY_NAME = "/DWPainting";
    static final private String ALPHABET = "0123456789";
    static final private Random rng = new SecureRandom();
    public static String uniqueId = "";
    ///end


    final String ip = "http://10.0.0.16/";
//    final static String site = "http://canimage.skyindya.co.in/WebServices/MobSetGetData.asmx/";
//    public static final String site2 = "http://canimage.skyindya.co.in/WebServices/MobSetGetData.asmx/";
//    public static final String site2 = "http://150.107.99.165:99/WebServices/MobSetGetData.asmx/";
//    public static final String site = "http://150.107.99.165:99/WebServices/MobSetGetData.asmx/";
    public static final String site2 = "http://103.224.6.71:99/WebServices/MobSetGetData.asmx/";
    public static final String site = "http://103.224.6.71:99/WebServices/MobSetGetData.asmx/";
//    public static final String site2 = "http://192.168.1.110:90/WebServices/MobSetGetData.asmx/";
//    public static final String site = "http://192.168.1.110:90/WebServices/MobSetGetData.asmx/";

//    public static String site = "http://192.168.1.17:81/WebServices/MobSetGetData.asmx/";

//    public static String getImei(Context context) {
//        TelephonyManager mngr = null;
//        try {
//            mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//            return mngr.getDeviceId();
//        } catch (SecurityException e) {
//            Log.d("getImei error ", e.toString());
//        }
//        return mngr.getDeviceId();
//    }

//    public static String getImei(Context context) {
//        String imei = "";
//        TelephonyManager mngr = null;
//        try {
//            mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                if (Build.VERSION.SDK_INT >= 26) {
//                    imei = mngr.getImei(0);
//                }
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    imei = mngr.getDeviceId(0);
//                } else {
//                    imei = mngr.getDeviceId();
//                }
//            }
//        } catch (SecurityException e) {
//            Log.d("getImei error ", e.toString());
//        }
//        return imei;
//    }

    public static String getImei(Context context) {
        String imei = "";
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                imei = getUniqueId(context);
            } else {
                imei = getImei1(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imei;
    }




    //changes by brahma start ---

    public static String getImei1(Context context) {
        String imei = "";
        TelephonyManager mngr = null;
        try {
            mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= 27) {
                    imei = mngr.getImei(0);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    imei = mngr.getDeviceId(0);
                } else {
                    imei = mngr.getDeviceId();
                }
            }
        } catch (SecurityException e) {
            Log.d("getImei error ", e.toString());
        }
        return imei;
    }

    static char randomChar(){
        return ALPHABET.charAt(rng.nextInt(ALPHABET.length()));
    }

    static String randomUUID(int length, int spacing, String spacerChar){
        StringBuilder sb = new StringBuilder();
        int spacer = 0;
        while(length > 0){
            if(spacer == spacing){
                sb.append(spacerChar);
                spacer = 0;
            }
            length--;
            spacer++;
            sb.append(randomChar());
        }
        return sb.toString();
    }

    public static String getUniqueId(Context context) {
        uniqueId = "";
        try {
            File imgDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + DIRECTORY_NAME);
            if (!imgDir.exists()) {
                if (!imgDir.mkdirs()) {
                    Log.d(DIRECTORY_NAME, "Oops! Failed create "
                            + DIRECTORY_NAME + " directory");
                }
            }

            File myDir = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/UniqueId.txt");
            if (myDir.exists()) {
                //verifyUser();
                readFile();
            }else {
                uniqueId = randomUUID(16,0,"");
                Log.d("unique",""+uniqueId);
                FileWriter fw;
                BufferedWriter bw = null;
                Log.d("content", "$$$$$$$" + uniqueId);
                fw = new FileWriter(myDir, true);
                bw = new BufferedWriter(fw);
                bw.write( uniqueId);
                bw.close();
                readFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uniqueId;
    }

    public static void readFile() {
        try {
            String jsonStr = "";

            File readFile = new File(Environment.getExternalStorageDirectory().toString() + "/DWPainting/UniqueId.txt");
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
                    uniqueId = text.toString();
                    Log.d("id",""+uniqueId);

                } catch (Exception e) {
                    e.printStackTrace();
                    // saveErrorFile(e.getMessage() + "\nreadFile" + "\nDWP P MainActivity");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //  saveErrorFile(e.getMessage() + "\nreadFile" + "\nDWP P MainActivity");
        }
    }



    //End





    public static String getDate() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String date1 = DATE_FORMAT.format(date);
        return date1;
    }

    public boolean isNetworkAvailable(Activity ac) {
        ConnectivityManager cm = (ConnectivityManager)
                ac.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static void setRow(TableRow.LayoutParams tp, TextView tv, TableRow tr, Activity act) {
        tv.setLayoutParams(tp);
        tv.setTextColor(act.getResources().getColor(R.color.black));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(2, 2, 2, 2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            tv.setBackground(act.getResources().getDrawable(R.drawable.border));
        }
        else
        {
            tv.setBackgroundDrawable(act.getResources().getDrawable(R.drawable.border));
        }
        tr.addView(tv);
    }

    public static void uploadData(final Activity act, final ProgressDialog pd, final JSONArray jsonArray,final long id) {
        if (new Common().isNetworkAvailable(act)) {
            pd.setMessage("Wait...");
            pd.setIndeterminate(true);
            pd.show();
            pd.setCancelable(false);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, new Common().site + "",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                pd.dismiss();
                                Log.d("server response", "" + response);
                                if (response.trim().equalsIgnoreCase("Success")) {
                                    Toast.makeText(act, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                    DatabaseHelper db = new DatabaseHelper(act);
                                    if(jsonArray.length()==1)
                                    {
                                        db.deleteSingleData(""+id);
                                    }
                                    else
                                    {
                                        db.deleteAllData();
                                    }
                                } else {
                                    Toast.makeText(act, "Error Occured while uploading.", Toast.LENGTH_SHORT).show();
                                }
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
                                Toast.makeText(act, "Network error.", Toast.LENGTH_SHORT).show();
                            } catch (NullPointerException e) {
                                Log.d("error:->", "NullPointerException " + error.getMessage());
                            }
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("json", jsonArray.toString());
                    return map;
                }
            };
            VolleySingleton.getInstance(act).addRequestQueue(stringRequest);
        } else {
            Toast.makeText(act, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }

    public static Bitmap getProperImage(Bitmap bitmap, String photoPath)
    {
        Bitmap rotatedBitmap = null;
        try {
            ExifInterface ei = new ExifInterface(photoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);


            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rotatedBitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

}
