package com.huaan.doorbar.net;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @description
 * @author: litrainy
 * @create: 2019-05-07 09:55
 **/
public class OkUtils {
    private Context mContext;
    private static OkHttpClient sOkHttpClient = null;
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/jpg");
    private static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");
    private Map<String, String> map = new HashMap<String, String>();//存放
    private List<String> list = new ArrayList<String>();
    private static File file;
    private static String imgpath;
    private static String imageName;

    private OkUtils() {
    }


    /**
     *单例获取
     **/
    public static OkHttpClient getInstance() {
        if (sOkHttpClient == null) {
            synchronized (OkUtils.class) {
                if (sOkHttpClient == null)
                    sOkHttpClient = new OkHttpClient();
            }
        }
        return sOkHttpClient;
    }

    /**
     * 下载文件
     * @param imgPath      文件网址
     * @param saveDir  保存路径
     * @param listener 回调不用管
     */
    public static void download(final String imgPath, final String imageName,final String saveDir, final OnDownloadListener listener) {
        sOkHttpClient = getInstance();
        Request request = new Request.Builder().url(imgPath).build();
        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                String savePath = isExistDir(saveDir);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
//                    File file = new File(savePath, getNameFromUrl(imageName));
                    File file = new File(savePath, imageName);
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
                    listener.onDownloadSuccess();
                } catch (Exception e) {
                    listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {

                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {

                    }
                }
            }
        });
    }



    /**
     * @param saveDir * @return * @throws IOException * 判断下载目录是否存在
     */
    private static String isExistDir(String saveDir) throws IOException {
        // 下载位置
//        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        File downloadFile = new File(saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    /**
     * @param imageName * @return * 从下载连接中解析出文件名
     */
    @NonNull
    private static String getNameFromUrl(String imageName) {
        return imageName.substring(0,imageName.lastIndexOf("_"));
    }

    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess();

        /**
         * @param progress * 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载失败
         */
        void onDownloadFailed();
    }





















    /**
     * 键值对上传数据
     **/
    public static void UploadSJ(String url, Map<String, String> map, Callback callback) {
        FormBody.Builder builder = new FormBody.Builder();
        //遍历map中所有的参数到builder
        for (String key : map.keySet()) {
            builder.add(key, map.get(key));
            Log.e("", "key: " + key + "   map.get:  " + map.get(key));
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     * 上传一张图片带参数
     **/
    public static void UploadFileCS(String url, String key1, String path, Map<String, String> map, Callback callback) {
        String imagpath = path.substring(0, path.lastIndexOf("/"));
        String imgName[] = path.split("/");
        for (int i = 0; i < imgName.length; i++) {
            if (i == imgName.length - 1) {
                String name = imgName[i];
                file = new File(imagpath, name);
            }
        }
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody fileBody = RequestBody.create(MEDIA_TYPE_PNG, file);
        //遍历map中所有的参数到builder
        for (String key : map.keySet()) {
            builder.addFormDataPart(key, map.get(key));
        }
        //讲文件添加到builder中
        builder.addFormDataPart(key1, file.getName(), fileBody);
        //创建请求体
        RequestBody requestBody = builder.build();
        OkHttpClient okhttp = getInstance();
        okhttp.newBuilder().followRedirects(true);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     *上传多个图片文件
     **/
    @SuppressWarnings("unused")
    public static void UploadFileMore(String url, List<String> paths, Callback callback) {
        if (paths != null) {
            //创建文件集合
            List<File> list = new ArrayList<File>();
            //遍历整个图片地址
            for (String str : paths) {
                //截取图片地址：/storage/emulated/0
                imgpath = str.substring(0, str.lastIndexOf("/"));
                //将图片路径分解成String数组
                String[] imgName = str.split("/");
                for (int i = 0; i < imgName.length; i++) {
                    if (i == imgName.length - 1) {
                        imageName = imgName[i];//获取图片名称
                        File file = new File(imgpath, imageName);
                        list.add(file);
                    }
                }
            }
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);//设置表单类型
            //遍历图片文件
            for (File file : list) {
                if (file != null) {
                    builder.addFormDataPart("acrd", file.getName(), RequestBody.create(MEDIA_TYPE_PNG, file));
                }
            }
            //构建请求体
            MultipartBody requestBody = builder.build();
            Request request = new Request.Builder().url(url).post(requestBody).build();
            Call call = getInstance().newCall(request);
            call.enqueue(callback);
        }

    }

    /**
     * 上传多张图片带参数
     **/
    @SuppressWarnings("unused")
    public static void UploadFileSCMore(String url, String value, List<String> paths, Map<String, String> map, Callback callback) {
        if (paths != null && map != null) {
            //创建文件集合
            List<File> list = new ArrayList<File>();
            //遍历整个图片地址
            for (String str : paths) {
                //截取图片地址：/storage/emulated/0
                imgpath = str.substring(0, str.lastIndexOf("/"));
                //将图片路径分解成String数组
                String[] imgName = str.split("/");
                for (int i = 0; i < imgName.length; i++) {
                    if (i == imgName.length - 1) {
                        imageName = imgName[i];//获取图片名称
                        File file = new File(imgpath, imageName);
                        list.add(file);
                    }
                }
            }
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);//设置表单类型
            //遍历图片文件
            for (File file : list) {
                if (file != null) {
                    builder.addFormDataPart(value, file.getName(), RequestBody.create(MEDIA_TYPE_PNG, file));
                }
            }
            //遍历map中所有的参数到builder
            for (String key : map.keySet()) {
                builder.addFormDataPart(key, map.get(key));
            }
            RequestBody requestBody = builder.build();

            Request request = new Request.Builder().url(url).post(requestBody).build();
            Call call = getInstance().newCall(request);
            call.enqueue(callback);
        }
    }

    /**
     * post上传json字符串方法
     * url:服务器网址
     *
     * @param
     */
    public static void PostJaon(String url, String json, Callback callback) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request build = new Request.Builder()
                .url(url)
                .patch(requestBody)
                .build();
        Call call = getInstance().newCall(build);
        call.enqueue(callback);
    }


}


