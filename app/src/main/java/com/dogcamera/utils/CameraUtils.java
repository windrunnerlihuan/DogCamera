package com.dogcamera.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.dogcamera.transcode.utils.VideoDimensionCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by huanli on 2018/3/2.
 */

public class CameraUtils {

    private static final String TAG = "CameraUtils";

    public static int PREVIEW_RESOLUTION_WIDTH = 1280; //视频预览分辨率宽度
    public static int PREVIEW_RESOLUTION_HEIGHT = 720; //视频预览分辨率高度
    public static final float PICSIZERATE = 16.0f / 9f;

    public static int openBackCameraWithFailover(Camera camera){
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        int cameraId = 0;
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null) {
            Log.d(TAG, "No back-facing camera found; opening default");
            camera = Camera.open();    // opens first back-facing camera
        }
        if (camera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        return cameraId;

    }

    public static Camera openCamera(int cameraId){
        Camera camera = Camera.open(cameraId);
        if (camera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        return camera;
    }

    public static void setDefaultParameters(Context context, Camera camera, int cameraId){
        //设置参数
        Camera.Parameters params = camera.getParameters();
        int degrees = getCameraDisplayOrientation((Activity)context, cameraId);
        Log.e(TAG, "摄像头旋转" + degrees + "度");
        //Nexus 5X 机器的摄像头装反了
        if (VideoDimensionCompat.NEXUS_5X.equals(Build.MODEL)) {
            degrees = 360 - degrees;
        }
        params.setRotation(degrees);
        params.setRecordingHint(true);

        List<Camera.Size> previewSizeList = params.getSupportedPreviewSizes();
        List<Camera.Size> pictureSizeList = params.getSupportedPictureSizes();
        Camera.Size previewSize = getPropPicSize(previewSizeList, PICSIZERATE, PREVIEW_RESOLUTION_WIDTH);
        Camera.Size pictureSize = getPropPicSize(pictureSizeList, PICSIZERATE, PREVIEW_RESOLUTION_WIDTH);
        int previewWidth = previewSize.width;
        int previewHeight = previewSize.height;
        int picWidth = pictureSize.width;
        int picHeight = pictureSize.height;
        params.setPreviewSize(previewWidth, previewHeight);
        params.setPictureSize(picWidth, picHeight);
        //设置焦点
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (params.getSupportedFocusModes().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
        }
        //前面params设置过rotation了，这句不用了
//            mCamera.setDisplayOrientation(degress);
        camera.setParameters(params);
    }

    //google 官方推荐获取预览方向的写法。
    public static int getCameraDisplayOrientation(Activity activity, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degree) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degree + 360) % 360;
        }

        return result;
    }

    public static Camera.Size getPropPicSize(List<Camera.Size> sizeList, float sizeRate, int minWidth) {
        Collections.sort(sizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width == rhs.width) {
                    return 0;
                } else if (lhs.width > rhs.width) {
                    return 1;
                } else {

                    return -1;
                }
            }
        });
        int i = 0;
        for (Camera.Size size : sizeList) {
            if (size.width >= minWidth && equalSizeRate(size, sizeRate)) {
                break;
            }
            i++;
        }
        if (i == sizeList.size()) {
            //如果没找到，就选用最大的size
            i -= 1;
        }
        return sizeList.get(i);
    }

    public static boolean equalSizeRate(Camera.Size size, float rate) {
        float sizeRate = ((float) size.width) / ((float) size.height);
        return Math.abs(sizeRate - rate) < 0.2;
    }

    public static void startPreview(Camera camera, byte[] callbackBuffer, SurfaceTexture surfaceTexture){
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(callbackBuffer == null){
            int preWidth = camera.getParameters().getPreviewSize().width;
            int preHeight = camera.getParameters().getPreviewSize().height;
            callbackBuffer = new byte[preWidth * preHeight * 3 / 2];
        }
        camera.addCallbackBuffer(callbackBuffer);
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                camera.addCallbackBuffer(bytes);
            }
        });
        camera.startPreview();
    }


    public static void focusCameraTouch(Camera camera, int previewWidth, int previewHeight, float x, float y) {
        if (camera != null) {
            Rect focusRect = calculateTapArea(x, y, 1f, previewWidth, previewHeight);
            Rect meteringRect = calculateTapArea(x, y, 1.5f, previewWidth, previewHeight);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                focusAreas.add(new Camera.Area(focusRect, 600));
                parameters.setFocusAreas(focusAreas);
            }
            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(meteringRect, 600));
                parameters.setMeteringAreas(meteringAreas);
            }
            camera.cancelAutoFocus();
            camera.setParameters(parameters);
            camera.autoFocus(null);
        }
    }


    private static Rect calculateTapArea(float x, float y, float coefficient, int previewWidth, int previewHeight) {
        float focusAreaSize = 200;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) ((x / previewWidth) * 2000 - 1000);
        int centerY = (int) ((y / previewHeight) * 2000 - 1000); //预览页面进行了90度旋转，因此是乘以比例系数
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);
        RectF rectF = new RectF(left, top, right, bottom);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
}
