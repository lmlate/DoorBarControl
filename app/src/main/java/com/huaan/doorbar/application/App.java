package com.huaan.doorbar.application;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.huaan.doorbar.activity.SplashScreenActivity;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

/**
 * @description
 * @author: litrainy
 * @create: 2019-06-03 16:33
 **/
public class App extends Application implements Thread.UncaughtExceptionHandler {

    private static App instance;
    private PendingIntent mIntent;
    private SharedPreferences mSharedPreferences;


    @Override
    public void onCreate() {
        super.onCreate();
        initLogger();
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(this);
        mIntent = PendingIntent.getActivity(this, 0,new Intent(this, SplashScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, mIntent);
        System.exit(0);
    }

    private void initLogger() {
        PrettyFormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder().tag("DoorControl").showThreadInfo(true).build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
    }

    public static App getInstance(){
        return instance;
    }
}
