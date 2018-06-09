package com.dogcamera.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.dogcamera.R;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.utils.ViewUtils;
import com.dogcamera.widget.PlayView;
import com.dogcamera.widget.RectProgressView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.OnClick;

public class PreviewActivity extends BaseActivity {

    private static final int MSG_PROGRESS_UPDATE = 1;

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

    private ProgressHandler mHandler;

    public float testprogress = 0;

    private class ProgressHandler extends Handler {

        private WeakReference<PreviewActivity> mContextRef;

        public ProgressHandler(PreviewActivity context) {
            mContextRef = new WeakReference<>(context);
        }
        @Override
        public void handleMessage(Message msg) {
            if (mContextRef.get() == null)
                return;
            PreviewActivity activity = mContextRef.get();
            switch (msg.what){
                case MSG_PROGRESS_UPDATE:


                    if(testprogress >= 1){
                        activity.updateProgressUI(1f, new RectProgressView.OnProgressEndListener() {
                            @Override
                            public void onProgressEnd() {
                                Toast.makeText(PreviewActivity.this, "合成完成", Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    }
                    activity.updateProgressUI(testprogress, null);
                    testprogress += 0.1;
                    sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, 500);

                    break;
            }


        }
    }

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
        mHandler = new ProgressHandler(this);
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

    private void updateProgressUI(float progress, RectProgressView.OnProgressEndListener onProgressEndListener){
        mRectProgressView.setProgress(progress, onProgressEndListener);
    }

    @OnClick(R.id.preview_test)
    public void test(){
        if(mRectProgressView.getVisibility() == View.GONE){
            mRectProgressView.setVisibility(View.VISIBLE);
        }else{
            mRectProgressView.setVisibility(View.GONE);
        }
        if(testprogress == 0){
            mHandler.sendEmptyMessage(MSG_PROGRESS_UPDATE);
        }
    }
    @OnClick(R.id.preview_test2)
    public void test2(){
        if(mMusicIcon.getVisibility() == View.VISIBLE){
            mMusicIcon.setVisibility(View.GONE);
            mChartIcon.setVisibility(View.GONE);
            mEffectIcon.setVisibility(View.GONE);
        }else{
            mMusicIcon.setVisibility(View.VISIBLE);
            mChartIcon.setVisibility(View.VISIBLE);
            mEffectIcon.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.preview_finish)
    public void finishPreview(){
        mBottomContainer.setVisibility(View.VISIBLE);

        mMusicIcon.setVisibility(View.GONE);
        mChartIcon.setVisibility(View.GONE);
        mEffectIcon.setVisibility(View.GONE);
    }

    @OnClick(R.id.preview_back)
    public void backToRecord(){
        mBottomContainer.setVisibility(View.GONE);

        mMusicIcon.setVisibility(View.VISIBLE);
        mChartIcon.setVisibility(View.VISIBLE);
        mEffectIcon.setVisibility(View.VISIBLE);
    }

}
