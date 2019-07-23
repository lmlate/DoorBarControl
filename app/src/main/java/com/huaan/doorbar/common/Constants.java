package com.huaan.doorbar.common;

import android.content.Context;
import android.content.SharedPreferences;

public class Constants {
    public static final String SP_NAME = "ArcFaceDemo";
    public static String APP_ID = "CfKVx5snbRFqAkEa7LnHE1n1ba2vAMQm8qcdWbe1crxR";
    public static String SDK_KEY = "3gNoWRNFMSVgiujUJ9T5SRMu2hBPzHhYnwvEQDMtHtKc";

    private static String IP = "10.128.4.152";
    private static String PORT = "8080";
    private static int SOCKET_PORT = 10003;

    private static float FACE_SIMILAR_THRESHOLD = 0.75f;//识别度

    public static String getImageSearchUrl(String ip, String port) {
        return "http://" + IP + ":" + PORT + "/warehouse/face/peopleFaceInfo";
    }

    public static String getImageDownloadUrl(String ip, String port) {
        return "http://" + IP + ":" + PORT + "/warehouse/images/";
    }

    public static String getUpLoadUrl(String ip, String port) {
        return "http://" + IP + ":" + PORT + "/warehouse/entranceGuard/faceAuthentication";
    }

    public static String getSyncFaceUrl(String ip, String PORT) {
        return "http://"+IP+":"+ PORT+"/warehouse/sync/getFaceInfo";
    }

    public static String getSyncFinishFaceUrl(String ip, String PORT) {
        return "http://"+IP+":"+ PORT+"/warehouse/sync/finishFaceInfo";
    }

    public static void setFaceSimilar(Context context, float value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putFloat("face_similar_threshold", value).apply();
        FACE_SIMILAR_THRESHOLD = value;
    }

    public static float getFaceSimilar(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getFloat("face_similar_threshold", FACE_SIMILAR_THRESHOLD);
    }

    public static void setServerIp(Context context, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("server_ip", value).apply();
        IP = value;
    }

    public static String getServerIp(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("server_ip", IP);
    }

    public static void setServerPort(Context context, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("server_port", value).apply();
        PORT = value;
    }

    public static String getServerPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("server_port", PORT);
    }

    public static void setSocketPort(Context context, int value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt("socket_port", value).apply();
        SOCKET_PORT = value;
    }

    public static int getSocketPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt("socket_port", SOCKET_PORT);
    }

}

