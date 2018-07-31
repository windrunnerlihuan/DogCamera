package com.dogcamera.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dogcamera.R;
import com.dogcamera.utils.ViewUtils;

public class LoadingView extends LinearLayout {

    private String mLoadingTxt = "正在加载,请稍后...";

    private ImageView mImageView;
    private TextView mTextView;

    private OnRetryListener mOnRetryListener;

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadingView);
        String s = a.getString(R.styleable.LoadingView_text);
        if(s != null){
            mLoadingTxt = s;
        }
        a.recycle();
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        mImageView = new ImageView(getContext());
        LayoutParams lpImage = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpImage.bottomMargin = ViewUtils.dip2px(getContext(), 5f);
        mImageView.setLayoutParams(lpImage);
        mImageView.setImageDrawable(getResources().getDrawable(R.drawable.loading));
        addView(mImageView);
        mTextView = new TextView(getContext());
        mTextView.setTextColor(getResources().getColor(R.color.light_gray));
        mTextView.setTextSize(12);
        LayoutParams lpText = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mTextView.setLayoutParams(lpText);
        mTextView.setText(mLoadingTxt);
        addView(mTextView);
        setOnClickListener(v -> {
            if(mOnRetryListener != null){
                mOnRetryListener.onRetry();
            }
        });
    }

    public void start(){

        setClickable(false);
        mImageView.setImageDrawable(getResources().getDrawable(R.drawable.loading));
        mTextView.setText(mLoadingTxt);
        if(mImageView.getDrawable() instanceof AnimationDrawable){
            ((AnimationDrawable) mImageView.getDrawable()).start();
        }
    }

    private void stop(){
        if(mImageView.getDrawable() instanceof AnimationDrawable){
            ((AnimationDrawable) mImageView.getDrawable()).stop();
        }
    }

    public void finish(int imgId, String msg, boolean canRetry){
        setClickable(canRetry);
        mImageView.setImageDrawable(getResources().getDrawable(imgId > 0 ? imgId : R.mipmap.loading_finish));
        if(!TextUtils.isEmpty(msg)){
            mTextView.setText(msg);
        }else{
            mTextView.setVisibility(View.GONE);
        }
    }

    public void setOnRetryListener(OnRetryListener onRetryListener){
        mOnRetryListener = onRetryListener;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    public interface OnRetryListener {
        void onRetry();
    }

}
