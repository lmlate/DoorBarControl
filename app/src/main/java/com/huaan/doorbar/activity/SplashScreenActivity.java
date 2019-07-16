package com.huaan.doorbar.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huaan.doorbar.R;
import com.huaan.doorbar.common.Constants;
import com.huaan.doorbar.faceserver.FaceServer;
import com.huaan.doorbar.model.PeopleFaceInfo;
import com.huaan.doorbar.net.OkUtils;
import com.huaan.doorbar.util.ConfigUtil;
import com.huaan.doorbar.util.ImageUtil;
import com.huaan.doorbar.util.other.MyToast;
import com.huaan.doorbar.widget.ProgressDialog;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @description
 * @author: litrainy
 * @create: 2019-06-03 14:19
 **/
public class SplashScreenActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext = SplashScreenActivity.this;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    /**
     * UI控件
     */
    private EditText mSplashEtSimilar;
    private Button mSplashBtnSimilar;
    private EditText mSplashEtIp;
    private Button mSplashBtnIp;
    private Button mSplashBtnDownAngle;
    private TextView mSplashTvAngle;
    private Button mSplashBtnUpAngle;
    private Button mSplashBtnDownFocal;
    private TextView mSplashTvFocal;
    private Button mSplashBtnUpFocal;
    private Button mSplashBtnEngine;
    private TextView mSplashTvEngine;
    private Button mSplashBtnDownload;
    private TextView mSplashTvDownload;
    private Button mSplashBtnRegister;
    private TextView mSplashTvRegister;
    private Button mSplashBtnClear;
    private Button mSplashBtnInto;
    private Button mSplashBtnIntoClose;
    private TextView mSplashTvSimilar;
    private TextView mSplashTvIp;

    private ProgressDialog progressDialog;
    //应下载的图片数量
    private int mDownLoadImgSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        initView();
        mSharedPreferences = getSharedPreferences(Constants.AppSetting, MODE_PRIVATE);
        //引擎注册
        boolean isActivation = mSharedPreferences.getBoolean("IsActivation", false);
        if (!isActivation) {
            activeEngine();
        }
        boolean isSave = mSharedPreferences.getBoolean("IsSave", false);
        if (isSave) {
            startActivity(new Intent(SplashScreenActivity.this, RecognizeActivity.class));
        }
        initData();

    }

    private void initData() {
        //识别角度
        int angle = ConfigUtil.getFtOrient(this);
        setAngelText(angle);
        //人脸比对阈值
        mSplashTvSimilar.setText(Constants.getFaceSimilar(this) * 100 + "");
        //服务器ip
        mSplashTvIp.setText(Constants.getServerIp(this));
        //摄像头焦距
        int focal_length = mSharedPreferences.getInt("focal_length", 0);
        mSplashTvFocal.setText(focal_length + "");

        progressDialog = new ProgressDialog(this);
    }

    private void setAngelText(int angle) {
        switch (angle) {
            case 1:
                mSplashTvAngle.setText("人脸仅检测0度");
                break;
            case 2:
                mSplashTvAngle.setText("人脸仅检测90度");
                break;
            case 3:
                mSplashTvAngle.setText("人脸仅检测180度");
                break;
            case 4:
                mSplashTvAngle.setText("人脸仅检测270度");
                break;
            case 5:
                mSplashTvAngle.setText("全方向人脸检测");
                break;
            default:
                break;
        }
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
                            mSplashTvEngine.setText(getString(R.string.active_success));
                            mEditor = mSharedPreferences.edit();
                            mEditor.putBoolean("IsActivation", true).apply();
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            mSplashTvEngine.setText(getString(R.string.already_activated));
                            mEditor = mSharedPreferences.edit();
                            mEditor.putBoolean("IsActivation", true).apply();
                        } else {
                            mSplashTvEngine.setText(getString(R.string.active_failed, activeCode));
                            mEditor = mSharedPreferences.edit();
                            mEditor.putBoolean("IsActivation", false).apply();
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
                MyToast.showToast(mContext, getString(R.string.permission_denied));
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

    private void initView() {
        mSplashEtSimilar = findViewById(R.id.splash_et_similar);
        mSplashBtnSimilar = findViewById(R.id.splash_btn_similar);
        mSplashBtnSimilar.setOnClickListener(this);
        mSplashEtIp = findViewById(R.id.splash_et_ip);
        mSplashBtnIp = findViewById(R.id.splash_btn_ip);
        mSplashBtnIp.setOnClickListener(this);
        mSplashBtnDownAngle = findViewById(R.id.splash_btn_down_angle);
        mSplashBtnDownAngle.setOnClickListener(this);
        mSplashTvAngle = findViewById(R.id.splash_tv_angle);
        mSplashBtnUpAngle = findViewById(R.id.splash_btn_up_angle);
        mSplashBtnUpAngle.setOnClickListener(this);
        mSplashBtnDownFocal = findViewById(R.id.splash_btn_down_focal);
        mSplashBtnDownFocal.setOnClickListener(this);
        mSplashTvFocal = findViewById(R.id.splash_tv_focal);
        mSplashBtnUpFocal = findViewById(R.id.splash_btn_up_focal);
        mSplashBtnUpFocal.setOnClickListener(this);
        mSplashBtnEngine = findViewById(R.id.splash_btn_engine);
        mSplashBtnEngine.setOnClickListener(this);
        mSplashTvEngine = findViewById(R.id.splash_tv_engine);
        mSplashBtnDownload = findViewById(R.id.splash_btn_download);
        mSplashBtnDownload.setOnClickListener(this);
        mSplashTvDownload = findViewById(R.id.splash_tv_download);
        mSplashBtnRegister = findViewById(R.id.splash_btn_register);
        mSplashBtnRegister.setOnClickListener(this);
        mSplashTvRegister = findViewById(R.id.splash_tv_register);
        mSplashBtnClear = findViewById(R.id.splash_btn_clear);
        mSplashBtnClear.setOnClickListener(this);
        mSplashBtnInto = findViewById(R.id.splash_btn_into);
        mSplashBtnInto.setOnClickListener(this);
        mSplashBtnIntoClose = findViewById(R.id.splash_btn_into_close);
        mSplashBtnIntoClose.setOnClickListener(this);
        mSplashTvSimilar = findViewById(R.id.splash_tv_similar);
        mSplashEtSimilar = findViewById(R.id.splash_et_similar);
        mSplashTvIp = findViewById(R.id.splash_tv_ip);
        mSplashEtIp = findViewById(R.id.splash_et_ip);
        mSplashTvAngle = findViewById(R.id.splash_tv_angle);
        mSplashTvFocal = findViewById(R.id.splash_tv_focal);
        mSplashTvEngine = findViewById(R.id.splash_tv_engine);
        mSplashTvDownload = findViewById(R.id.splash_tv_download);
        mSplashTvRegister = findViewById(R.id.splash_tv_register);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.splash_btn_similar:
                if (!TextUtils.isEmpty(mSplashEtSimilar.getText())) {
                    int similar = Integer.parseInt(mSplashEtSimilar.getText().toString());
                    if (similar >= 0 && similar <= 99) {
                        Constants.setFaceSimilar(this, similar / 100);
                        mSplashTvSimilar.setText(similar + "%");
                        mSplashEtSimilar.getText().clear();
                        MyToast.showToast(this, "设置成功");
                    } else {
                        MyToast.showToast(this, "阈值在0-99之间");
                    }
                } else {
                    MyToast.showToast(this, "填写不能为空");
                }
                break;
            case R.id.splash_btn_ip:
                if (!TextUtils.isEmpty(mSplashEtIp.getText())) {
                    String ip = mSplashEtIp.getText().toString();
                    if (Pattern.compile("((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}").matcher(ip).matches()) {
                        Constants.setServerIp(this, ip);
                        mSplashTvIp.setText(ip);
                        mSplashEtIp.getText().clear();
                        MyToast.showToast(this, "设置成功");
                    } else {
                        MyToast.showToast(this, "ip格式错误");
                    }
                } else {
                    MyToast.showToast(this, "填写不能为空");
                }
                break;
            case R.id.splash_btn_down_angle:
                int ftOrientDown = ConfigUtil.getFtOrient(this);
                if (ftOrientDown == 1) {
                    return;
                } else {
                    ConfigUtil.setFtOrient(this, ftOrientDown - 1);
                    setAngelText(ftOrientDown - 1);
                }
                break;
            case R.id.splash_btn_up_angle:
                int ftOrientUp = ConfigUtil.getFtOrient(this);
                if (ftOrientUp == 5) {
                    return;
                } else {
                    ConfigUtil.setFtOrient(this, ftOrientUp + 1);
                    setAngelText(ftOrientUp + 1);
                }
                break;
            case R.id.splash_btn_down_focal:
                int focal_length_down = mSharedPreferences.getInt("focal_length", 0);
                if (focal_length_down == 0) {
                    return;
                } else {
                    mEditor.putInt("focal_length", focal_length_down - 1);
                    mSplashTvFocal.setText((focal_length_down - 1) + "");
                }
                break;
            case R.id.splash_btn_up_focal:
                int focal_length_up = mSharedPreferences.getInt("focal_length", 0);
                if (focal_length_up == 0) {
                    return;
                } else {
                    mEditor.putInt("focal_length", focal_length_up - 1);
                    mSplashTvFocal.setText((focal_length_up + 1) + "");
                }
                break;
            case R.id.splash_btn_engine:
                activeEngine();
                break;
            case R.id.splash_btn_download:
                downLoad();
                break;
            case R.id.splash_btn_register:
                doRegister();
                break;
            case R.id.splash_btn_clear:
                clearLocalPicture();
                break;
            case R.id.splash_btn_into:
                startActivity(new Intent(SplashScreenActivity.this, RecognizeActivity.class));
                break;
            case R.id.splash_btn_into_close:
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("确认对话框")
                        .setMessage("请确认已经设置完毕再点击")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditor = mSharedPreferences.edit();
                                mEditor.putBoolean("IsSave", true).apply();
                                startActivity(new Intent(SplashScreenActivity.this, RecognizeActivity.class));
                                finish();
                            }
                        })
                        .setNegativeButton("取消", null);
                builder.show();

                break;
            default:
                break;
        }
    }

    /**
     * 下载图片
     */
    private List<PeopleFaceInfo> mInfoList = new ArrayList<>();

    private void downLoad() {
        Request request = new Request.Builder().url(Constants.IMG_SEARCH_URL).build();
        OkUtils.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    MyToast.showToast(mContext, "网络未连接");
                    doRegister();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    Gson gson = new Gson();
                    mInfoList = gson.fromJson(body, new TypeToken<List<PeopleFaceInfo>>() {
                    }.getType());
                    runOnUiThread(() -> {
//                        clearLocalPicture();//开始下载前清空本地照片
                        mEditor = mSharedPreferences.edit();
                        mDownLoadImgSize = mInfoList.size();
                        for (int i = 0; i <= mDownLoadImgSize; i++) {
                            if (mDownLoadImgSize == i) {
                                new Handler().postDelayed(() -> {
                                    mEditor.apply();
                                    clearFaces();
                                    doRegister();
                                }, Constants.DELAY_REGISTER_TIME);
                            } else {
                                final String faceImage = mInfoList.get(i).getFaceImage();
                                final String name = mInfoList.get(i).getName();
                                final String id = mInfoList.get(i).getId();
                                OkUtils.download(Constants.IMG_DOWNLOAD_URL + faceImage, name + ".jpg", REGISTER_DIR, new OkUtils.OnDownloadListener() {
                                    @Override
                                    public void onDownloadSuccess() {
                                        Logger.e("下载成功" + name);
                                        mEditor.putString(name, id);
                                    }

                                    @Override
                                    public void onDownloading(int progress) {
//                                            Logger.e("下载中：" + progress);
                                    }

                                    @Override
                                    public void onDownloadFailed() {
                                        Logger.e("下载失败" + name);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }


    /**
     * 人脸注册
     */
    private void doRegister() {
        File dir = new File(REGISTER_DIR);
        if (!dir.exists()) {
            MyToast.showToast(this, REGISTER_DIR + "该目录不存在");
            return;
        }
        if (!dir.isDirectory()) {
            MyToast.showToast(this, REGISTER_DIR + "不是目录");
            return;
        }
        final File[] jpgFiles = dir.listFiles((dir1, name) -> name.endsWith(FaceServer.IMG_SUFFIX));
        executorService.execute(() -> {
            final int totalCount = jpgFiles.length;
            if (totalCount != mDownLoadImgSize) {
                MyToast.showLongToast(mContext, "已下载数量:" + totalCount + "\n应下载数量:" + mDownLoadImgSize + "\n请联系管理员核对！");
            }
            int successCount = 0;
            runOnUiThread(() -> {
                progressDialog.setMaxProgress(totalCount);
                progressDialog.show();
            });
            for (int i = 0; i < totalCount; i++) {
                final int finalI = i;
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.refreshProgress(finalI);
                    }
                });
                final File jpgFile = jpgFiles[i];
                Bitmap bitmap = BitmapFactory.decodeFile(jpgFile.getAbsolutePath());
                if (bitmap == null) {
                    File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                    if (!failedFile.getParentFile().exists()) {
                        failedFile.getParentFile().mkdirs();
                    }
                    jpgFile.renameTo(failedFile);
                    continue;
                }
                bitmap = ImageUtil.alignBitmapForNv21(bitmap);
                if (bitmap == null) {
                    File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                    if (!failedFile.getParentFile().exists()) {
                        failedFile.getParentFile().mkdirs();
                    }
                    jpgFile.renameTo(failedFile);
                    continue;
                }
                byte[] nv21 = ImageUtil.bitmapToNv21(bitmap, bitmap.getWidth(), bitmap.getHeight());
                boolean success = FaceServer.getInstance().register(this, nv21, bitmap.getWidth(), bitmap.getHeight(),
                        jpgFile.getName().substring(0, jpgFile.getName().lastIndexOf(".")));
                if (!success) {
                    File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                    if (!failedFile.getParentFile().exists()) {
                        failedFile.getParentFile().mkdirs();
                    }
                    jpgFile.renameTo(failedFile);
                } else {
                    successCount++;
                }
            }
            final int finalSuccessCount = successCount;
            tvNotificationRegisterResult = new StringBuilder();
            runOnUiThread(() -> {
//                        progressDialog.dismiss();
                tvNotificationRegisterResult.append("注册完成，一共：" + totalCount + "张\n成功：" + finalSuccessCount + "张\n失败：" + (totalCount - finalSuccessCount));
//                        Logger.e(tvNotificationRegisterResult.toString());
                MyToast.showToast(mContext, tvNotificationRegisterResult.toString());
            });
            Logger.i("注册成功 " + executorService.isShutdown());
        });
        //刷新缓存
        MediaScannerConnection.scanFile(mContext, new String[]{dir.getAbsolutePath()}, null, null);
    }

    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "facecontrol";
    private static final String REGISTER_DIR = ROOT_DIR + File.separator + "register";
    private static final String REGISTER_FAILED_DIR = ROOT_DIR + File.separator + "failed";


    //    ProgressDialog progressDialog = null;
    private StringBuilder tvNotificationRegisterResult;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 人脸清空
     */
    public void clearFaces() {
        int faceNum = FaceServer.getInstance().getFaceNumber(this);
        if (faceNum == 0) {
            MyToast.showToast(mContext, R.string.no_face_need_to_delete);
        } else {
            final int deleteCount = FaceServer.getInstance().clearAllFaces(mContext);

            MyToast.showToast(mContext, deleteCount + "张已删除");
        }
    }


    /**
     * 本地照片清空
     */
    private void clearLocalPicture() {
        deleteFile(new File(REGISTER_DIR));
        deleteFile(new File(REGISTER_FAILED_DIR));
    }

    /**
     * 删除文件
     * @param file 要删除的文件夹的所在位置
     */
    private void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
//            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}
