package com.dogcamera.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.dogcamera.widget.LoadingView;
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
    private static final int MSG_PROGRESS_FINISH = 2;
    private static final int MSG_PROGRESS_FAILED = 3;

    @BindView(R.id.preview_root_layout)
    ConstraintLayout mRootLayout;

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

    @BindView(R.id.preview_finish)
    ImageView mFinishImg;
    @BindView(R.id.preview_back)
    ImageView mBackImg;
    @BindView(R.id.preview_compose_loading)
    LoadingView mLoadingView;

    private String mPlayUri;
    private String mFilterId;

    private ProgressHandler mHandler;

    private List<PreviewRestartParams.PreviewRestartListener> mRestartListener;

    private boolean mIsComposing = false;

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
                    float progress = (float) msg.obj;
                    activity.updateProgressUI(progress, null);
                    break;
                case MSG_PROGRESS_FINISH:
                    removeCallbacksAndMessages(null);
                    activity.updateProgressUI(1f, () -> mLoadingView.finish("合成完成，棒棒哒(๑•̀ㅂ•́)و✧", false));
                    String outPath = (String) msg.obj;
                    doComposeSuccess(outPath);
                    break;
                case MSG_PROGRESS_FAILED:
                    removeCallbacksAndMessages(null);
                    mLoadingView.finish("合成失败，点我重试(￣_,￣ )", true);
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
        mLoadingView.setOnRetryListener(() -> doComposeRetry());
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
        mIsComposing = true;
        //function
        mVideoView.stopPlay();
        SimpleArrayMap<Integer, Object> retPropSet = new SimpleArrayMap<>();
        for (PreviewRestartParams.PreviewRestartListener l : mRestartListener) {
            l.onPreviewStop();
            if(l.onPreviewGetPropSet() != null){
                retPropSet.putAll(l.onPreviewGetPropSet());
            }
        }
        //UI
        mRootLayout.setClickable(false);
        mMusicIcon.setVisibility(View.INVISIBLE);
        mChartIcon.setVisibility(View.INVISIBLE);
        mEffectIcon.setVisibility(View.INVISIBLE);
        mFinishImg.setVisibility(View.INVISIBLE);
        mBackImg.setVisibility(View.INVISIBLE);
        mRectProgressView.setVisibility(View.VISIBLE);
        mLoadingView.setVisibility(View.VISIBLE);

        doCompose(retPropSet);

    }

    private void doCompose(SimpleArrayMap<Integer, Object> retPropSet){

        new Thread(){
            @Override
            public void run() {
                String outpath = RecordConstant.RECORD_DIR + File.separator + "transcode" + System.currentTimeMillis() + ".mp4";

                VideoUtils.transcodeVideo(mPlayUri, outpath,
                        new RenderConfig.Builder()
                                .setFilterId(mFilterId)
                                .setAudioPath((String) retPropSet.get(DogConstants.PREVIEW_KEY_MUSIC))
                                .setOriginMute(
                                        retPropSet.get(DogConstants.PREVIEW_KEY_ORIGIN_MUTE) != null ?
                                                (Boolean) retPropSet.get(DogConstants.PREVIEW_KEY_ORIGIN_MUTE) :
                                                false
                                )
                                .build(),
                        new MediaTranscoder.Listener() {
                            @Override
                            public void onTranscodeProgress(double progress) {
                                Log.e(TAG, "合成进度： " + (float)progress);
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_UPDATE, (float)progress));
                            }

                            @Override
                            public void onTranscodeCompleted() {
                                Log.e(TAG, "onTranscodeCompleted");
                                mHandler.sendEmptyMessage(MSG_PROGRESS_FINISH);
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_FINISH, outpath));
                            }

                            @Override
                            public void onTranscodeCanceled() {
                                // no exiting currently
                                //TODO cancel compose
                            }

                            @Override
                            public void onTranscodeFailed(Exception exception) {
                                Log.e(TAG, "onTranscodeFailed");
                                mHandler.sendEmptyMessage(MSG_PROGRESS_FAILED);
                            }
                        });
            }
        }.start();
    }

    private void doComposeSuccess(String outPath){
        //TODO doComposeSuccess

    }

    private void doComposeRetry(){
        Toast.makeText(this, "施工中=.=", Toast.LENGTH_SHORT).show();
        mLoadingView.start();
        mHandler.postDelayed(() -> mLoadingView.finish("合成失败，点我重试(￣_,￣ )", true), 2000);
        //TODO doComposeFailed retry or finish

    }

    @Override
    public void onBackPressed() {
        if(mIsComposing){
            Toast.makeText(this, "正在合成，请勿退出(ノ=Д=)ノ┻━┻", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
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
        onBackPressed();
    }

}
