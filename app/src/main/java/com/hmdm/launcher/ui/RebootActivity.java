package com.hmdm.launcher.ui;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.hmdm.launcher.R;
import com.hmdm.launcher.databinding.ActivityRebootBinding;
import com.hmdm.launcher.util.LegacyUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class RebootActivity extends BaseActivity {

    ActivityRebootBinding binding;

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reboot);

        new CountDownTimer(6000, 1000) {
            public void onTick(long millisUntilFinished) {
                NumberFormat f = new DecimalFormat("00");
                long sec = (millisUntilFinished / 1000) % 60;
                binding.tvInfo.setText("Rebooting TV In: " + f.format(sec));
            }

            public void onFinish() {
                reboot();
            }
        }.start();
    }

    public void reboot() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            ComponentName deviceAdmin = LegacyUtils.getAdminComponentName(getApplicationContext());
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            try {
                devicePolicyManager.reboot(deviceAdmin);
            } catch (Exception e) {
                Toast.makeText(this, R.string.reboot_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

}
