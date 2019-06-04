package com.huaan.doorbar.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.huaan.doorbar.R;
import com.huaan.doorbar.common.Constants;
import com.huaan.doorbar.util.ConfigUtil;
import com.huaan.doorbar.util.other.MyToast;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @description
 * @author: litrainy
 * @create: 2019-06-03 14:19
 **/
public class SplashScreenActivity extends AppCompatActivity {

    private Context mContext = SplashScreenActivity.this;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        ConfigUtil.setFtOrient(this, FaceEngine.ASF_OP_90_ONLY);//识别角度90°,FaceEngine.ASF_OP_0_HIGHER_EXT全方向识别
        activeEngine();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(mContext, RecognizeActivity.class);
            startActivity(intent);
            finish();
        }, 100);
    }

    /**
     * 激活引擎
     */
    public void activeEngine() {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            FaceEngine faceEngine = new FaceEngine();
            int activeCode = faceEngine.active(mContext, Constants.APP_ID, Constants.SDK_KEY);
            emitter.onNext(activeCode);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            MyToast.showToast(mContext,getString(R.string.active_success));
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            MyToast.showToast(mContext,getString(R.string.already_activated));
                        } else {
                            MyToast.showToast(mContext,getString(R.string.active_failed, activeCode));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                activeEngine();
            } else {
               MyToast.showToast(mContext,getString(R.string.permission_denied));
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;// 拦截用户点击返回键
        }
        return super.onKeyDown(keyCode, event);
    }

}
