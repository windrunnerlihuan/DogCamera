package com.dogcamera.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.dogcamera.R;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.fragment.MusicFragment;
import com.dogcamera.widget.PlayView;
import com.dogcamera.widget.RectProgressView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.OnClick;

public class PreviewActivity extends BaseActivity {

    private String[] SYMBOLS = new String[]{"NONE", "MUSIC", "CHART", "EFFECT"};

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


    private void showTopSelectFragment(String tag){
        if(SYMBOLS[0].equals(tag)){
            mMusicIcon.setVisibility(View.VISIBLE);
            mChartIcon.setVisibility(View.VISIBLE);
            mEffectIcon.setVisibility(View.VISIBLE);
            mBottomContainer.setVisibility(View.GONE);
            return;
        }
        mMusicIcon.setVisibility(View.GONE);
        mChartIcon.setVisibility(View.GONE);
        mEffectIcon.setVisibility(View.GONE);
        mBottomContainer.setVisibility(View.VISIBLE);
        FragmentManager fm = getSupportFragmentManager();
        Fragment target = fm.findFragmentByTag(tag);
        if(target != null && target.isAdded() && !target.isHidden()){
            return;
        }
        FragmentTransaction ft = fm.beginTransaction();
        for(String t : SYMBOLS){
            Fragment f = fm.findFragmentByTag(t);
            if(!t.equals(tag) && f != null && f.isAdded() && !f.isHidden()){
                ft.hide(f);
            }
        }
        if(target == null){
            String t = null;
            if(SYMBOLS[1].equals(tag)){
                t = SYMBOLS[1];
                target = new MusicFragment();
            }else if(SYMBOLS[2].equals(tag)){
                t = SYMBOLS[2];
                //TODO

            }else if(SYMBOLS[3].equals(tag)){
                t = SYMBOLS[3];
                //TODO
            }
            if(target != null){
                ft.add(R.id.preview_bottom_container, target, t);
            }
        }
        if(target != null && target.isHidden()){
            ft.show(target);
        }
        ft.commitAllowingStateLoss();
    }

    @OnClick(R.id.preview_root_layout)
    void onRootClick(){
        showTopSelectFragment(SYMBOLS[0]);
    }

    @OnClick(R.id.preview_topbar_music)
    void onMusicClick(){
        showTopSelectFragment(SYMBOLS[1]);
    }
    @OnClick(R.id.preview_topbar_chart)
    void onChartClick(){
        showTopSelectFragment(SYMBOLS[2]);
    }
    @OnClick(R.id.preview_topbar_effect)
    void onEffectClick(){
        showTopSelectFragment(SYMBOLS[3]);
    }

    @OnClick(R.id.preview_finish)
    void onFinishClick(){

    }

    @OnClick(R.id.preview_back)
    void onBackClick(){

    }

}
