package com.dogcamera.widget;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.AttributeSet;

import static com.dogcamera.av.Rotation.ROTATION_180;
import static com.dogcamera.av.Rotation.ROTATION_270;
import static com.dogcamera.av.Rotation.ROTATION_90;

public class PlayView extends BaseGLSurfaceView {

    private int mVideoWidth = -1;
    private int mVideoHeight = -1;

    private String mPlayVideoPath;


    public PlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPlayVideoPath(String path) {

        if (TextUtils.isEmpty(path)) {
            return;
        }
        mPlayVideoPath = path;

        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {

            String tmp;

            retr.setDataSource(path);
            String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            if (!TextUtils.isEmpty(tmp)) {
                mVideoWidth = Integer.parseInt(tmp);
            }
            tmp = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (!TextUtils.isEmpty(tmp)) {
                mVideoHeight = Integer.parseInt(tmp);
            }

            if (!TextUtils.isEmpty(rotation)) {
                if (rotation.equals("90")) {
                    mRotation = ROTATION_90;
                } else if (rotation.equals("270")) {
                    mRotation = ROTATION_270;
                } else if (rotation.equals("180")) {
                    mRotation = ROTATION_180;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            retr.release();
        }
    }

    @Override
    protected void onSurfaceInit() {
        adjustSurfaceSize();
    }

    private void adjustSurfaceSize() {
        if (mVideoWidth > 0 || mVideoHeight > 0) {
            return;
        }
        int originWidth = this.getMeasuredWidth();
        int originHeight = this.getMeasuredHeight();

        int outputWidth = mVideoWidth;
        int outputHeight = mVideoHeight;
        if (mRotation == ROTATION_90 || mRotation == ROTATION_270) {
            outputWidth = mVideoHeight;
            outputHeight = mVideoWidth;
        }
        float radio1 = outputWidth * 1f / originWidth;
        float radio2 = outputHeight * 1f / originHeight;
        if (radio1 > radio2) {
            mSurfaceWidth = originWidth;
            mSurfaceHeight = (int) (originWidth * (outputHeight * 1f / outputWidth) + 0.5f);
        } else {
            mSurfaceHeight = originHeight;
            mSurfaceWidth = (int) (originHeight * (outputWidth * 1f / outputHeight) + 0.5f);
        }
    }

    @Override
    protected int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    protected int getVideoHeight() {
        return mVideoHeight;
    }
}
