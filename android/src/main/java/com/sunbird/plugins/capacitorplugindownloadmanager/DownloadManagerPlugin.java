package com.sunbird.plugins.capacitorplugindownloadmanager;

import android.util.Log;

public class DownloadManagerPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
