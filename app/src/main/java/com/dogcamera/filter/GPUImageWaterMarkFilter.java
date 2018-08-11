package com.dogcamera.filter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import com.dogcamera.DogApplication;
import com.dogcamera.R;
import com.dogcamera.utils.BitmapUtils;
import com.dogcamera.utils.OpenGlUtils;
import com.dogcamera.utils.ViewUtils;

import java.nio.FloatBuffer;

public class GPUImageWaterMarkFilter extends GPUImageFilter {

    private Bitmap mBitmap;

    private int mWaterMarkWidth;
    private int mWaterMarkHeight;
    private int mWaterMarkMargin;
    private int mWaterMarkX;
    private int mWaterMarkY;

    public int mFilterSourceTexture = OpenGlUtils.NO_TEXTURE;

    public enum Positon {
        LT, LB, MM, RT, RB
    }

    public void setDefaultWaterMark() {
        int height = ViewUtils.dip2px(DogApplication.getInstance(), 15);
        int margin = ViewUtils.dip2px(DogApplication.getInstance(), 10);
        Bitmap bm = BitmapFactory.decodeResource(DogApplication.getInstance().getResources(), R.mipmap.watermark);
        Bitmap scaleBitmap = BitmapUtils.scaleBitmap(bm, height * 1f / bm.getHeight());
        setWaterMark(scaleBitmap, margin, Positon.RT);
    }

    public void setWaterMark(Bitmap bm, int margin, Positon positon){
        setBitmap(bm);
        runOnDraw(() -> {
            mWaterMarkHeight = bm.getHeight();
            mWaterMarkWidth = bm.getWidth();
            mWaterMarkMargin = margin;
            switch (positon){
                case LT:
                    mWaterMarkX = mWaterMarkMargin;
                    mWaterMarkY = mOutputHeight - mWaterMarkMargin - mWaterMarkHeight;
                    break;
                case LB:
                    mWaterMarkX = mWaterMarkMargin;
                    mWaterMarkY = mWaterMarkMargin;
                    break;
                case MM:
                    mWaterMarkX = (mOutputWidth - mWaterMarkWidth) / 2;
                    mWaterMarkY = (mOutputHeight - mWaterMarkHeight) / 2;
                    break;
                case RT:
                    mWaterMarkX = mOutputWidth - mWaterMarkMargin - mWaterMarkWidth;
                    mWaterMarkY = mOutputHeight - mWaterMarkMargin - mWaterMarkHeight;
                    break;
                case RB:
                    mWaterMarkY = mWaterMarkMargin;
                    break;
            }
        });
    }



    private void setBitmap(final Bitmap bitmap) {
        if (bitmap != null && bitmap.isRecycled()) {
            return;
        }
        mBitmap = bitmap;
        if (mBitmap == null) {
            return;
        }
        runOnDraw(() -> {
            if (mFilterSourceTexture == OpenGlUtils.NO_TEXTURE) {
                if (bitmap == null || bitmap.isRecycled()) {
                    return;
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                mFilterSourceTexture = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false);
            }
        });
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void recycleBitmap() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, new int[]{
                mFilterSourceTexture
        }, 0);
        mFilterSourceTexture = OpenGlUtils.NO_TEXTURE;
    }


    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);
        GLES20.glViewport(mWaterMarkX,
                mWaterMarkY,
                mWaterMarkWidth, mWaterMarkHeight);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        super.onDraw(mFilterSourceTexture, cubeBuffer, textureBuffer);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
    }
}
