package com.hmdm.launcher.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.hmdm.launcher.util.DeviceInfoProvider;

public class SerialActivity extends BaseActivity {

    private static final String TAG = "SerialActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fetchSerialNo();
    }

    @SuppressLint("UnsafeIntentLaunch")
    private void fetchSerialNo() {
        String serialNo = DeviceInfoProvider.getSerialNumber();
        Log.e(TAG, "fetchSerialNo: " + serialNo);
        Intent intent = getIntent();
        intent.putExtra("SERIAL_NO_KEY", serialNo);
        setResult(RESULT_OK, intent);
        finish();
    }

}
