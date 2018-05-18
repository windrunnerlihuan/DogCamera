package com.dogcamera.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.dogcamera.R;

/**
 * Created by huanli on 2018/3/1.
 */

public class CircleProgressView extends View {

    private Paint mPaint;
    private RectF mRectF;

    private float mProgressRate;
    private int mArcBgColor;
    private float mCircleLineStroke;
    private int mArcColor;

    private int mMaxProgress = 1; // 最大进度

    private OnProgressListener mOnProgressListener;

    public CircleProgressView(Context context) {
        this(context, null);
    }

    public CircleProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mRectF = new RectF();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView);
        mArcColor = typedArray.getColor(R.styleable.CircleProgressView_arc_color, 0xffff6633);
        mProgressRate = typedArray.getFloat(R.styleable.CircleProgressView_progress_rate, 0);
        mCircleLineStroke = typedArray.getDimension(R.styleable.CircleProgressView_circle_stroke, 8);
        mArcBgColor = typedArray.getColor(R.styleable.CircleProgressView_arc_bg_color, 0x00000000);
        typedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mArcBgColor);
        canvas.drawColor(Color.TRANSPARENT);
        mPaint.setStrokeWidth(mCircleLineStroke);
        mPaint.setStyle(Paint.Style.STROKE);
        mRectF.left = mCircleLineStroke / 2;
        mRectF.top = mCircleLineStroke / 2;
        mRectF.right = width - mRectF.left;
        mRectF.bottom = height - mRectF.top;
        //圆弧背景
        canvas.drawArc(mRectF, -90, 360, false, mPaint);
        //进度
        mPaint.setColor(mArcColor);
        canvas.drawArc(mRectF, -90, 360 * 1.0f * mProgressRate, false, mPaint);
        //当进度到达最大值时  调用此函数
        if (mOnProgressListener != null) {
            if (mProgressRate == mMaxProgress) {
                mOnProgressListener.onEnd();
            }
        }
    }

    public void setArcBackColor(int color) {
        mArcBgColor = color;
    }

    public void setArcColor(int arcColor) {
        this.mArcColor = arcColor;
    }

    public void setProgress(float progress) {
        this.mProgressRate = progress;
        this.postInvalidate();
    }

    public void updateProgressView(float progressRate) {
        this.mProgressRate = progressRate;
        this.postInvalidate();
    }

    public void updateArcBgColor(int arcBgColor) {
        this.mArcBgColor = arcBgColor;
        this.postInvalidate();
    }

    public void updateArcColor(int arcColor) {
        this.mArcColor = arcColor;
        this.postInvalidate();
    }

    public void setProgressWithAnimation(float progress) {
        setProgressWithAnimation(progress, 800);
    }

    public void setProgressWithAnimation(float progress, int duration) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "progress", mProgressRate, progress);
        objectAnimator.setDuration(duration);
        objectAnimator.setInterpolator(new DecelerateInterpolator());
        objectAnimator.start();
        this.mProgressRate = progress;
    }

    public void setCircleLineStroke(float circleLineStroke) {
        this.mCircleLineStroke = circleLineStroke;
    }

    public void setOnProgressListener(OnProgressListener mOnProgressListener) {
        this.mOnProgressListener = mOnProgressListener;
    }

    /**
     * 回调接口
     */
    public interface OnProgressListener {
        /**
         * 回调函数 当进度条满时调用此方法
         */
        public void onEnd();

    }
}
