package com.huaan.doorbar.activity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.icu.util.Calendar;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.SeekBar;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huaan.doorbar.R;
import com.huaan.doorbar.common.Constants;
import com.huaan.doorbar.faceserver.CompareResult;
import com.huaan.doorbar.faceserver.FaceServer;
import com.huaan.doorbar.model.DrawInfo;
import com.huaan.doorbar.model.FacePreviewInfo;
import com.huaan.doorbar.model.PeopleFaceInfo;
import com.huaan.doorbar.net.OkUtils;
import com.huaan.doorbar.util.ConfigUtil;
import com.huaan.doorbar.util.DrawHelper;
import com.huaan.doorbar.util.ImageUtil;
import com.huaan.doorbar.util.camera.CameraHelper;
import com.huaan.doorbar.util.camera.CameraListener;
import com.huaan.doorbar.util.face.FaceHelper;
import com.huaan.doorbar.util.face.FaceListener;
import com.huaan.doorbar.util.face.RequestFeatureStatus;
import com.huaan.doorbar.util.other.MotherboardUtil;
import com.huaan.doorbar.util.other.MyToast;
import com.huaan.doorbar.widget.FaceRectView;
import com.huaan.doorbar.widget.ShowFaceInfoAdapter;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import static com.huaan.doorbar.util.other.MotherboardUtil.pullDownLight;

@RequiresApi(api = Build.VERSION_CODES.N)
public class RecognizeActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * 最大检测人数
     */
    private static final int MAX_DETECT_NUM = 5;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 50;
    /**
     * 相机
     */
    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * 优先打开的摄像头
     */
    private Integer cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;

    private FaceEngine faceEngine;
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    private ShowFaceInfoAdapter adapter;
    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;

    private int afCode = -1;
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;

    /**
     * 唯一权限请求码
     */
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 识别度
     */
    private static final float SIMILAR_THRESHOLD = Constants.FACE_SIMILAR_THRESHOLD;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private Context mContext = RecognizeActivity.this;

    /**
     * 定时下载
     */
    private Timer mTimer;

    /**
     * 焦距调节
     */
    private SeekBar mSeekBar;
    private boolean mIsDoIt = false;//隐藏条件
    //应下载的图片数量
    private int mDownLoadImgSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_and_recognize);

        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        //初始化主板接口
        MotherboardUtil.init(this);

        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        //本地人脸库初始化
        FaceServer.getInstance().init(this);

        previewView = findViewById(R.id.texture_preview);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        faceRectView = findViewById(R.id.face_rect_view);

        RecyclerView recyclerShowFaceInfo = findViewById(R.id.recycler_view_person);
        compareResultList = new ArrayList<>();
        adapter = new ShowFaceInfoAdapter(compareResultList, this);
        recyclerShowFaceInfo.setAdapter(adapter);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        recyclerShowFaceInfo.setLayoutManager(new LinearLayoutManager(this));
        recyclerShowFaceInfo.setItemAnimator(new DefaultItemAnimator());

//        progressDialog = new ProgressDialog(this);
        mSharedPreferences = getSharedPreferences("faceDetail", Context.MODE_PRIVATE);
        downLoad();
        downLoadTask();


        mSeekBar = findViewById(R.id.focal_length);
        int focal_length = mSharedPreferences.getInt("focal_length", 0);
        mSeekBar.setProgress(focal_length);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cameraHelper.setZoom(progress);
                mEditor.putInt("focal_length", progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDoIt = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new Handler().postDelayed(() -> runOnUiThread(() -> mSeekBar.setVisibility(View.INVISIBLE)), Constants.DELAY_ADJUST_FOCUS_TIME);
            }
        });
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            if (!mIsDoIt) {
                mSeekBar.setVisibility(View.INVISIBLE);
            }
        }), Constants.DELAY_ADJUST_FOCUS_TIME);

    }

    /**
     * 定时任务
     */
    private void downLoadTask() {
        mTimer = new Timer();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, Constants.UPDATE_TIME_HOUR); //凌晨1点
        calendar.set(Calendar.MINUTE, Constants.UPDATE_TIME_MINUTE);
        calendar.set(Calendar.SECOND, Constants.UPDATE_TIME_SECOND);
        Date date = calendar.getTime();
        //如果第一次执行定时任务的时间 小于当前的时间，此时添加一天
        if (date.before(new Date())) {
            date = this.addDay(date, 1);
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> downLoad());
            }
        }, date, Constants.PERIOD_UPDATE_DAY);
    }

    /**
     * 增加或减少天数
     */
    private Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }


    /**
     * 初始化引擎
     */
    private void initEngine() {

        faceEngine = new FaceEngine();
        afCode = faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Logger.i("initEngine:  init: " + afCode + "  version:" + versionInfo);
        if (afCode != ErrorInfo.MOK) {
            MyToast.showToast(mContext, getString(R.string.init_failed, afCode));
        }
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {
        if (afCode == ErrorInfo.MOK) {
            afCode = faceEngine.unInit();
            Logger.i("unInitEngine: " + afCode);
        }
    }


    @Override
    protected void onDestroy() {

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized (faceHelper) {
                unInitEngine();
            }
            ConfigUtil.setTrackId(this, faceHelper.getCurrentTrackId());
            faceHelper.release();
        } else {
            unInitEngine();
        }
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.dispose();
            getFeatureDelayedDisposables.clear();
        }
        //-------------------------------//
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
        //-------------------------------//
        FaceServer.getInstance().unInit();
        pullDownLight();
        super.onDestroy();
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

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Logger.e("onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId) {
                //FR成功
                if (faceFeature != null) {
                    Logger.i("onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);

                    //活体检测通过，搜索特征
                    if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，延迟100ms再执行该函数
                    else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.UNKNOWN) {
                        getFeatureDelayedDisposables.add(Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                .subscribe(aLong -> onFaceFeatureInfoGet(faceFeature, requestId)));
                    }
                    //活体检测失败
                    else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.NOT_ALIVE);
//                        runOnUiThread(() -> MyToast.showToast(mContext, "未检测到活体"));
//                        MotherboardUtil.Failure();
                    }

                }
                //FR 失败
                else {
                    requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);

                }
            }

        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation, cameraId, isMirror);

                faceHelper = new FaceHelper.Builder()
                        .faceEngine(faceEngine)
                        .frThreadNum(MAX_DETECT_NUM)
                        .previewSize(previewSize)
                        .faceListener(faceListener)
                        .currentTrackId(ConfigUtil.getTrackId(mContext.getApplicationContext()))
                        .build();
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }

                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21, mContext);//多人警告，暂不拒绝

                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    List<DrawInfo> drawInfoList = new ArrayList<>();
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
//                        drawInfoList.add(new DrawInfo(facePreviewInfoList.get(i).getFaceInfo().getRect(), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, LivenessInfo.UNKNOWN, name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
                        drawInfoList.add(new DrawInfo(facePreviewInfoList.get(i).getFaceInfo().getRect(), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, LivenessInfo.UNKNOWN, ""));
                    }
                    drawHelper.draw(faceRectView, drawInfoList);
                }

                clearLeftFace(facePreviewInfoList);

                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {

                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        if (livenessDetect) {
                            livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                         * FR回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer)}中回传
                         */
                        if (requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == null || requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == RequestFeatureStatus.FAILED) {
                            requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
                            Logger.i("onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackId());
                        }
                    }
                }
            }

            @Override
            public void onCameraClosed() {
                Logger.i("onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Logger.i("onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Logger.i("onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(cameraID != null ? cameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
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
                initEngine();
                initCamera();
                if (cameraHelper != null) {
                    cameraHelper.start();
                }
            } else {
                MyToast.showToast(this, R.string.permission_denied);
            }
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        Set<Integer> keySet = requestFeatureStatusMap.keySet();
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                if (!keySet.contains(compareResultList.get(i).getTrackId())) {
                    compareResultList.remove(i);
                    adapter.notifyItemRemoved(i);
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            return;
        }

        for (Integer integer : keySet) {
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == integer) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(integer);
                livenessMap.remove(integer);
            }
        }

    }

    /**
     * 人脸比对
     *
     * @param frFace
     * @param requestId
     */
    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable
                .create((ObservableOnSubscribe<CompareResult>) emitter -> {
                    Logger.i("subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                    CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
                    Logger.i("subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    if (compareResult == null) {
                        emitter.onError(null);
                    } else {
                        emitter.onNext(compareResult);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, "未知" + requestId);
                            return;
                        }

                        Logger.i("onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {

                                //认证成功 回调请求
                                String id = mSharedPreferences.getString(compareResult.getUserName(), "");
                                Map map = new HashMap();
                                map.put("userId", id);
                                OkUtils.UploadSJ(Constants.UP_LOAD_URL, map, new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        Logger.e("回调失败");

                                    }
                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        Logger.e("回调成功");
                                        //MotherboardUtil.Succeed();
                                    }
                                });



                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.addName(requestId, "VISITOR " + requestId);
                                return;
                            }
                            for (CompareResult compareResult1 : compareResultList) {
                                if (compareResult1.getTrackId() == requestId) {
                                    isAdded = true;
                                    break;
                                }
                            }
                            if (!isAdded) {
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    compareResultList.remove(0);
                                    adapter.notifyItemRemoved(0);
                                }
                                //添加显示人员时，保存其trackId
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);
                                adapter.notifyItemInserted(compareResultList.size() - 1);
                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.addName(requestId, compareResult.getUserName());
                        } else if (compareResult.getSimilar() > 0.5F) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, "VISITOR " + requestId);
                            //补光增加辨识度
//                            new Thread(MotherboardUtil::FillLight).start();
                        } else {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, "VISITOR " + requestId);
                        }
                    }
                    @Override
                    public void onError(Throwable e) {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initEngine();
            initCamera();
        }
    }

    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "facecontrol";
    private static final String REGISTER_DIR = ROOT_DIR + File.separator + "register";
    private static final String REGISTER_FAILED_DIR = ROOT_DIR + File.separator + "failed";

    private boolean flag = false;

    /**
     * 下载图片
     */
    private List<PeopleFaceInfo> mInfoList = new ArrayList<>();

    private void downLoad() {
        Request request = new Request.Builder().url(Constants.IMG_SEARCH_URL).build();
        OkUtils.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> MyToast.showToast(mContext, "网络未连接"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    Gson gson = new Gson();
                    mInfoList = gson.fromJson(body, new TypeToken<List<PeopleFaceInfo>>() {
                    }.getType());
                    runOnUiThread(() -> {
                        clearLocalPicture();//开始下载前清空本地照片
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

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    //    ProgressDialog progressDialog = null;
    private StringBuilder tvNotificationRegisterResult;

    /**
     * 人脸注册
     */
    private void doRegister() {
        File dir = new File(REGISTER_DIR);
        MediaScannerConnection.scanFile(mContext, new String[]{dir.getAbsolutePath()}, null, null);
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
//               runOnUiThread(()->{
//                   progressDialog.setMaxProgress(totalCount);
//                   progressDialog.show();
//               });
            for (int i = 0; i < totalCount; i++) {
                final int finalI = i;
//                    runOnUiThread(()->{
//                        if (progressDialog != null) {
//                            progressDialog.refreshProgress(finalI);
//                        }
//                    });
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
                boolean success = FaceServer.getInstance().register(mContext, nv21, bitmap.getWidth(), bitmap.getHeight()
                        , jpgFile.getName().substring(0, jpgFile.getName().lastIndexOf(".")));
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
    }

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


}
