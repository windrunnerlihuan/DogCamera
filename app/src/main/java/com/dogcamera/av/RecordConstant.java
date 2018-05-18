package com.dogcamera.av;

import android.os.Environment;

/**
 * Created by huanli on 2018/3/3.
 */

public class RecordConstant {

    public static final int RECORD_TIME_MAX = 1000 * 30;//30 s

    public static final String RECORD_DIR = Environment.getExternalStorageDirectory().getPath();

}
