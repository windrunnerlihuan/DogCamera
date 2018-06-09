package com.dogcamera.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.dogcamera.utils.ViewUtils;

import java.util.LinkedList;
import java.util.Queue;

public class RectProgressView extends View {

    private static final String TAG = RectProgressView.class.getSimpleName();
    private Paint mPaint;

    private int mDrawLen = 0;

    private float mPrevProgress = -1;
    private long mPrevTime;

    private int mWidth;
    private int mHeight;

    private Queue<Runnable> mProgressRuns = new LinkedList<>();

    public RectProgressView(Context context) {
        this(context, null);
    }

    public RectProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#FFFFFF"));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(ViewUtils.dip2px(getContext(), 8));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mWidth <= 0 || mHeight <= 0){
            Log.e(TAG, "width = " + mWidth + " , height = " + mHeight);
            return;
        }
        if (mDrawLen < mWidth) {
            canvas.drawLine(0, 0, mDrawLen, 0, mPaint);
        } else if (mDrawLen < mWidth + mHeight) {
            canvas.drawLine(0, 0, mWidth, 0, mPaint);
            canvas.drawLine(mWidth, 0, mWidth, mDrawLen - mWidth, mPaint);
        } else if (mDrawLen < mWidth * 2 + mHeight) {
            canvas.drawLine(0, 0, mWidth, 0, mPaint);
            canvas.drawLine(mWidth, 0, mWidth, mHeight, mPaint);
            canvas.drawLine(mWidth, mHeight, 2 * mWidth + mHeight - mDrawLen, mHeight, mPaint);
        } else {
            canvas.drawLine(0, 0, mWidth, 0, mPaint);
            canvas.drawLine(mWidth, 0, mWidth, mHeight, mPaint);
            canvas.drawLine(mWidth, mHeight, 0, mHeight, mPaint);
            canvas.drawLine(0, mHeight, 0, 2 * (mWidth + mHeight) - mDrawLen, mPaint);
        }

    }

    public void setProgress(float progress, OnProgressEndListener onProgressEndListener){
        if(mPrevProgress == progress){
            return;
        }
        long curTime = System.currentTimeMillis();
        if(mPrevProgress < 0){
            mPrevTime = curTime;
        }
        if(curTime != mPrevTime){
            ProgressRunnable runnable = new ProgressRunnable(mPrevProgress, progress,
                    curTime - mPrevTime, animation -> {
                Float value = (Float) animation.getAnimatedValue();
                mDrawLen = (int) (value * (mWidth + mHeight) * 2);
                invalidate();

            }, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressRuns.poll();
                    if(onProgressEndListener != null){
                        onProgressEndListener.onProgressEnd();
                    }
                    if (!mProgressRuns.isEmpty()) {
                        mProgressRuns.poll().run();
                    }
                }
            }, onProgressEndListener);
            if(mProgressRuns.isEmpty()){
                runnable.run();
            }
            mProgressRuns.add(runnable);
        }
        mPrevProgress = progress;
        mPrevTime = curTime;
    }

    private class ProgressRunnable implements Runnable {

        float startProgress;
        float endProgress;
        long duration;
        ValueAnimator animator;
        ValueAnimator.AnimatorUpdateListener updateListener;
        AnimatorListenerAdapter listenerAdapter;
        OnProgressEndListener onProgressEndListener;

        public ProgressRunnable(float startProgress, float endProgress, long duration,
                                ValueAnimator.AnimatorUpdateListener updateListener,
                                AnimatorListenerAdapter listenerAdapter,
                                OnProgressEndListener onProgressEndListener){
            this.startProgress = startProgress;
            this.endProgress = endProgress;
            this.duration = duration;
            this.updateListener = updateListener;
            this.listenerAdapter = listenerAdapter;
            this.onProgressEndListener = onProgressEndListener;
        }

        @Override
        public void run() {
            animator = ValueAnimator.ofFloat(startProgress, endProgress);
            animator.setDuration(duration);
            animator.addUpdateListener(updateListener);
            animator.addListener(listenerAdapter);
            animator.start();

        }
    }

    public interface OnProgressEndListener {
        void onProgressEnd();
    }
}
