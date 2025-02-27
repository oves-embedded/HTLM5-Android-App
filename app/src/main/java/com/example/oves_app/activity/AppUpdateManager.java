package com.example.oves_app.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";

    // AWS S3 URL where your APK is stored
    private static final String APK_URL = "https://oves-apks.s3.amazonaws.com/oves_app.apk";

    // URL to a JSON file containing version info
    private static final String VERSION_INFO_URL = "https://oves-apks.s3.amazonaws.com/version_info.json";

    private Activity activity;

    public AppUpdateManager(Activity activity) {
        this.activity = activity;
    }

    // Check if an update is available
    public void checkForUpdates(int currentVersionCode) {
        new FetchVersionInfoTask(activity, currentVersionCode).execute();
    }

    // AsyncTask to fetch version info
    private static class FetchVersionInfoTask extends AsyncTask<Void, Void, VersionResult> {
        private Activity activity;
        private int currentVersionCode;

        FetchVersionInfoTask(Activity activity, int currentVersionCode) {
            this.activity = activity;
            this.currentVersionCode = currentVersionCode;
        }

        @Override
        protected VersionResult doInBackground(Void... voids) {
            try {
                URL url = new URL(VERSION_INFO_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    int latestVersionCode = jsonObject.getInt("versionCode");

                    boolean updateAvailable = latestVersionCode > currentVersionCode;
                    return new VersionResult(updateAvailable, latestVersionCode);
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error checking for updates", e);
            }

            return new VersionResult(false, currentVersionCode);
        }

        @Override
        protected void onPostExecute(VersionResult result) {
            if (result.isUpdateAvailable) {
                showUpdateDialog(result.latestVersionCode);
            }
        }

        private void showUpdateDialog(int newVersionCode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Update Available");
            builder.setMessage("A new version (v" + newVersionCode + ") of the app is available. Would you like to update now?");
            builder.setPositiveButton("Update", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL));
                activity.startActivity(intent);
            });
            builder.setNegativeButton("Later", null);
            builder.setCancelable(false);
            builder.show();
        }
    }

    // Helper class to hold version check result
    private static class VersionResult {
        boolean isUpdateAvailable;
        int latestVersionCode;

        VersionResult(boolean isUpdateAvailable, int latestVersionCode) {
            this.isUpdateAvailable = isUpdateAvailable;
            this.latestVersionCode = latestVersionCode;
        }
    }
}
