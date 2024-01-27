package com.skyindya.dwsupervisor2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by SkyIndya-server on 11/2/2016.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASENAME = "dws";
    public static final String DATA = "executedata";
    public static final String PLANS = "plans";
    public static final String PRINTS = "prints";
    public static final String REMARKS = "remarks";
    public static final String PENDINGPLANS = "pendingplans";

    public DatabaseHelper(Context context) {
        super(context, DATABASENAME, null, 2);
        Log.d("inside", "DatabaseHelper ctr");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DATA + "(DATAID INTEGER PRIMARY KEY AUTOINCREMENT,SERVERID TEXT,SERVERPLANID TEXT," +
                "VILLAGECODE TEXT,REMARK TEXT," +
                "NEARIMAGE TEXT,FARIMAGE TEXT,LAT TEXT,LNG TEXT,ADDRESS TEXT,EXECUTIONDATE TEXT,PRINTNO TEXT,FLAG TEXT)");
        db.execSQL("create table " + PLANS + "(PLANID INTEGER PRIMARY KEY AUTOINCREMENT,SERVERPLANID TEXT,VILLAGECODE TEXT," +
                "VILLAGENAME TEXT,TEHSIL TEXT,DISTRICT TEXT,STATE TEXT,BALANCE TEXT,LAT TEXT,LNG TEXT,RANGE TEXT,PROJECT TEXT,PLANNAME TEXT)");
        db.execSQL("create table " + PRINTS + "(PRINTID INTEGER PRIMARY KEY AUTOINCREMENT,SERVERID TEXT,SERVERPLANID TEXT," +
                "VILLAGECODE TEXT,BRAND TEXT,WIDTH TEXT,HEIGHT TEXT,SQFT TEXT,LANGUAGE TEXT,PRINTNO TEXT,LAT TEXT,LNG TEXT," +
                "ADDRESS TEXT,FLAG TEXT,REMARKS TEXT,PROJECTCODE TEXT)");
        db.execSQL("create table " + REMARKS + "(REMARKID INTEGER PRIMARY KEY AUTOINCREMENT,SERVERID TEXT,TITLE TEXT,PROJECTCODE TEXT)");
        db.execSQL("create table " + PENDINGPLANS + "(PLANID INTEGER PRIMARY KEY AUTOINCREMENT,SERVERPLANID TEXT,VILLAGECODE TEXT," +
                "VILLAGENAME TEXT,TEHSIL TEXT,DISTRICT TEXT,STATE TEXT,BALANCE INTEGER,TOTAL TEXT,VENDORNAME TEXT,RANGE TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("inside", "onUpgrade sqlite");
        //db.execSQL("ALTER TABLE "+PLANS+" ADD COLUMN PROJECT TEXT");
        //db.execSQL("ALTER TABLE "+PLANS+" ADD COLUMN PLANNAME TEXT");
    }

    public Cursor getVillageName(String code) {
        Log.d("inside", "getVillageName sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * FROM " + PLANS + " WHERE VILLAGECODE = '"+code+"'", null);
        Log.d("res count =", "" + res.getCount());
        return res;
    }

    public Cursor viewData(String id) {
        Log.d("inside", "viewData sqlite");
        Cursor res;
        SQLiteDatabase db = this.getWritableDatabase();
        if (id.equals(""))
            res = db.rawQuery("select * FROM " + DATA + " WHERE FLAG = '0'", null);
        else
            res = db.rawQuery("select * FROM " + DATA + " WHERE SERVERID = " + id, null);
        return res;
    }

    public Cursor viewUploadedData() {
        Log.d("inside", "viewUploadedData sqlite");
        Cursor res;
        SQLiteDatabase db = this.getWritableDatabase();
        res = db.rawQuery("select DATAID,SERVERID,SERVERPLANID,VILLAGECODE,PRINTNO FROM " + DATA + " WHERE FLAG != '0' order by DATAID DESC LIMIT 200;" , null);
        return res;
    }

    public Cursor viewPlans() {
        Log.d("inside", "viewPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * FROM " + PLANS + " WHERE BALANCE != '0'", null);
        Log.d("res count =", "" + res.getCount());
        return res;
    }

    public Cursor viewVillage(String vCode) {
        Log.d("inside", "viewPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * FROM " + PLANS + " WHERE BALANCE != '0' AND VILLAGECODE = '"+vCode+"';", null);
        Log.d("res count =", "" + res.getCount());
        return res;
    }

    public Cursor viewPendingPlans() {
        Log.d("inside", "viewPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * FROM " + PENDINGPLANS , null);
        Log.d("res count =", "" + res.getCount());

        return res;
    }

    public Cursor viewRemarks(String projectcode) {

        Log.d("inside", "viewRemarks sqlite projectcode = " + projectcode);
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * FROM " + REMARKS + " WHERE PROJECTCODE = '"+projectcode+"'", null);
        Log.d("res count =", "" + res.getCount());
        return res;
    }

    public Cursor viewPrints(String code) {
        Log.d("inside", "viewPrints sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res;
        if(code.equals(""))
            res = db.rawQuery("select * FROM " + PRINTS + " WHERE FLAG = '0'", null);
        else
            res = db.rawQuery("select * FROM " + PRINTS + " WHERE VILLAGECODE = '"+code+"' AND FLAG = '0'", null);
        Log.d("res count =", "" + res.getCount());
        return res;
    }


    public Cursor viewPrintsForSupervisor(String code) {
        Log.d("inside", "viewPrints sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res;
        if(code.equals(""))
            res = db.rawQuery("select pt.*,pl.VillageName  FROM  "+PRINTS+" pt left join " + PLANS + " pl on pt.VILLAGECODE=pl.VILLAGECODE  WHERE pt.FLAG = '0'", null);
        else
            res = db.rawQuery("select * FROM " + PRINTS + " WHERE VILLAGECODE = '"+code+"' AND FLAG = '0'", null);
        Log.d("res count =", "" + res.getCount());
        return res;
    }


    public int verifyVillage(String code) {
        Log.d("inside", "viewPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * FROM " + PLANS + " WHERE VILLAGECODE = '"+code+"'", null);
        Log.d("res count =", "" + res.getCount());
        return res.getCount();
    }

    public long addData(String serverId,String planid,String villageCode, String remark, String nearpath, String farPath, String lat,
                        String lng,String address,String date,String prno) {
        Log.d("inside", "addData sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("SERVERID", serverId);
        content.put("SERVERPLANID", planid);
        content.put("VILLAGECODE", villageCode);
        content.put("REMARK", remark);
        content.put("NEARIMAGE", nearpath);
        content.put("FARIMAGE", farPath);
        content.put("LAT", lat);
        content.put("LNG", lng);
        content.put("ADDRESS", address);
        content.put("EXECUTIONDATE", date);
        content.put("PRINTNO", prno);
        content.put("FLAG", "0");
        Long res = db.insert(DATA, null, content);
        return res;
    }

    boolean addPlans(String serverId, String code, String name, String tehsil, String district, String state, String bal,
                      String lat,String lng,String range,String project,String planName) {
        Log.d("inside", "addPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("SERVERPLANID", serverId);
        content.put("VILLAGECODE", code);
        content.put("VILLAGENAME", name);
        content.put("TEHSIL", tehsil);
        content.put("DISTRICT", district);
        content.put("STATE", state);
        content.put("BALANCE", bal);
        content.put("LAT", lat);
        content.put("LNG", lng);
        content.put("RANGE", range);
        content.put("PROJECT", project);
        content.put("PLANNAME", planName);
        Long res = db.insert(PLANS, null, content);
        if (res == -1) {
            return false;
        } else {
            return true;
        }
    }

    boolean addPendingPlans(String serverId, String code, String name, String tehsil, String district,
                     String state, String bal,String total,String vendorname,String range) {
        Log.d("inside", "addPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("SERVERPLANID", serverId);
        content.put("VILLAGECODE", code);
        content.put("VILLAGENAME", name);
        content.put("TEHSIL", tehsil);
        content.put("DISTRICT", district);
        content.put("STATE", state);
        content.put("BALANCE", bal);
        content.put("TOTAL", total);
        content.put("VENDORNAME", vendorname);
        content.put("RANGE", range);
        Long res = db.insert(PENDINGPLANS, null, content);

        if (res == -1) {
            return false;
        } else {
            return true;
        }
    }

    boolean addRemarks(String remarkId, String remarks,String projcode) {
        Log.d("inside", "addPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("SERVERID", remarkId);
        content.put("TITLE", remarks);
        content.put("PROJECTCODE", projcode);
        Long res = db.insert(REMARKS, null, content);
        if (res == -1) {
            return false;
        } else {
            return true;
        }
    }

    boolean addPrints(String serverId,String planId, String code, String brand, String width, String height,
                      String sqft, String language,String printno,String lat,String lng,String address,String remarks,String projectcode) {
        Log.d("inside", "addPlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("SERVERID", serverId);
        content.put("SERVERPLANID", planId);
        content.put("VILLAGECODE", code);
        content.put("BRAND", brand);
        content.put("WIDTH", width);
        content.put("HEIGHT", height);
        content.put("SQFT", sqft);
        content.put("LANGUAGE", language);
        content.put("PRINTNO", printno);
        content.put("LAT", lat);
        content.put("LNG", lng);
        content.put("ADDRESS", address);
        content.put("FLAG", "0");
        content.put("REMARKS", remarks);
        content.put("PROJECTCODE", projectcode);
        Long res = db.insert(PRINTS, null, content);
        if (res == -1) {
            return false;
        } else {
            return true;
        }
    }

    public int getBalance(String id,String code) {
        Log.d("inside", "getBalance sqlite");
        int bal=0;
        Cursor res;
        SQLiteDatabase db = this.getWritableDatabase();
            res = db.rawQuery("select BALANCE FROM " + PLANS + " WHERE SERVERPLANID = '"+id+"' AND VILLAGECODE = '"+code+"'", null);
        Log.d("res count =", "" + res.getCount());
        if(res.getCount()>0)
        {
            res.moveToNext();
            bal = res.getInt(0);
        }

        return bal;
    }

    boolean updateDataFlag(String id,String flag) {
        Log.d("inside ","updatePlans");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("FLAG", flag);
        int temp = db.update(DATA, content, "DATAID = ?", new String[]{id});
        String a = toString().valueOf(temp);
        Log.d("integer-->", a);

        if (temp > 0) {
            return true;
        } else {
            return false;
        }
    }

    boolean updateDataFlagAgain(String id,String flag,String serverid,String planid,String printNo,String villageCode) {
        Log.d("inside ","updatePlans");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("FLAG", flag);
        int temp = db.update(DATA, content, "DATAID = ? ",new String[]{id});
        String a = toString().valueOf(temp);
        Log.d("integer-->", a);

        if (temp > 0) {
            return true;
        } else {
            return false;
        }
    }

    boolean updatePrintFlag(String id,String flag) {
        Log.d("inside ","updatePlans");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("FLAG", flag);
        int temp = db.update(PRINTS, content, "PRINTID = ?", new String[]{id});
        String a = toString().valueOf(temp);
        Log.d("integer-->", a);

        if (temp > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void updateBalance(String planId,String villageCode) {
        Log.d("inside ","updatePlans");
        int villageBalance = getBalance(planId,villageCode);
        villageBalance = villageBalance - 1;
        int a = reduceBalance(villageBalance,planId,villageCode);
        Log.d("result  = ", ""+a);

    }

    public int reduceBalance(int balance,String id,String code) {
        int temp;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("BALANCE", balance);
        temp = db.update(PLANS, content, "SERVERPLANID = ? AND VILLAGECODE = ?", new String[]{id,code});

        return temp;
    }

    boolean deleteAllData() {
        Log.d("inside", "deleteAllData sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + DATA);
        return true;
    }

    boolean deleteSingleData(String id) {
        Log.d("inside", "deleteSingleData sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + DATA + " WHERE SERVERID = " + id);
        return true;
    }

    boolean deletePlans() {
        Log.d("inside", "deletePlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + PLANS);
        return true;
    }

    boolean deletePendingPlans() {
        Log.d("inside", "deletePlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + PENDINGPLANS);
        return true;
    }

    boolean deletePrints() {
        Log.d("inside", "deletePlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + PRINTS);
        return true;
    }

    boolean deleteRemarks() {
        Log.d("inside", "deletePlans sqlite");
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + REMARKS);
        return true;
    }
}
