package com.sunbird.plugins.capacitorplugindownloadmanager;

import java.lang.reflect.Array;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.content.ContentResolver;
import android.net.TrafficStats;
import android.content.Context;

import com.getcapacitor.CapacitorWebView;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.JSObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
public class DownloadManagerPlugin {

    public Boolean echo(String value) {
        Log.i("Echo", value);
        return true;
    }

    public String enqueue(String id) {
        Log.i("enqueue", id);
        return id;
    }

    public JSONArray query(JSONArray entries) {
        Log.i("query", String.valueOf(entries));
        return entries;
    }

    public String remove(String removeCount) {
        Log.i("remove", removeCount);
        return removeCount;
    }

    public String addCompletedDownload(String id) {
        Log.i("addCompletedDownload", id);
        return id;
    }

    public String fetchSpeedLog(String log) {
        Log.i("fetchSpeedLog", log);
        return log;
    }

}
