package com.dogcamera.filter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.opengl.GLES20;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import java.nio.FloatBuffer;

public class GPUImageSildeFilterGroup extends GPUImageFilter {

    private static final String TAG = GPUImageSildeFilterGroup.class.getSimpleName();
    private GPUImageFilter mCurFilter;
    private GPUImageFilter mLeftFilter;
    private GPUImageFilter mRightFilter;

    private int[] mFrameBuffers = new int[1];
    private int[] mFrameBufferTextures = new int[1];
    /**
     * 滤镜分割线 值为滑动距离 大于0表示向右滑动 小于0表示向左滑动
     */
    private int mDividerOffset = 0;

    private static final int SCROLL_TIME = 500;

    public GPUImageSildeFilterGroup() {

    }

    public void setFilter(GPUImageFilter curFilter, GPUImageFilter leftFilter, GPUImageFilter rightFilter) {
        if (mCurFilter != null) {
            mCurFilter.destroy();
        }
        mCurFilter = curFilter;
        if (mLeftFilter != null) {
            mLeftFilter.destroy();
        }
        mLeftFilter = leftFilter;
        if (mRightFilter != null) {
            mRightFilter.destroy();
        }
        mRightFilter = rightFilter;
    }

    @Override
    public void onInit() {
        if (mCurFilter != null)
            mCurFilter.init();
        if (mLeftFilter != null)
            mLeftFilter.init();
        if (mRightFilter != null)
            mRightFilter.init();
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        /*
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        */
        /*
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        */
        if (mCurFilter != null)
            mCurFilter.onOutputSizeChanged(width, height);
        if (mLeftFilter != null)
            mLeftFilter.onOutputSizeChanged(width, height);
        if (mRightFilter != null)
            mRightFilter.onOutputSizeChanged(width, height);
    }

    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer, final FloatBuffer textureBuffer) {
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
//                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
//        GLES20.glClearColor(0, 0, 0, 0);
        //TODO draw
        drawFilter(textureId, cubeBuffer, textureBuffer);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void drawFilter(final int textureId, final FloatBuffer cubeBuffer,
                            final FloatBuffer textureBuffer) {
        if (mDividerOffset > 0) {//右滑
            //左边滤镜
            if(mLeftFilter != null){
                GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(0, 0, mDividerOffset, mOutputHeight);
                mLeftFilter.onDraw(textureId, cubeBuffer, textureBuffer);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            }
            //当前滤镜
            if(mCurFilter != null){
                GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(mDividerOffset, 0, mOutputWidth - mDividerOffset, mOutputHeight);
                mCurFilter.onDraw(textureId, cubeBuffer, textureBuffer);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            }

        } else if (mDividerOffset < 0) {//左滑
            //当前滤镜
            if(mCurFilter != null) {
                GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(0, 0, mOutputWidth + mDividerOffset, mOutputHeight);
                mCurFilter.onDraw(textureId, cubeBuffer, textureBuffer);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            }
            //右边滤镜
            if(mRightFilter != null) {
                GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(mOutputWidth + mDividerOffset, 0, -mDividerOffset, mOutputHeight);
                mRightFilter.onDraw(textureId, cubeBuffer, textureBuffer);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            }
        } else {
            if(mCurFilter != null)
                mCurFilter.onDraw(textureId, cubeBuffer, textureBuffer);
        }

    }

    public synchronized void setDividerOffset(int offset){
//        runOnDraw(() -> mDividerOffset = offset);
        mDividerOffset = offset;

    }

    public int getDividerOffset(){
        return mDividerOffset;
    }

    public void flingTo(int end, OnFilterScrollListener onFilterScrollListener){
        Log.e(TAG, "scrollTo start = " + mDividerOffset + " end = " + end);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(mDividerOffset, end).setDuration(SCROLL_TIME);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            int offset = (int) animation.getAnimatedValue();
            Log.e(TAG, "flingTo = " + offset);
            setDividerOffset(offset);
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDividerOffset = 0;
                if(onFilterScrollListener != null){
                    onFilterScrollListener.scrollToEnd();
                }
            }
        });
        valueAnimator.start();
    }

    @Override
    public void onDestroy() {
//        destroyFramebuffers();
        if (mCurFilter != null)
            mCurFilter.destroy();
        if (mLeftFilter != null)
            mLeftFilter.destroy();
        if (mRightFilter != null)
            mRightFilter.destroy();
        super.onDestroy();
    }

    private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    public interface OnFilterScrollListener{
        void scrollToEnd();
    }


}
