package com.sunbird.plugins.capacitorplugindownloadmanager;

import com.getcapacitor.JSObject;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginResult;
import com.getcapacitor.CapacitorWebView;
import com.getcapacitor.annotation.ActivityCallback;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.getcapacitor.Logger;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@CapacitorPlugin(name = "DownloadManagerPlugin")
public class DownloadManagerPluginPlugin extends Plugin {
    private DownloadManagerPlugin implementation = new DownloadManagerPlugin();

    public static final int STATUS_PENDING = 1 << 0;
    public static final int STATUS_RUNNING = 1 << 1;
    public static final int STATUS_PAUSED = 1 << 2;
    public static final int STATUS_SUCCESSFUL = 1 << 3;
    public static final int STATUS_FAILED = 1 << 4;

    /**
     * This download hasn't stated yet
     */
    public static final int DOWNLOADS_STATUS_PENDING = 190;
    /**
     * This download has started
     */
    public static final int DOWNLOADS_STATUS_RUNNING = 192;
    /**
     * This download has been paused by the owning app.
     */
    public static final int DOWNLOADS_STATUS_PAUSED_BY_APP = 193;
    /**
     * This download encountered some network error and is waiting before retrying the request.
     */
    public static final int DOWNLOADS_STATUS_WAITING_TO_RETRY = 194;
    /**
     * This download is waiting for network connectivity to proceed.
     */
    public static final int DOWNLOADS_STATUS_WAITING_FOR_NETWORK = 195;
    /**
     * This download exceeded a size limit for mobile networks and is waiting for a Wi-Fi
     * connection to proceed.
     */
    public static final int DOWNLOADS_STATUS_QUEUED_FOR_WIFI = 196;
    public static final int DOWNLOADS_STATUS_SUCCESS = 200;

    public static final String DOWNLOADS_COLUMN_MIME_TYPE = "mimetype";
    public static final String DOWNLOADS_COLUMN_TOTAL_BYTES = "total_bytes";
    public static final String DOWNLOADS_COLUMN_CURRENT_BYTES = "current_bytes";
    public static final String DOWNLOADS_COLUMN_LAST_MODIFICATION = "lastmod";

    DownloadManager downloadManager;
    private final Handler handler = new Handler();
    private long mLastRxBytes = 0;
    private long mLastTxBytes = 0;
    private long mLastTime = 0;
    private long mTotalBytesDownloaded = 0;
    private ContentResolver mResolver;
    private Map<Integer, Integer> rangeMap = new HashMap<>();
    private Map<Integer, Integer> indexMap = new HashMap<>();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                double speed = getNetworkSpeed();
                if (speed > 0) {
                    int key = speed < 1024 ? getFirstBucketKey(speed) : getSecondBucketKey(speed);
                    int range = indexMap.get(key);
                    if (rangeMap.containsKey(range)) {
                        Integer rangeKey = rangeMap.get(range);
                        rangeMap.put(range, rangeKey + 1);
                    } else {
                        rangeMap.put(range, 1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private AppCompatActivity activity = new AppCompatActivity() {
        @Override
        protected void onPause() {
            super.onPause();
            if (handler != null) {
                handler.removeMessages(0);
            }
        }
    
        @Override
        protected void onResume() {
            super.onResume();
            if (handler != null && runnable != null) {
                handler.postDelayed(runnable, 1000);
            }
        }    
    };

    public void load() {
        downloadManager = (DownloadManager) getActivity().getApplication().getApplicationContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);
        mResolver = getActivity().getApplication().getApplicationContext().getContentResolver();
        mLastRxBytes = TrafficStats.getTotalRxBytes();
        mLastTxBytes = TrafficStats.getTotalTxBytes();
        mLastTime = System.currentTimeMillis();
        this.initSpeedLogger();
        startFetchingDownloadSpeed();
    }

    private void startFetchingDownloadSpeed() {
        handler.postDelayed(runnable, 2000);
    }

    public void initSpeedLogger() {
        indexMap.put(1, 32);
        indexMap.put(2, 64);
        indexMap.put(3, 128);
        indexMap.put(4, 256);
        indexMap.put(5, 512);
        indexMap.put(6, 1024);
        indexMap.put(7, 1536);
        indexMap.put(8, 2048);
        indexMap.put(9, 2560);
        indexMap.put(10, 3072);
        indexMap.put(11, 3584);
        indexMap.put(-1, 4096);
    }

    public int getFirstBucketKey(double speed) {
        int result = (int) (Math.log(speed) / Math.log(2) - 3);
        return result < 1 ? 1 : result;
    }

    public int getSecondBucketKey(double speed) {
        int result = (int) (speed / 512) + 4;
        return result >= 16 ? -1 : result;
    }

    private double getNetworkSpeed() {
        try {
            long currentRxBytes = TrafficStats.getTotalRxBytes();
            long currentTxBytes = TrafficStats.getTotalTxBytes();
            long usedRxBytes = currentRxBytes - mLastRxBytes;
            long usedTxBytes = currentTxBytes - mLastTxBytes;
            long currentTime = System.currentTimeMillis();
            long usedTime = currentTime - mLastTime;

            mLastRxBytes = currentRxBytes;
            mLastTxBytes = currentTxBytes;
            mLastTime = currentTime;

            long totalBytes = usedRxBytes + usedTxBytes;
            double totalSpeed = 0;
            if (usedTime > 0) {
                totalSpeed = (double) totalBytes / usedTime;
            }
            mTotalBytesDownloaded += totalBytes;
            return totalSpeed;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }
    
    @PluginMethod
    public void enqueue(PluginCall call) throws JSONException {
        JSObject obj = call.getObject("req");
        Log.d("****** ", obj.toString());
        try {
            DownloadManager.Request req = deserialiseRequest(obj);
            Log.d("manager req ", req.toString());
            if (downloadManager == null)
                downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            long id = downloadManager.enqueue(req);
            JSObject ret = new JSObject();
            ret.put("value", implementation.enqueue(String.valueOf(id)));
            Log.d("return value id ", ret.toString());
            call.resolve(ret);
        } catch (Error e) {
            call.reject(e.toString());
        }
    }

    @PluginMethod
    public void query(PluginCall call) throws JSONException {
        JSObject obj = call.getObject("filter");
        Log.d("********** query req ", obj.toString());
        DownloadManager.Query query = deserialiseQuery(obj);

        Cursor downloads;
        Log.d("Query from deserialise ", query.toString());
        try {
            downloads = downloadManager.query(query);
            Log.d("downlaods on query ", downloads.toString());
        } catch (SQLiteException e) {
            e.printStackTrace();

            // SELECT _id, _id, mediaprovider_uri, destination, title, description, uri, status, hint, media_type, total_size, last_modified_timestamp, bytes_so_far, allow_write, local_uri, reason
            // FROM downloads WHERE ((uid=10175 OR otheruid=10175)) AND ((_id = ? ) AND deleted != '1') ORDER BY lastmod DESC

            Log.d("catch ", e.toString());
            Uri uri = Uri.parse("content://downloads/my_downloads");
            String[] projection = new String[]{
                    DownloadManager.COLUMN_ID,
                    DownloadManager.COLUMN_MEDIAPROVIDER_URI,
                    DownloadManager.COLUMN_TITLE,
                    DownloadManager.COLUMN_DESCRIPTION,
                    DownloadManager.COLUMN_URI,
                    DownloadManager.COLUMN_STATUS,
                    DOWNLOADS_COLUMN_MIME_TYPE,
                    DOWNLOADS_COLUMN_TOTAL_BYTES,
                    DOWNLOADS_COLUMN_LAST_MODIFICATION,
                    DOWNLOADS_COLUMN_CURRENT_BYTES
            };
            String selection = "(_id = ? ) AND deleted != '1'";
            String[] selectionArgs = new String[]{
                    obj.optJSONArray("ids").getString(0)
            };
            String orderBy = "lastmod DESC";

            downloads = mResolver.query(uri, projection, selection, selectionArgs, orderBy);
            Log.d("dowlaods on catch ", downloads.toString());
        }

        Log.d("downlaods  ", String.valueOf(downloads));
        Log.d("Count ", String.valueOf(downloads.getCount()));
        if(downloads.getCount() > 0) {
            JSObject ret = new JSObject();
            ret.put("value", implementation.query(JSONFromCursor(downloads)));
            call.resolve(ret);
        }

        if (downloads != null) {
            downloads.close();
        }
        call.resolve();
    }

    @PluginMethod
    public void remove(PluginCall call) throws JSONException {
        JSONArray arr = call.getArray("ids");
        Log.d("Array ", arr.toString());
        long[] ids = longsFromJSON(arr);
        int removed = downloadManager.remove(ids);
        JSObject ret = new JSObject();
        ret.put("value", implementation.remove(String.valueOf(removed)));
        call.resolve(ret);
    }

    @PluginMethod
    public void addCompletedDownload(PluginCall call) throws JSONException {
        JSObject obj = call.getObject("req");
        Log.d("******* add compeleted Download ", obj.toString());
        long id = downloadManager.addCompletedDownload(obj.optString("title"), obj.optString("description"),
                obj.optBoolean("isMediaScannerScannable", false), obj.optString("mimeType"), obj.optString("path"),
                obj.optLong("length"), obj.optBoolean("showNotification", true));
        // NOTE: If showNotification is false, you need
        // <uses-permission android: name =
        // "android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
        Log.d("id on add complete download ", String.valueOf(id));
        JSObject ret = new JSObject();
        ret.put("value", implementation.addCompletedDownload(String.valueOf(id)));
        call.resolve(ret);
    }

    @PluginMethod
    public void fetchSpeedLog(PluginCall call) throws JSONException {
        try {
            JSONObject speedLog = new JSONObject();

            long totalKBdownloaded = mTotalBytesDownloaded / 1024;

            JSONObject distribution = new JSONObject();

            for (Map.Entry<Integer, Integer> entry : rangeMap.entrySet()) {
                distribution.put(entry.getKey().toString(), entry.getValue());
            }

            speedLog.put("totalKBdownloaded", totalKBdownloaded);
            speedLog.put("distributionInKBPS", distribution);
            JSObject ret = new JSObject();
            ret.put("value", implementation.fetchSpeedLog(String.valueOf(speedLog)));
            call.resolve(ret);
            mTotalBytesDownloaded = 0;
            rangeMap.clear();
        } catch (Exception e) {
            call.reject(e.toString());
            mTotalBytesDownloaded = 0;
            rangeMap.clear();
        }
    }

    protected DownloadManager.Request deserialiseRequest(JSObject obj) throws JSONException {
        Log.d("****** obj ", obj.getString("uri"));
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(obj.getString("uri")));
        Log.d("****** req ", req.toString());

        req.setTitle(obj.optString("title"));
        req.setDescription(obj.optString("description"));
        req.setMimeType(obj.optString("mimeType", null));

        if (obj.has("destinationInExternalFilesDir")) {
            Context context = getActivity().getApplication().getApplicationContext();

            JSONObject params = obj.getJSONObject("destinationInExternalFilesDir");

            req.setDestinationInExternalFilesDir(context, params.optString("dirType"), params.optString("subPath"));
        } else if (obj.has("destinationInExternalPublicDir")) {
            JSONObject params = obj.getJSONObject("destinationInExternalPublicDir");

            req.setDestinationInExternalPublicDir(params.optString("dirType"), params.optString("subPath"));
        } else if (obj.has("destinationUri"))
            req.setDestinationUri(Uri.parse(obj.getString("destinationUri")));

        req.setVisibleInDownloadsUi(obj.optBoolean("visibleInDownloadsUi", true));
        req.setNotificationVisibility(obj.optInt("notificationVisibility"));

        if (obj.has("headers")) {
            JSONArray arrHeaders = obj.optJSONArray("headers");
            for (int i = 0; i < arrHeaders.length(); i++) {
                JSONObject headerObj = arrHeaders.getJSONObject(i);
                req.addRequestHeader(headerObj.optString("header"), headerObj.optString("value"));
            }
        }
        Log.d("return req ", req.toString());
        return req;
    }

    protected DownloadManager.Query deserialiseQuery(JSONObject obj) throws JSONException {
        DownloadManager.Query query = new DownloadManager.Query();

        long[] ids = longsFromJSON(obj.optJSONArray("ids"));
        query.setFilterById(ids);

        if (obj.has("status")) {
            query.setFilterByStatus(obj.getInt("status"));
        }

        Log.d("return query ", query.toString());
        return query;
    }

    private static long[] longsFromJSON(JSONArray arr) throws JSONException {
        if (arr == null)
            return null;

        long[] longs = new long[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
            String str = arr.getString(i);
            longs[i] = Long.valueOf(str);
        }

        return longs;
    }

    private static JSONArray JSONFromCursor(Cursor cursor) throws JSONException {
        JSONArray result = new JSONArray();

        cursor.moveToFirst();
        do {
            JSONObject rowObject = new JSONObject();
            if (cursor.getColumnIndex(DownloadManager.COLUMN_ID) != -1) {
                rowObject.put("id", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_ID)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_TITLE) != -1) {
                rowObject.put("title", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION) != -1) {
                rowObject.put("description", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE) != -1) {
                rowObject.put("mediaType", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)));
            }
            if (cursor.getColumnIndex(DOWNLOADS_COLUMN_MIME_TYPE) != -1) {
                rowObject.put("mediaType", cursor.getString(cursor.getColumnIndex(DOWNLOADS_COLUMN_MIME_TYPE)));
            }
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N
                    && cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME) != -1) {
                rowObject.put("localFilename",
                        cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI) != -1) {
                rowObject.put("localUri", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI) != -1) {
                rowObject.put("mediaproviderUri",
                        cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_URI) != -1) {
                rowObject.put("uri", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP) != -1) {
                rowObject.put("lastModifiedTimestamp",
                        cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
            }
            if (cursor.getColumnIndex(DOWNLOADS_COLUMN_LAST_MODIFICATION) != -1) {
                rowObject.put("lastModifiedTimestamp",
                        cursor.getLong(cursor.getColumnIndex(DOWNLOADS_COLUMN_LAST_MODIFICATION)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) != -1) {
                if (cursor.getColumnIndex(DOWNLOADS_COLUMN_LAST_MODIFICATION) != -1) {
                    rowObject.put("status", translateStatus(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))));
                } else {
                    rowObject.put("status", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                }
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_REASON) != -1) {
                rowObject.put("reason", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
            }
            if (cursor.getColumnIndex(DOWNLOADS_COLUMN_CURRENT_BYTES) != -1) {
                rowObject.put("bytesDownloadedSoFar",
                        cursor.getLong(cursor.getColumnIndex(DOWNLOADS_COLUMN_CURRENT_BYTES)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) != -1) {
                rowObject.put("bytesDownloadedSoFar",
                        cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
            }
            if (cursor.getColumnIndex(DOWNLOADS_COLUMN_TOTAL_BYTES) != -1) {
                rowObject.put("totalSizeBytes",
                        cursor.getLong(cursor.getColumnIndex(DOWNLOADS_COLUMN_TOTAL_BYTES)));
            }
            if (cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) != -1) {
                rowObject.put("totalSizeBytes",
                        cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
            }
            result.put(rowObject);
        } while (cursor.moveToNext());

        return result;
    }

    private static int translateStatus(int status) {
        switch (status) {
            case DOWNLOADS_STATUS_PENDING:
                return STATUS_PENDING;

            case DOWNLOADS_STATUS_RUNNING:
                return STATUS_RUNNING;

            case DOWNLOADS_STATUS_PAUSED_BY_APP:
            case DOWNLOADS_STATUS_WAITING_TO_RETRY:
            case DOWNLOADS_STATUS_WAITING_FOR_NETWORK:
            case DOWNLOADS_STATUS_QUEUED_FOR_WIFI:
                return STATUS_PAUSED;

            case DOWNLOADS_STATUS_SUCCESS:
                return STATUS_SUCCESSFUL;

            default:
                return STATUS_FAILED;
        }
    }

}
