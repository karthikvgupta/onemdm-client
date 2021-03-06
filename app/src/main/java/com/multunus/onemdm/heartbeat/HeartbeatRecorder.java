package com.multunus.onemdm.heartbeat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.multunus.onemdm.config.Config;
import com.multunus.onemdm.util.Logger;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HeartbeatRecorder {

    private final Context context;
    private final RequestQueue requestQueue;

    public HeartbeatRecorder(Context context){
        this.context = context;
        requestQueue = Volley.newRequestQueue(this.context);

    }
    public void sendHeartbeatToServer() {

        JsonObjectRequest request = new CustomJsonObjectRequest(
                Request.Method.POST,
                Config.HEARTBEAT_URL,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            configureNextHeartbeat(response.getLong("next_heartbeat_time"));
                        }
                        catch (Exception ex){

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        configureNextHeartbeatForRetry();
                        Logger.warning(error.toString());
                    }
                }
        );
        requestQueue.add(request);
    }

    public void configureNextHeartbeat(long nextHearbeatTime) {
        nextHearbeatTime = nextHearbeatTime * 1000;
        configureNextHeartbeatWithMilliSeconds(nextHearbeatTime);
    }

    public  void configureNextHeartbeatForRetry() {
        configureNextHeartbeatWithMilliSeconds(getDefaultNextHearbeatTime());
    }

    public void configureNextHeartbeatWithMilliSeconds(long nextHearbeatTime) {
        Logger.debug(" next heartbeat time " + nextHearbeatTime);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, HeartbeatListener.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0,
                intent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                nextHearbeatTime, sender);
    }
    private long getDefaultNextHearbeatTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, Config.DEFAULT_HEARTBEAT_RETRY_INTERVAL);
        Logger.debug(calendar.getTime().toString());
        return calendar.getTimeInMillis();
    }

    public class CustomJsonObjectRequest extends JsonObjectRequest
    {
        public CustomJsonObjectRequest(int method, String url,
                                       Response.Listener listener,
                                       Response.ErrorListener errorListener)
        {
            super(method, url, listener, errorListener);
        }

        @Override
        public Map getHeaders() throws AuthFailureError {
            String token =  context.getSharedPreferences(Config.PREFERENCE_TAG,
                    Context.MODE_PRIVATE).getString(Config.ACCESS_TOKEN, "");
            Map headers = new HashMap();
            headers.put("Authorization", "Token token="+token);
            Logger.debug("added header "+token);
            return headers;
        }

    }
}
