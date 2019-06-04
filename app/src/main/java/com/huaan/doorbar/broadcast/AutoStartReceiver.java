package com.huaan.doorbar.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.huaan.doorbar.activity.SplashScreenActivity;

/**
 * @description
 * @author: litrainy
 * @create: 2019-05-28 09:09
 **/
public class AutoStartReceiver extends BroadcastReceiver {
    public AutoStartReceiver()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            Intent i = new Intent(context, SplashScreenActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
