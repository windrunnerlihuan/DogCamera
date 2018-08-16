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

/**
 * 水印只能用一次，好多图片还显示不出来。。。。。。
 * 我自己写的bug，好气呀，暂时还不知道怎么改，等我再研究研究=。=
 * 这个类暂时废弃
 */
@Deprecated
public class GPUImageWaterMarkFilter extends GPUImageFilter {

    private Bitmap mBitmap;

    private int mWaterMarkWidth;
    private int mWaterMarkHeight;
    private int mWaterMarkMargin;
    private int mWaterMarkX;
    private int mWaterMarkY;

    public int mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;

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

    public void setWaterMark(Bitmap bm, int margin, final Positon positon){
        setBitmap(bm);
        mWaterMarkHeight = bm.getHeight();
        mWaterMarkWidth = bm.getWidth();
        mWaterMarkMargin = margin;
        runOnDraw(() -> {
            switch (positon){
                case LT:
                    mWaterMarkX = mWaterMarkMargin;
                    mWaterMarkY = mOutputHeight - mWaterMarkMargin - mWaterMarkHeight ;
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
                    mWaterMarkX = mOutputWidth - mWaterMarkMargin - mWaterMarkWidth;
                    mWaterMarkY = mWaterMarkMargin;
                    break;
            }
            mWaterMarkX = mWaterMarkX < 0 ? 0 : (mWaterMarkX > mOutputWidth ? mOutputWidth : mWaterMarkX);
            mWaterMarkY = mWaterMarkY < 0 ? 0 : (mWaterMarkY > mOutputHeight ? mOutputHeight : mWaterMarkY);
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
            if (mFilterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                if (bitmap == null || bitmap.isRecycled()) {
                    return;
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                mFilterSourceTexture2 = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false);
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
                mFilterSourceTexture2
        }, 0);
        mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    }


    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);
        drawWaterMark(mFilterSourceTexture2, cubeBuffer, textureBuffer);
    }

    private void drawWaterMark(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer){
        GLES20.glViewport(mWaterMarkX,
                mWaterMarkY,
                mWaterMarkWidth, mWaterMarkHeight);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        onDrawWaterMark(textureId, cubeBuffer, textureBuffer);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
    }

    private void onDrawWaterMark(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer){
        GLES20.glUseProgram(mGLProgId);

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
