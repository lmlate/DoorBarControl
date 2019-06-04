package com.huaan.doorbar.util.other;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

/**
 * @description
 * @author: litrainy
 * @create: 2019-05-31 10:41
 **/
public class MyToast {
    /**
     * 吐丝~~~
     */
    private static Toast toast = null;

    public static void showToast(Context context,String string) {
        if (toast == null) {
            toast = Toast.makeText(context, string, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(string);
            toast.show();
        }
    }

    public static void showToast(Context context, @StringRes int res) {
        if (toast == null) {
            toast = Toast.makeText(context, res, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(res);
            toast.show();
        }
    }

    public static void showLongToast(Context context, String string) {
        if (toast == null) {
            toast = Toast.makeText(context, string, Toast.LENGTH_LONG);
            toast.show();
        } else {
            toast.setText(string);
            toast.show();
        }
    }

    public static void showLongToast(Context context, @StringRes int res) {
        if (toast == null) {
            toast = Toast.makeText(context, res, Toast.LENGTH_LONG);
            toast.show();
        } else {
            toast.setText(res);
            toast.show();
        }
    }

}
