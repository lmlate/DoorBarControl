package com.huaan.doorbar.common;

import android.content.Context;
import android.content.SharedPreferences;

public class Constants {
    public static final String AppSetting = "AppSetting";
    public static final String APP_ID = "C4H8atN7VHaNGWc8oVyFkswQznPG2BeS2fVVAyDHBcta";
    public static final String SDK_KEY = "HbsmwUgBVizSZkw4AND3zuPnLvJrFNF7RYmwAfHDihdf";

    private static String IP = "10.128.4.152";

    public static String IMG_SEARCH_URL = "http://" + IP + "/warehouse/face/peopleFaceInfo";
    public static String IMG_DOWNLOAD_URL = "http://" + IP + "/warehouse/images/";
    public static String UP_LOAD_URL = "http://" + IP + "/warehouse/entranceGuard/faceAuthentication";

    private static float SIMILAR_THRESHOLD = 0.75f;

    public static final int DELAY_REGISTER_TIME = 5000;


    public static void setFaceSimilar(Context context, float value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppSetting, Context.MODE_PRIVATE);
        sharedPreferences.edit().putFloat("face_similar_threshold", value).apply();
        SIMILAR_THRESHOLD = value;
    }

    public static float getFaceSimilar(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppSetting, Context.MODE_PRIVATE);
        return sharedPreferences.getFloat("face_similar_threshold", SIMILAR_THRESHOLD);
    }

    public static void setServerIp(Context context, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppSetting, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("server_ip", value).apply();
        IP = value;
    }

    public static String getServerIp(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppSetting, Context.MODE_PRIVATE);
        return sharedPreferences.getString("server_ip", IP);
    }
}

