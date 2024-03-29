package com.hmdm.launcher.service;

import static com.hmdm.launcher.util.DeviceInfoProvider.getSerialNumber;
import static com.hmdm.launcher.util.InstallUtils.createIntentSender;
import static com.hmdm.launcher.util.Utils.isDeviceOwner;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    private static final String MB_PACKAGE_NAME = "com.diipl.moviebeam";
    private static final String MB_SERIAL_ACTIVITY = "com.diipl.moviebeam.ui.serial_info.SerialActivity";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String softwareData = intent.getStringExtra("softwareData");
        SoftwareResponse response = new Gson().fromJson(softwareData, SoftwareResponse.class);
        String buildVersion = intent.getStringExtra("buildVersion"); // existing apk version

        String url = response.getSoftwareDownloadFtpUrl();
        String apkVersion = response.getSoftwareVersion();
        url += response.getFileName() + ".apk";
        String filePath;

        boolean isUpgradeable = compareVersions(buildVersion, apkVersion);
        boolean isOwner = isDeviceOwner(getApplicationContext());

        filePath = startDownload(getApplicationContext(), url);

        Log.e(TAG, "filePath: "+filePath );
        if (filePath != null && isUpgradeable) {
            boolean isCRCValid = isCRCValid(filePath, response.getFileCrc());
            File file = new File(filePath);
            String fileSize = String.valueOf(file.length() / 1024);
            Log.e(TAG, "fileSize: "+fileSize +"  ==>  "+response.getFileSize() );
            if (fileSize.equals(response.getFileSize()))
                startInstall(filePath);
        }

        return START_STICKY;
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
    public String startDownload(Context context, String fileURL) {
        AtomicReference<String> result = new AtomicReference<>(null);
        new Handler().post(() -> {
            try {
                URL url = new URL(fileURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    File directory = new File(context.getExternalMediaDirs()[0], "APK");
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
                    result.set(file.getAbsolutePath());
                } else {
                    Log.e("startDownload:", "Failed");
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e("startDownload:", " Exception: " + e.getMessage());
            }

        });
        return result.get();
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
