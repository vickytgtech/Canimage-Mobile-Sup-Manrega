package com.skyindya.dwsupervisor2;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by skyindya on 3/15/2017.
 */

public class ConvertImage {

    public Bitmap decodeFile(String imgPath) {
        Bitmap b = null;
        int max_size = 1000;
        File f = new File(imgPath);
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();
            int scale = 1;
            if (o.outHeight > max_size || o.outWidth > max_size) {
                scale = (int) Math.pow(2, (int) Math.ceil(Math.log(max_size / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
//            o2.inSampleSize = scale;
            o2.inSampleSize = 2;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    public Bitmap loadImage(Uri path,Activity act)
    {
        int max_size = 1000;
        Bitmap takenImage = null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        ContentResolver cr = act.getContentResolver();
        InputStream input = null;
        InputStream input1 = null;
        try {
            input = cr.openInputStream(path);
            BitmapFactory.decodeStream(input, null, o);
            if (input != null) {
                input.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int scale = 1;
        if (o.outHeight > max_size || o.outWidth > max_size) {
            scale = (int) Math.pow(2, (int) Math.ceil(Math.log(max_size / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        try {
            input1 = cr.openInputStream(path);
             takenImage = BitmapFactory.decodeStream(input1,null,o2);
            if (input1 != null) {
                input1.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return takenImage;
    }
}
