package com.hmdm.launcher.ui;

import static com.hmdm.launcher.util.InstallUtils.createIntentSender;
import static com.hmdm.launcher.util.Utils.isDeviceOwner;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.hmdm.launcher.R;
import com.hmdm.launcher.databinding.ActivitySoftwareUpdateBinding;
import com.hmdm.launcher.json.SoftwareResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.CRC32;

public class SoftwareUpdateActivity extends AppCompatActivity {

    private static final String TAG = "SoftwareUpdateActivity";
    private static final String MB_PACKAGE_NAME = "com.diipl.moviebeam";
    private static final String MB_SERIAL_ACTIVITY = MB_PACKAGE_NAME+".ui.serial_info.SerialActivity";
    ActivitySoftwareUpdateBinding binding;
    SoftwareResponse response;

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_software_update);

        Intent intent = getIntent();

        String softwareData = intent.getStringExtra("softwareData");
        response = new Gson().fromJson(softwareData, SoftwareResponse.class);
        String buildVersion = intent.getStringExtra("buildVersion"); // existing apk version

        Log.e(TAG, "onCreate: "+softwareData );

        String url = response.getSoftwareDownloadFtpUrl();
        String apkVersion = response.getSoftwareVersion();
        url += response.getFileName();

        boolean isUpgradeable = compareVersions(buildVersion, apkVersion);
        boolean isOwner = isDeviceOwner(getApplicationContext());

        if (isUpgradeable && isOwner) {
            startDownload(url);
        } else finish();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isCRCValid(String filePath, String crcValue) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            CRC32 checksum = new CRC32();
            checksum.update(data);
            Log.e(TAG, "isCRCValid: " + crcValue + "   -->>  " + checksum.getValue());
            return String.valueOf(checksum.getValue()).equals(crcValue);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean compareVersions(String buildVersion, String apkVersion) {
        int a = Integer.parseInt(apkVersion.replace(".", ""));
        int b = Integer.parseInt(buildVersion.replace(".", ""));

        return a > b;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startDownload(String fileURL) {
        new Thread(() -> {
            try {
                Log.e(TAG, "startDownload: "+fileURL );
                URL url = new URL(fileURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    File directory = new File(getExternalMediaDirs()[0], "APK");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File file = new File(directory, url.getPath().substring(url.getPath().lastIndexOf("/") + 1));
                    FileOutputStream outputStream = new FileOutputStream(file);
                    InputStream inputStream = connection.getInputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    outputStream.close();

                    String filePath = file.getAbsolutePath();
                    boolean isCRCValid = isCRCValid(filePath, response.getFileCrc());
                    String fileSize = String.valueOf(file.length());
                    Log.e(TAG, "fileSize: "+fileSize +"  ==>  "+response.getFileSize() );
                    if (fileSize.equals(response.getFileSize()))
                        startInstall(filePath);
                    else finish();

                } else {
                    Log.e("startDownload:", "Failed");
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e("startDownload:", " Exception: " + e.getLocalizedMessage());
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startInstall(String file) {
        try {
            FileInputStream in = new FileInputStream(file);
            PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(getPackageName());
            // set params
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            OutputStream out = session.openWrite("COSU", 0, -1);
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            in.close();
            out.close();

            session.commit(createIntentSender(this, sessionId, getPackageName()));

            Intent intent = new Intent();
//            intent.setComponent(new ComponentName("com.diipl.moviebeam", "com.diipl.moviebeam.ui.mainmenu.MainMenuActivity"));
            intent.setComponent(new ComponentName(MB_PACKAGE_NAME, MB_SERIAL_ACTIVITY));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "PackageInstaller error: " + e.getMessage());
        }
    }

}