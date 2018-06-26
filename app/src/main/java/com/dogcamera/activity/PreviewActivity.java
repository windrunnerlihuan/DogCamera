package com.dogcamera.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.dogcamera.R;
import com.dogcamera.av.RecordConstant;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.fragment.MusicFragment;
import com.dogcamera.module.PreviewRestartParams;
import com.dogcamera.transcode.MediaTranscoder;
import com.dogcamera.transcode.engine.RenderConfig;
import com.dogcamera.utils.DogConstants;
import com.dogcamera.utils.VideoUtils;
import com.dogcamera.widget.PlayView;
import com.dogcamera.widget.RectProgressView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
    private String mFilterId;

    private ProgressHandler mHandler;

    public float testprogress = 0;

    private List<PreviewRestartParams.PreviewRestartListener> mRestartListener;

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
            switch (msg.what) {
                case MSG_PROGRESS_UPDATE:


                    if (testprogress >= 1) {
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
        mVideoView.setOnPlayStatusListener(new PlayView.OnPlayStatusListenerAdapter(){
            @Override
            public void onCompletion() {
                restartPlay(new PreviewRestartParams.Builder().setIsNotify(true).build());
            }
        });
    }

    private void initValues() {
        mHandler = new ProgressHandler(this);
        mRestartListener = new ArrayList<>();
        mPlayUri = getIntent().getStringExtra("uri");
        mFilterId = getIntent().getStringExtra("filterid");

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

    private void updateProgressUI(float progress, RectProgressView.OnProgressEndListener onProgressEndListener) {
        mRectProgressView.setProgress(progress, onProgressEndListener);
    }


    private void showTopSelectFragment(String tag) {
        if (SYMBOLS[0].equals(tag)) {
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
        if (target != null && target.isAdded() && !target.isHidden()) {
            return;
        }
        FragmentTransaction ft = fm.beginTransaction();
        for (String t : SYMBOLS) {
            Fragment f = fm.findFragmentByTag(t);
            if (!t.equals(tag) && f != null && f.isAdded() && !f.isHidden()) {
                ft.hide(f);
            }
        }
        if (target == null) {
            String t = null;
            if (SYMBOLS[1].equals(tag)) {
                t = SYMBOLS[1];
                target = new MusicFragment();
            } else if (SYMBOLS[2].equals(tag)) {
                t = SYMBOLS[2];
                //TODO 贴纸

            } else if (SYMBOLS[3].equals(tag)) {
                t = SYMBOLS[3];
                //TODO 滤镜
            }
            if (target != null) {
                ft.add(R.id.preview_bottom_container, target, t);
                if (target instanceof PreviewRestartParams.PreviewRestartListener) {
                    mRestartListener.add((PreviewRestartParams.PreviewRestartListener) target);
                }
            }
        }
        if (target != null && target.isHidden()) {
            ft.show(target);
        }
        ft.commitAllowingStateLoss();
    }

    public void restartPlay(PreviewRestartParams p) {
        if(p != null){
            if(p.isMute != null){
                mVideoView.setMute(p.isMute);
            }
            if(p.isNotify != null && p.isNotify){
                for (PreviewRestartParams.PreviewRestartListener l : mRestartListener) {
                    l.onPreviewRestart();
                }
            }
        }
        mVideoView.restartPlay();
    }

    private void finishPreview(){
        mVideoView.stopPlay();
        SimpleArrayMap<Integer, Object> retPropSet = new SimpleArrayMap<>();
        for (PreviewRestartParams.PreviewRestartListener l : mRestartListener) {
            l.onPreviewStop();
            if(l.onPreviewGetPropSet() != null){
                retPropSet.putAll(l.onPreviewGetPropSet());
            }
        }
        new Thread(){
            @Override
            public void run() {
                String outpath = RecordConstant.RECORD_DIR + File.separator + "transcode" + System.currentTimeMillis() + ".mp4";

                VideoUtils.transcodeAddFilter(mPlayUri, outpath,
                        new RenderConfig.Builder()
                                .setFilterId(mFilterId)
                                .setOutputWidth(mVideoView.getImageWidth())
                                .setOutputHeight(mVideoView.getImageHeight())
                                .setAudioPath((String) retPropSet.get(DogConstants.PREVIEW_KEY_MUSIC))
                                .build(),
                        new MediaTranscoder.Listener() {
                    @Override
                    public void onTranscodeProgress(double progress) {

                    }

                    @Override
                    public void onTranscodeCompleted() {
                        Log.e(TAG, "onTranscodeCompleted");
                    }

                    @Override
                    public void onTranscodeCanceled() {

                    }

                    @Override
                    public void onTranscodeFailed(Exception exception) {
                        Log.e(TAG, "onTranscodeCompleted");
                    }
                });
            }
        }.start();
    }

    @OnClick(R.id.preview_root_layout)
    void onRootClick() {
        showTopSelectFragment(SYMBOLS[0]);
    }

    @OnClick(R.id.preview_topbar_music)
    void onMusicClick() {
        showTopSelectFragment(SYMBOLS[1]);
    }

    @OnClick(R.id.preview_topbar_chart)
    void onChartClick() {
//        showTopSelectFragment(SYMBOLS[2]);
        Toast.makeText(this, "贴纸施工中-.-", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.preview_topbar_effect)
    void onEffectClick() {
//        showTopSelectFragment(SYMBOLS[3]);
        Toast.makeText(this, "特效施工中-.-", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.preview_finish)
    void onFinishClick() {
        finishPreview();
    }

    @OnClick(R.id.preview_back)
    void onBackClick() {

    }

}
