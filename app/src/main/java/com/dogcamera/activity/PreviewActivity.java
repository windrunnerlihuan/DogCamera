package com.dogcamera.activity;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.dogcamera.R;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.utils.ViewUtils;
import com.dogcamera.widget.PlayView;
import com.dogcamera.widget.RectProgressView;

import butterknife.BindView;

public class PreviewActivity extends BaseActivity {

    @BindView(R.id.preview_videoview)
    PlayView mVideoView;

    @BindView(R.id.preview_rect_progress)
    RectProgressView mRectProgressView;

    @BindView(R.id.preview_topbar_music)
    ImageView mMusicIcon;
    @BindView(R.id.preview_topbar_chart)
    ImageView mChartIcon;
    @BindView(R.id.preview_topbar_effect)
    ImageView mEffectIcon;

    @BindView(R.id.preview_bottom_container)
    FrameLayout mBottomContainer;

    private String mPlayUri;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_preview;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        initValues();

        mVideoView.setLooping(true);
        mVideoView.setPlayVideoPath(mPlayUri);
    }

    private void initValues() {
        mPlayUri = getIntent().getStringExtra("uri");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.onPause();
    }
}
