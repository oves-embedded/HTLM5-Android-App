package com.oves.app.util;



import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.HashMap;
import java.util.Map;

public class DeviceUtil {


    public static Map<String, String> getDeviceInfo(Context context) {
        Map<String, String> buildInfoMap = new HashMap<>();
        // 基本信息
        buildInfoMap.put("BOARD", Build.BOARD);
        buildInfoMap.put("BOOTLOADER", Build.BOOTLOADER);
        buildInfoMap.put("BRAND", Build.BRAND);
        // 设备信息
        buildInfoMap.put("DEVICE", Build.DEVICE);
        buildInfoMap.put("DISPLAY", Build.DISPLAY);
        buildInfoMap.put("FINGERPRINT", Build.FINGERPRINT);
        // 硬件信息
        buildInfoMap.put("HARDWARE", Build.HARDWARE);
        buildInfoMap.put("HOST", Build.HOST);
        buildInfoMap.put("ID", Build.ID);
        // 制造商和型号
        buildInfoMap.put("MANUFACTURER", Build.MANUFACTURER);
        buildInfoMap.put("MODEL", Build.MODEL);
        buildInfoMap.put("PRODUCT", Build.PRODUCT);
        // 版本信息
        buildInfoMap.put("RELEASE", Build.VERSION.RELEASE);
        buildInfoMap.put("SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
        buildInfoMap.put("CODENAME", Build.VERSION.CODENAME);
        // 标签和类型
        buildInfoMap.put("TAGS", Build.TAGS);
        buildInfoMap.put("TYPE", Build.TYPE);
        buildInfoMap.put("USER", Build.USER);
        // 构建时间
        buildInfoMap.put("TIME", String.valueOf(Build.TIME));

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            buildInfoMap.put("MCC_MNC", telephonyManager.getNetworkOperator());
            buildInfoMap.put("CARRIER", telephonyManager.getNetworkOperatorName());
        } else {
            buildInfoMap.put("MCC_MNC", "");
            buildInfoMap.put("CARRIER", "");
        }


        // Android ID
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        buildInfoMap.put("ANDROID_ID", androidId);
        return buildInfoMap;
    }


    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        // Android 10+ 推荐使用 NetworkCapabilities
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            // 兼容旧版本
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }


    /**
     * 判断移动数据开关是否打开
     *
     * @param context 上下文
     * @return 移动数据开关是否打开
     */
    public static boolean isMobileDataEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0 及以上版本
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                return networkInfo != null && networkInfo.isConnectedOrConnecting();
            }
        } else {
            // Android 7.0 以下版本
            try {
                return Settings.Secure.getInt(context.getContentResolver(), "mobile_data") == 1;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 检测 GPS 是否开启
     *
     * @param context 上下文
     * @return 如果 GPS 开启返回 true，否则返回 false
     */
    public static boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        return false;
    }


    public static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WIFI";
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "CELLULAR";
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "ETHERNET";
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                    return "BLUETOOTH";
                }
            }
        }
        return "UNKNOWN";
    }


}
