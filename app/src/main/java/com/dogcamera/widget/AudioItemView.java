package com.dogcamera.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dogcamera.R;
import com.dogcamera.utils.ViewUtils;

public class AudioItemView extends LinearLayout implements View.OnClickListener{

    ImageView mIcon;
    TextView mTv;

    private String mUSStr, mSStr;
    private int mUSImg, mSImg;

    private boolean mSelected;

    public AudioItemView(Context context) {
        this(context, null);
    }

    public AudioItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER);
        mIcon = new ImageView(getContext());
        LayoutParams iconLp = new LayoutParams(ViewUtils.dip2px(getContext(), 55), ViewUtils.dip2px(getContext(), 55));
        mIcon.setLayoutParams(iconLp);
        addView(mIcon);
        mTv = new TextView(getContext());
        mTv.setTextSize(10);
        mTv.setTextColor(mSelected ? Color.WHITE : getResources().getColor(R.color.light_gray));
        LayoutParams tvLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mTv.setLayoutParams(tvLp);
        addView(mTv);
        setOnClickListener(this);
    }

    public void setImg(int usImg, int sImg){
        mUSImg = usImg;
        mSImg = sImg;
        if(mSelected){
            mIcon.setImageResource(mSImg);
        }else{
            mIcon.setImageResource(mUSImg);
        }
    }

    public void setTv(String usStr, String sStr){
        mUSStr = usStr;
        mSStr = sStr;
        if(mSelected){
            mTv.setText(mSStr);
        }else{
            mTv.setText(mUSStr);
        }
    }

    public void setSelected(boolean selected){
        mSelected = selected;
        if(mSelected){
            mIcon.setImageResource(mSImg);
            mTv.setText(mSStr);
            mTv.setTextColor(Color.WHITE);
        }else{
            mIcon.setImageResource(mUSImg);
            mTv.setText(mUSStr);
            mTv.setTextColor(getResources().getColor(R.color.light_gray));
        }
    }

    @Override
    public void onClick(View v) {
        setSelected(!mSelected);
        //TODO

    }
}
