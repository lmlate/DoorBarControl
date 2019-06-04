package com.huaan.doorbar.util.other;

import android.content.Context;

import com.example.yfaceapi.GPIOManager;

/**
 * @description
 * @author: litrainy
 * @create: 2019-05-31 09:13
 **/
public class MotherboardUtil {

    private static GPIOManager gpioManager;
    private static final int SLEEP_TIME = 3000;

    public static void init(Context context) {
        gpioManager= GPIOManager.getInstance(context);
    }

    public static void Failure() {
        pullDownLight();
        gpioManager.pullUpRedLight();
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        gpioManager.pullDownRedLight();
    }

    public static void Succeed() {
        pullDownLight();
        gpioManager.pullUpGreenLight();
//        gpioManager.pullUpRelay();
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        gpioManager.pullDownGreenLight();
//        gpioManager.pullDownRelay();
    }

    public static void pullDownLight() {
        gpioManager.pullDownRedLight();
        gpioManager.pullDownGreenLight();
        gpioManager.pullDownWhiteLight();
    }




}
