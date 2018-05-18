package com.dogcamera.widget;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dogcamera.utils.ViewUtils;

public class VideoFilterHintView extends LinearLayout {

    private static final String TAG = "VideoFilterHintView";

    private static final int SHOW_DURATION = 100;
    private static final int FADE_IN_DURATION = 500;
    private static final int FADE_OUT_DURATION = 1500;

    private TextView mIdView;
    private TextView mNameView;

    public VideoFilterHintView(Context context) {
        this(context, null);
    }

    public VideoFilterHintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    private void initViews() {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER);

        mIdView = new TextView(getContext());
        mIdView.setLayerType(LAYER_TYPE_SOFTWARE, null);
        mIdView.setTextColor(Color.WHITE);
        mIdView.setTypeface(null, Typeface.BOLD);
        mIdView.setTextSize(36);
        mIdView.setGravity(Gravity.LEFT);
        mIdView.setShadowLayer(ViewUtils.dip2px(getContext(), 2), ViewUtils.dip2px(getContext(), 1),
                ViewUtils.dip2px(getContext(), 1), Color.parseColor("#80000000"));
        addView(mIdView);

        mNameView = new TextView(getContext());
        mNameView.setLayerType(LAYER_TYPE_SOFTWARE, null);
        mNameView.setTextColor(Color.WHITE);
        mNameView.setTypeface(null, Typeface.BOLD);
        mNameView.setTextSize(17);
        mNameView.setGravity(Gravity.LEFT);
        mNameView.setShadowLayer(ViewUtils.dip2px(getContext(), 2), ViewUtils.dip2px(getContext(), 1),
                ViewUtils.dip2px(getContext(), 1), Color.parseColor("#80000000"));


        addView(mNameView);
    }

    public void setFilterHint(String id, String name) {


        if (mIdView != null) {
            mIdView.setText(id);
        }

        if (mNameView != null) {
            mNameView.setText(name);
        }

        fadeIn();
    }

    private void fadeIn() {

        setVisibility(View.VISIBLE);

        setAlpha(0f);
        animate().setListener(null);
        animate().cancel();
        animate().alpha(1f).setDuration(FADE_IN_DURATION).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {


                postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        fadeOut();
                    }
                }, SHOW_DURATION);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).start();

    }

    void fadeOut() {

        animate().setListener(null);
        animate().cancel();

        animate().alpha(0f).setDuration(FADE_OUT_DURATION).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {


                setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).start();

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
