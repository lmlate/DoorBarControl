package com.huaan.doorbar.common;

public interface Constants {
    String APP_ID = "C4H8atN7VHaNGWc8oVyFkswQznPG2BeS2fVVAyDHBcta";
    String SDK_KEY = "HbsmwUgBVizSZkw4AND3zuPnLvJrFNF7RYmwAfHDihdf";

    String IMG_SEARCH_URL = "http://10.128.4.152:8080/warehouse/face/peopleFaceInfo";
    String IMG_DOWNLOAD_URL = "http://10.128.4.152:8080/warehouse/images/";
    String UP_LOAD_URL = "http://10.128.4.152:8080/entranceGuard/faceAuthentication";

    int DELAY_REGISTER_TIME = 5000;
    int DELAY_ADJUST_FOCUS_TIME = 5000;

    long PERIOD_UPDATE_DAY = 24 * 60 * 60 * 1000;
    int UPDATE_TIME_HOUR = 1;//凌晨一点
    int UPDATE_TIME_MINUTE = 0;
    int UPDATE_TIME_SECOND = 0;

    float FACE_SIMILAR_THRESHOLD = 0.8F;//识别度

}

