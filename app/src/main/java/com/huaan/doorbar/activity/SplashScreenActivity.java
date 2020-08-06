package com.huaan.doorbar.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huaan.doorbar.R;
import com.huaan.doorbar.common.Constants;
import com.huaan.doorbar.db.DBHelper;
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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * UI 控件
     */
    private TextView mSplashTvSimilar;
    private EditText mSplashEtSimilar;
    private Button mSplashBtnSimilar;
    private TextView mSplashTvIp;
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
    private TextView mSplashTvPort;
    private EditText mSplashEtPort;
    private Button mSplashBtnPort;
    private Button mSplashBtnClearLocalPicture;
    private TextView mSplashTvSocketPort;
    private EditText mSplashEtSocketPort;
    private Button mSplashBtnSocketPort;
    private TextView mSplashTvMineIp;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private int mDownLoadImgSize = 0;//应下载的图片数量
    private int mSucceedCount = 0;//已下载数量
    private int mFailedCount = 0;//下载失败数量
    private List<PeopleFaceInfo> mInfoList = new ArrayList<>();//下载图片
    private Map<String, String> mPeopleMap = new HashMap<>();

    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "facecontrol";
    private static final String REGISTER_DIR = ROOT_DIR + File.separator + "register";
    private static final String REGISTER_FAILED_DIR = ROOT_DIR + File.separator + "failed";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private StringBuilder tvNotificationRegisterResult;

    private AlertDialog mAlertDialog;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private ProgressDialog progressDialog;

    private String mIp;
    private String mPort;

    private DBHelper mHelper;
    private SQLiteDatabase mDatabase;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initView();
        mSharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        //激活验证
        activeEngine();
        boolean isSave = mSharedPreferences.getBoolean("IsSave", false);
        if (isSave) {
            startActivity(new Intent(mContext, RecognizeActivity.class));
        }

        //数据库初始化
        mHelper = new DBHelper(this);
        mDatabase = mHelper.getWritableDatabase();

        //加载UI数据
        initData();
        FaceServer.getInstance().init(this);

    }

    private void initData() {
        //识别角度
        int angle = ConfigUtil.getFtOrient(this);
        setAngelText(angle);
        //人脸比对阈值
        mSplashTvSimilar.setText(Constants.getFaceSimilar(this) * 100 + "%");
        //服务器ip
        mIp = Constants.getServerIp(this);
        mSplashTvIp.setText(mIp);
        //服务器端口
        mPort = Constants.getServerPort(this);
        mSplashTvPort.setText(mPort);
        //Socket端口
        mSplashTvSocketPort.setText(Constants.getSocketPort(this) + "");
        //本机IP
        mSplashTvMineIp.setText(getHostIP());
        //摄像头焦距
        int focal_length = mSharedPreferences.getInt("focal_length", 0);
        mSplashTvFocal.setText(focal_length + "");

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
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            mSplashTvEngine.setText(getString(R.string.already_activated));
                        } else {
                            mSplashTvEngine.setText(getString(R.string.active_failed, activeCode));
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
        mSplashTvSimilar = findViewById(R.id.splash_tv_similar);
        mSplashEtSimilar = findViewById(R.id.splash_et_similar);
        mSplashBtnSimilar = findViewById(R.id.splash_btn_similar);
        mSplashBtnSimilar.setOnClickListener(this);
        mSplashTvIp = findViewById(R.id.splash_tv_ip);
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
        mSplashTvPort = findViewById(R.id.splash_tv_port);
        mSplashEtPort = findViewById(R.id.splash_et_port);
        mSplashBtnPort = findViewById(R.id.splash_btn_port);
        mSplashBtnPort.setOnClickListener(this);
        mSplashBtnClearLocalPicture = findViewById(R.id.splash_btn_clear_local_picture);
        mSplashBtnClearLocalPicture.setOnClickListener(this);
        mSplashTvSocketPort = findViewById(R.id.splash_tv_socket_port);
        mSplashEtSocketPort = findViewById(R.id.splash_et_socket_port);
        mSplashBtnSocketPort = findViewById(R.id.splash_btn_socket_port);
        mSplashBtnSocketPort.setOnClickListener(this);
        mSplashTvMineIp = findViewById(R.id.splash_tv_mine_ip);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.splash_btn_similar:
                if (!TextUtils.isEmpty(mSplashEtSimilar.getText())) {
                    float similar = Float.parseFloat(mSplashEtSimilar.getText().toString());
                    if (similar >= 0 && similar <= 99) {
                        Constants.setFaceSimilar(this, similar / 100);
                        mSplashTvSimilar.setText(similar + "%");
                        MyToast.showToast(this, "人脸阈值设置成功");
                    } else {
                        MyToast.showToast(this, "请设置阈值在0-99之间");
                    }
                } else {
                    MyToast.showToast(this, "填写不能为空");
                }
                break;
            case R.id.splash_btn_ip:
                if (!TextUtils.isEmpty(mSplashEtIp.getText())) {
                    String ip = mSplashEtIp.getText().toString();
                    if (Pattern.compile("((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))").matcher(ip).matches()) {
                        Constants.setServerIp(this, ip);
                        mSplashTvIp.setText(ip);
                        MyToast.showToast(this, "IP设置成功");
                    } else {
                        MyToast.showToast(this, "ip格式错误");
                    }
                } else {
                    MyToast.showToast(this, "填写不能为空");
                }
                break;
            case R.id.splash_btn_port:
                if (!TextUtils.isEmpty(mSplashEtPort.getText())) {
                    String port = mSplashEtPort.getText().toString();
                    if (Pattern.compile("^[1-9]$|(^[1-9][0-9]$)|(^[1-9][0-9][0-9]$)|(^[1-9][0-9][0-9][0-9]$)|(^[1-6][0-5][0-5][0-3][0-5]$)").matcher(port).matches()) {
                        Constants.setServerPort(this, port);
                        mSplashTvPort.setText(port);
                        MyToast.showToast(this, "端口号设置成功");
                    } else {
                        MyToast.showToast(this, "端口号格式错误");
                    }
                } else {
                    MyToast.showToast(this, "填写不能为空");
                }
                break;
            case R.id.splash_btn_socket_port:
                if (!TextUtils.isEmpty(mSplashEtSocketPort.getText())) {
                    String SocketPort = mSplashEtSocketPort.getText().toString();
                    if (Pattern.compile("^[1-9]$|(^[1-9][0-9]$)|(^[1-9][0-9][0-9]$)|(^[1-9][0-9][0-9][0-9]$)|(^[1-6][0-5][0-5][0-3][0-5]$)").matcher(SocketPort).matches()) {
                        Constants.setSocketPort(this, Integer.parseInt(SocketPort));
                        mSplashTvSocketPort.setText(SocketPort);
                        MyToast.showToast(this, "Socket端口号设置成功");
                    } else {
                        MyToast.showToast(this, "Socket端口号格式错误");
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
                    mSplashTvFocal.setText((focal_length_down - 1) + "");
                    mEditor.putInt("focal_length", (focal_length_down - 1)).apply();
                }
                break;
            case R.id.splash_btn_up_focal:
                int focal_length_up = mSharedPreferences.getInt("focal_length", 0);
                if (focal_length_up == 40) {
                    return;
                } else {
                    mSplashTvFocal.setText((focal_length_up + 1) + "");
                    mEditor.putInt("focal_length", focal_length_up + 1).apply();
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
                clearFaces();
                break;
            case R.id.splash_btn_clear_local_picture:
                clearLocalPicture();
                break;
            case R.id.splash_btn_into:
                startActivity(new Intent(this, RecognizeActivity.class));
                break;
            case R.id.splash_btn_into_close:
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("确认对话框")
                        .setMessage("请确认已经设置完毕再点击")
                        .setPositiveButton("确定", (dialog, which) -> {
                            mEditor = mSharedPreferences.edit();
                            mEditor.putBoolean("IsSave", true).apply();
                            startActivity(new Intent(SplashScreenActivity.this, RecognizeActivity.class));
                            finish();
                        })
                        .setNegativeButton("取消", null);
                builder.show();
                break;
            default:
                break;
        }
    }


    private void downLoad() {
        Request request = new Request.Builder().url(Constants.getSyncFaceUrl(mIp, mPort)).build();
        OkUtils.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    MyToast.showToast(mContext, "网络未连接");
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    String body = null;
                    try {
                        body = response.body().string();
                        Gson gson = new Gson();
                        mInfoList = gson.fromJson(body, new TypeToken<List<PeopleFaceInfo>>() {
                        }.getType());
                        runOnUiThread(() -> {
                            mDownLoadImgSize = mInfoList.size();
                            mSucceedCount = 0;
                            mFailedCount = 0;
                            mPeopleMap.clear();
                            for (int i = 0; i < mDownLoadImgSize; i++) {
                                final String faceImage = mInfoList.get(i).getFaceImage();
                                final String name = mInfoList.get(i).getName();
                                final String id = mInfoList.get(i).getId();
                                mPeopleMap.put(name, id);
                                runOnUiThread(() -> showDownloadDialog(mDownLoadImgSize));
                                OkUtils.download(Constants.getImageDownloadUrl(mIp, mPort) + faceImage, name + faceImage.substring(faceImage.indexOf(".")), REGISTER_DIR, new OkUtils.OnDownloadListener() {
                                    @Override
                                    public void onDownloadSuccess() {
                                        runOnUiThread(() -> {
                                            Logger.e("下载成功" + name);
                                            mSucceedCount++;
                                            if (mAlertDialog != null) {
                                                if (progressBar != null) {
                                                    progressBar.setProgress(mSucceedCount);
                                                }
                                                if (tvProgress != null) {
                                                    tvProgress.setText("进度： " + mSucceedCount + " / " + mDownLoadImgSize);
                                                }
                                                if (mSucceedCount == mDownLoadImgSize) {
                                                    mAlertDialog.dismiss();
                                                }
                                            }
                                            if (mSucceedCount == mDownLoadImgSize) {
                                                if (mAlertDialog != null) {
                                                    mAlertDialog.dismiss();
                                                    mAlertDialog = null;
                                                }
                                                mSplashTvDownload.setText("下载成功:" + mSucceedCount + "张\n下载失败:" + mFailedCount + "张");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onDownloading(int progress) {
                                        Logger.e("下载" + name + "中：" + progress);
                                        runOnUiThread(() -> {
                                            tvProgress.setText("进度： " + mSucceedCount + " / " + mDownLoadImgSize + "\n" + name + ":" + progress + "%");
                                        });
                                    }

                                    @Override
                                    public void onDownloadFailed() {
                                        Logger.e("下载失败" + name);
                                        mFailedCount++;
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

    }

    @SuppressLint("RestrictedApi")
    private void showDownloadDialog(int max) {
        progressBar = (ProgressBar) LayoutInflater.from(mContext).inflate(R.layout.horizontal_progress_bar, null);
        progressBar.setMax(max);
        tvProgress = new TextView(mContext);
        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(progressBar);
        linearLayout.addView(tvProgress);
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setView(linearLayout, 50, 50, 50, 50)
                .setCancelable(false)
                .setTitle("重要操作，请勿关闭")
                .show();
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
            int successCount = 0;
            runOnUiThread(() -> {
                progressDialog = null;
                progressDialog = new ProgressDialog(mContext);
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

            List<String> nameList = getFilesAllName(REGISTER_DIR);
            if (nameList.size() != 0) {
                insertData(nameList);
                clearLocalPicture();//清空本地照片
            }

            tvNotificationRegisterResult = new StringBuilder();
            runOnUiThread(() -> {
                progressDialog.dismiss();
                tvNotificationRegisterResult.append("注册完成，一共：" + totalCount + "张\n成功：" + finalSuccessCount + "张,失败：" + (totalCount - finalSuccessCount) + "张");
                mSplashTvRegister.setText(tvNotificationRegisterResult.toString());
            });
            Logger.i("注册成功 " + executorService.isShutdown());
        });
        //刷新缓存
        MediaScannerConnection.scanFile(mContext, new String[]{dir.getAbsolutePath()}, null, null);
    }

    /**
     * 注册数据
     *
     * @param nameList
     */
    private void insertData(List<String> nameList) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < nameList.size(); i++) {
            String name = nameList.get(i);
            String id = mPeopleMap.get(name);
            values.put(DBHelper.NAME, name);
            values.put(DBHelper.ID, id);
        }
//        mDatabase.insert(DBHelper.TABLE_NAME, null, values);
        mDatabase.insertWithOnConflict(DBHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * 人脸清空
     */
    public void clearFaces() {
        int faceNum = FaceServer.getInstance().getFaceNumber(this);
        if (faceNum == 0) {
            Toast.makeText(this, R.string.no_face_need_to_delete, Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.notification)
                    .setMessage(getString(R.string.confirm_delete, faceNum))
                    .setPositiveButton(R.string.ok, (dialog1, which) -> {
                        int deleteCount = FaceServer.getInstance().clearAllFaces(mContext);
                        Toast.makeText(mContext, deleteCount + "人脸已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
        }

    }

    /**
     * 拦截事件隐藏软键盘「原生系统的缺陷」
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isShouldHideKeyBord(view, ev)) {
                hideSoftInput(view.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判定当前是否需要隐藏
     */
    protected boolean isShouldHideKeyBord(View v, MotionEvent ev) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            return !(ev.getX() > left && ev.getX() < right && ev.getY() > top && ev.getY() < bottom);
        }
        return false;
    }

    /**
     * 隐藏软键盘
     */
    private void hideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
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

    /**
     * 获取文件名字
     */
    public static List<String> getFilesAllName(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            Log.e("error", "空目录");
            return null;
        }
        List<String> mList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            String str = files[i].getName();
            mList.add(str.substring(0, str.indexOf(".")));
        }
        return mList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        FaceServer.getInstance().unInit();
    }


    public String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

}
