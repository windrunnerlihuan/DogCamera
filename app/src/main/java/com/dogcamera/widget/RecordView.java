package com.dogcamera.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import com.dogcamera.av.EncoderConfig;
import com.dogcamera.av.Rotation;
import com.dogcamera.av.TextureMovieEncoder;
import com.dogcamera.filter.GPUImageFilter;
import com.dogcamera.utils.CameraUtils;

import java.io.File;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import static com.dogcamera.utils.CameraUtils.PICSIZERATE;
import static com.dogcamera.utils.CameraUtils.PREVIEW_RESOLUTION_HEIGHT;
import static com.dogcamera.utils.CameraUtils.PREVIEW_RESOLUTION_WIDTH;

/**
 * Created by huanli on 2018/2/27.
 */
@SuppressLint("NewApi")
public class RecordView extends BaseGLSurfaceView {
    private static final String TAG = "RecordView";


    private int mImageWidth = PREVIEW_RESOLUTION_WIDTH;
    private int mImageHeight = PREVIEW_RESOLUTION_HEIGHT;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    protected Camera mCamera;

    private TextureMovieEncoder mVideoEncoder;

    private boolean isRecording;
    private int mRecordingStatus;

    private String mEncodeVideoPath;

    private OnRecordingErrorListener mOnRecordingErrorListener;

    private String mFilterId;

    public RecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mVideoEncoder = new TextureMovieEncoder();
        mVideoEncoder.setOnErrorListener((errorType) -> {
            if (mOnRecordingErrorListener != null) {
                mOnRecordingErrorListener.onError(errorType);
            }
        });
    }

    @Override
    public boolean performClick() {
        return true;
    }

    @Override
    protected void onSurfaceInit() {
        openCamera();
    }

    public Camera getCamera() {
        return mCamera;
    }

    public boolean openCamera() {
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int numCameras = Camera.getNumberOfCameras();
            int cameraId = 0;
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                    mCamera = Camera.open(i);
                    break;
                }
            }
            if (mCamera == null) {
                Log.d(TAG, "No back-facing camera found; opening default");
                mCamera = Camera.open();    // opens first back-facing camera
            }
            if (mCamera == null) {
                throw new RuntimeException("Unable to open camera");
            }
            //设置参数
            Camera.Parameters params = mCamera.getParameters();
            int degrees = CameraUtils.getCameraDisplayOrientation((Activity)getContext(), cameraId);
            Log.e(TAG, "摄像头旋转" + degrees + "度");
            //Nexus 5X 机器的摄像头装反了
            if ("Nexus 5X".equals(Build.MODEL)) {
                degrees = 360 - degrees;
            }
            params.setRotation(degrees);
            params.setRecordingHint(true);

            List<Camera.Size> previewSizeList = params.getSupportedPreviewSizes();
            List<Camera.Size> pictureSizeList = params.getSupportedPictureSizes();
            Camera.Size previewSize = CameraUtils.getPropPicSize(previewSizeList, PICSIZERATE, PREVIEW_RESOLUTION_WIDTH);
            Camera.Size pictureSize = CameraUtils.getPropPicSize(pictureSizeList, PICSIZERATE, PREVIEW_RESOLUTION_WIDTH);
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
            mCamera.setParameters(params);

//            if(degress == 90 || degress == 270){
//                mImageWidth = previewHeight;
//                mImageHeight = previewWidth;
//            } else {
//                mImageWidth = previewWidth;
//                mImageHeight = previewHeight;
//            }
            mImageWidth = previewWidth;
            mImageHeight = previewHeight;

            Rotation rotation = Rotation.NORMAL;
            switch (degrees) {
                case 90:
                    rotation = Rotation.ROTATION_90;
                    break;
                case 180:
                    rotation = Rotation.ROTATION_180;
                    break;
                case 270:
                    rotation = Rotation.ROTATION_270;
                    break;
            }
            boolean flipHorizontal = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
            setRotation(rotation, flipHorizontal, false);

            //前面params设置过rotation了，这句不用了
//            mCamera.setDisplayOrientation(degress);
            mCamera.setPreviewTexture(mSurfaceTexture);

//            if (mCallbackBuffer == null) {
//                int preWidth = mCamera.getParameters().getPreviewSize().width;
//                int preHeight = mCamera.getParameters().getPreviewSize().height;
//                mCallbackBuffer = new byte[preWidth * preHeight * 3 / 2];
//            }
//            mCamera.addCallbackBuffer(mCallbackBuffer);
//            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        super.onDrawFrame(gl10);
        videoOnDrawFrame(genTextureID, mSurfaceTexture.getTimestamp());
    }

    private int[] getEncoderImageDimen(){
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            return new int[]{mImageHeight, mImageWidth};
        }
        return new int[]{mImageWidth, mImageHeight};
    }

    private void videoOnDrawFrame(int textureId, long timestamp) {
        if (isRecording) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    mVideoEncoder.setCubeAndTextureBuffer(mGLCubeBuffer, mGLTextureBuffer);
                    mVideoEncoder.startRecording(new EncoderConfig(EGL14.eglGetCurrentContext(),
                            new File(mEncodeVideoPath), getEncoderImageDimen()[0], getEncoderImageDimen()[1], mFilterId));
                    mVideoEncoder.setTextureId(textureId);
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }
        mVideoEncoder.frameAvailable(timestamp);
    }

    public void startRecord() {
        isRecording = true;
    }

    public void stopRecord() {
        isRecording = false;
    }

    public void setEncodeVideoPath(String videoPath) {
        mEncodeVideoPath = videoPath;
    }


    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    protected void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void exitRecord() {
        stopRecord();
        releaseCamera();
    }

    public void changeGpuImageFilter(GPUImageFilter filter, String filterId) {
        super.changeGpuImageFilter(filter);
        mFilterId = filterId;
    }

    @Override
    protected int getVideoWidth() {
        return mImageWidth;
    }

    @Override
    protected int getVideoHeight() {
        return mImageHeight;
    }

    public void setOnRecordingErrorListener(OnRecordingErrorListener onRecordingErrorListener) {
        mOnRecordingErrorListener = onRecordingErrorListener;
    }

    public interface OnRecordingErrorListener {
        void onError(int errorType);
    }
}
