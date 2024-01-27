package com.skyindya.dwsupervisor2;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by SkyIndya-server on 10/26/2016.
 */

public class VolleySingleton {

    private static VolleySingleton volleySingleton=null;
    private RequestQueue requestQueue=null;
    private static Context ctx;

    private VolleySingleton(Context ctx){
        this.ctx=ctx;
        requestQueue=getRequestQueue();

    }

    public RequestQueue getRequestQueue(){
        if(requestQueue== null){
            requestQueue= Volley.newRequestQueue(ctx);
        }
        return  requestQueue;
    }

    public static synchronized   VolleySingleton getInstance(Context ctx){
        if(volleySingleton==null){
            volleySingleton= new VolleySingleton(ctx);
        }
        return volleySingleton;
    }

    public<T> void addRequestQueue(Request<T> request){
        request.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    public void callToast(Context ctx, String s){
        Toast.makeText(ctx,s, Toast.LENGTH_LONG).show();
    }

    }

