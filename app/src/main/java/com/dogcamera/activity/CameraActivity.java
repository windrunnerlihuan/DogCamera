package com.dogcamera.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dogcamera.R;
import com.dogcamera.av.RecordConstant;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.filter.GPUImageFilter;
import com.dogcamera.filter.GPUImageSildeFilterGroup;
import com.dogcamera.utils.CameraUtils;
import com.dogcamera.utils.FilterProvider;
import com.dogcamera.utils.SystemUIUtils;
import com.dogcamera.utils.VideoUtils;
import com.dogcamera.widget.CircleProgressView;
import com.dogcamera.widget.RecordView;
import com.dogcamera.widget.VideoFilterHintView;
import com.dogcamera.widget.VideoFilterSwitcher;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class CameraActivity extends BaseActivity {

    private static final int UPDATE_PROGRESS_INTERVAL = 50;
    private static final int MSG_PROGRESS_UPDATE = 1;

    @BindView(R.id.record_root_layout)
    ConstraintLayout mRootView;

    @BindView(R.id.record_glview)
    RecordView mRecordView;

    @BindView(R.id.record_seg_line_bg)
    View mSegLineBgView;
    @BindView(R.id.record_seg_line_progress)
    LinearLayout mSegLineProgressView;

    @BindView(R.id.record_button_bg)
    View mRecordBgView;
    @BindView(R.id.record_button_stop_bg)
    View mRecordStopBgView;
    @BindView(R.id.record_button_txt)
    TextView mRecordTv;
    @BindView(R.id.record_button_progress)
    CircleProgressView mRecordProgressView;

    @BindView(R.id.record_time)
    TextView mRecordTimeTv;

    @BindView(R.id.record_filter_switcher)
    VideoFilterSwitcher mVideoFilterSwitcher;
    @BindView(R.id.record_filter_hint)
    VideoFilterHintView mVideoFilterHintView;

    @BindView(R.id.record_button_next)
    TextView mRecordNextTv;

    @BindView(R.id.record_button_delete)
    ImageView mRecordDeleteImg;

    private boolean mIsRecording = false;
    //focus view
    private View mPreFocusAnimationView;
    //单段录制时长
    private int mRecordTimeMs;
    //进度条时长
    private int mProgressTimeMs;
    //剩余录制时长
    private int mRemainRecordTimeMs;
    //录制总时长
    private int RECORD_MAX_TIME;

    private ProgressHandler mHandler;

    private FilterProvider mFilterProvider;

    private List<String> mRecordPathList = new ArrayList<>();

    ProgressDialog mProgressDialog;

    private static class ProgressHandler extends Handler {

        private WeakReference<CameraActivity> mContextRef;

        public ProgressHandler(CameraActivity context) {
            mContextRef = new WeakReference<CameraActivity>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mContextRef.get() == null)
                return;
            CameraActivity activity = mContextRef.get();
            switch (msg.what) {
                case MSG_PROGRESS_UPDATE:
                    // 更新进度
                    activity.updateProgressUI();
                    break;
            }

        }
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_camera;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initViews(Bundle savedInstanceState) {
        initUiVisibility();
        test();
        initValues();
        // 计时器

        // 录制按钮
        RecordGestureListener gestureListener = new RecordGestureListener();
        RecordGestureDetector gestureDetector = new RecordGestureDetector(this, gestureListener);
        mRecordView.setOnTouchListener((view, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return true;
        });
        // 滤镜选择器
        initFilterSwitcher();

    }

    private void initValues() {
        mHandler = new ProgressHandler(this);
        RECORD_MAX_TIME = RecordConstant.RECORD_TIME_MAX;
        mRemainRecordTimeMs = RecordConstant.RECORD_TIME_MAX;
        mFilterProvider = new FilterProvider();
    }

    private void initFilterSwitcher() {

        mVideoFilterSwitcher.setOnFilterChangedListener(((filterDes, lFilterDes, rFilterDes) -> {
            Log.e(TAG, "current=" + filterDes.getFilterId() + " left=" + lFilterDes.getFilterId() + " right=" + rFilterDes.getFilterId());
//            changeFilter(filterDes.getFilterId());
            changeFilter(filterDes.getFilterId(), lFilterDes.getFilterId(), rFilterDes.getFilterId());
            mVideoFilterHintView.setFilterHint(filterDes.getFilterId(), filterDes.getFilterName());

        }));
        mVideoFilterSwitcher.setFilters(mFilterProvider.getFilters());
    }

    private void test() {
        Log.e(TAG, Environment.getExternalStorageDirectory().getPath());
    }

    private void changeFilter(String id, String lId, String rId) {
        GPUImageSildeFilterGroup newFilter = new GPUImageSildeFilterGroup();
        newFilter.setFilter(mFilterProvider.createFilter(this, id),
                mFilterProvider.createFilter(this, lId),
                mFilterProvider.createFilter(this, rId));
        mRecordView.changeGpuImageFilter(newFilter, id);
    }

    @Deprecated
    private void changeFilter(String id) {
        GPUImageFilter newFilter = mFilterProvider.createFilter(this, id);
        mRecordView.changeGpuImageFilter(newFilter);
    }

    private class RecordGestureDetector extends GestureDetector {

        private RecordGestureListener mListener;

        public RecordGestureDetector(Context context, RecordGestureListener listener) {
            super(context, listener);
            mListener = listener;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_UP:
                    Log.e(TAG, "ACTION_UP");
                    mListener.onUp();
                    break;
            }
            return super.onTouchEvent(ev);
        }
    }

    private class RecordGestureListener extends GestureDetector.SimpleOnGestureListener {

        private int mDividerOffset = 0;
        private boolean mIsFlinging = false;

        @Override
        public boolean onDown(MotionEvent e) {
            mIsFlinging = false;
            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            mVideoFilterSwitcher.showDetail(false);
            focusCameraTouch(motionEvent);
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.e(TAG, "onScroll -- distanceX:" + distanceX + " , distanceY:" + distanceY);
            GPUImageFilter curFilter = mRecordView.getCurrentGPUImageFilter();
            if (curFilter == null || !(curFilter instanceof GPUImageSildeFilterGroup)) {
                return false;
            }
            mDividerOffset = (int) (mDividerOffset - distanceX);
            ((GPUImageSildeFilterGroup) curFilter).setDividerOffset(mDividerOffset);
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.e(TAG, "onFling -- velocityX:" + velocityX + " , velocityY:" + velocityY);
            GPUImageFilter curFilter = mRecordView.getCurrentGPUImageFilter();
            if (curFilter == null || !(curFilter instanceof GPUImageSildeFilterGroup)) {
                return false;
            }
            if(velocityX <= -3000f || velocityX >= 3000f){
                mIsFlinging = true;
                int end = 0;
                int pageOffset = 0;
                if(velocityX <= -3000f && mDividerOffset < 0){
                    end = -mRecordView.getWidth();
                    pageOffset = 1;
                }else if(velocityX >= 3000f && mDividerOffset > 0){
                    end = mRecordView.getWidth();
                    pageOffset = -1;
                }
                int finalPageOffset = pageOffset;
                ((GPUImageSildeFilterGroup) curFilter).flingTo(end, () -> {
                    mDividerOffset = 0;
                    mVideoFilterSwitcher.flingPageOffset(finalPageOffset);
                });

            }
            return false;
        }

        public boolean onUp() {
            Log.e(TAG, "onUp called !");
            GPUImageFilter curFilter = mRecordView.getCurrentGPUImageFilter();
            if (curFilter == null || !(curFilter instanceof GPUImageSildeFilterGroup)) {
                return false;
            }
            if (mIsFlinging) {
                return false;
            }
            int width = mRecordView.getWidth();
            int end = 0;
            int pageOffset = 0;
            if(mDividerOffset > 0 && mDividerOffset >= width / 2){
                end = width;
                pageOffset = -1;
            }else if(mDividerOffset < 0 && mDividerOffset <= -width / 2){
                end = -width;
                pageOffset = 1;
            }
            int finalPageOffset = pageOffset;
            ((GPUImageSildeFilterGroup) curFilter).flingTo(end, () -> {
                mDividerOffset = 0;
                mVideoFilterSwitcher.flingPageOffset(finalPageOffset);
            });
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordView.onResume();
    }

    private void focusCameraTouch(MotionEvent event) {
        //相机聚焦
        Camera camera = mRecordView.getCamera();
        int previewWidth = mRecordView.getSurfaceWidth();
        int previewHeight = mRecordView.getSurfaceHeight();
        CameraUtils.focusCameraTouch(camera, previewWidth, previewHeight, event.getRawX(), event.getRawY());
        //动画
        if (mPreFocusAnimationView != null) {
            mPreFocusAnimationView.clearAnimation();
        }
        final ImageView focusImageView = new ImageView(this);
        focusImageView.setLayoutParams(new FrameLayout.LayoutParams(250, 250));
        focusImageView.setX(event.getX() - 125);
        focusImageView.setY(event.getY() - 125);
        focusImageView.setImageDrawable(getResources().getDrawable(R.mipmap.record_focus_icon));
        mPreFocusAnimationView = focusImageView;
        mRootView.addView(focusImageView);

        ScaleAnimation focusAnimation = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f,
                Animation.ABSOLUTE, event.getX(), Animation.ABSOLUTE, event.getY());
        focusAnimation.setDuration(1000);
        focusAnimation.setInterpolator(new OvershootInterpolator());
        focusAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRootView.removeView(focusImageView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        focusImageView.startAnimation(focusAnimation);
    }

    private void initUiVisibility() {
        // 透明状态栏和导航栏
        SystemUIUtils.setTransStatusBar(this);
        mRecordDeleteImg.setVisibility(View.GONE);
        mRecordNextTv.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    private void showExitDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Are you OK ?")
                .setItems(new String[]{"Yes", "No"}, ((dialogInterface, i) -> {
                    if (i == 0) {
                        mRecordView.stopRecord();
                        finish();
                    }
                }))
                .create();
        dialog.show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecordView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecordView != null)
            mRecordView.exitRecord();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    private void updateProgressUI() {
        mProgressTimeMs += UPDATE_PROGRESS_INTERVAL;
        if (mProgressTimeMs % 1000 == 0) {
            int minutes = mProgressTimeMs / 1000 / 60;
            int seconds = mProgressTimeMs / 1000 % 60;
            mRecordTimeTv.setText(String.format(Locale.getDefault(), "%1$02d:%2$02d", minutes, seconds));
        }
        mRecordProgressView.updateProgressView(1.0f * mProgressTimeMs / mRemainRecordTimeMs);
        mHandler.sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, UPDATE_PROGRESS_INTERVAL);
        //没有剩余时间了，完成录制
        if (mProgressTimeMs >= mRemainRecordTimeMs) {
            stopRecord(true);
        }

    }

    private void startRecord() {
        //update UI
        //录制按钮
        mRecordBgView.setVisibility(View.GONE);
        mRecordStopBgView.setVisibility(View.VISIBLE);
        mRecordTv.setVisibility(View.GONE);
        //录制进度
        mRecordTimeTv.setVisibility(View.VISIBLE);
        mHandler.sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, UPDATE_PROGRESS_INTERVAL);
        //next delete
        mRecordDeleteImg.setVisibility(View.GONE);
        mRecordNextTv.setVisibility(View.GONE);
        //开始录制
        doStartRecord();
    }

    private void stopRecord(boolean isFinish) {
        //update UI
        //录制按钮
        mRecordBgView.setVisibility(View.VISIBLE);
        mRecordStopBgView.setVisibility(View.GONE);
        mRecordTv.setVisibility(View.VISIBLE);
        //录制进度
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        mRecordProgressView.updateProgressView(0);
        mRecordTimeTv.setVisibility(View.GONE);
        //seg progress
        int segWidth = (int) (mSegLineProgressView.getWidth() * (1.0f * mProgressTimeMs / RECORD_MAX_TIME));
        int SegLineWidth = segWidth - 3;
        View segLineView = new View(this);
        segLineView.setLayoutParams(new LinearLayout.LayoutParams(SegLineWidth, LinearLayout.LayoutParams.MATCH_PARENT));
        segLineView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        mSegLineProgressView.addView(segLineView);
        View separatorView = new View(this);
        separatorView.setLayoutParams(new LinearLayout.LayoutParams(3, LinearLayout.LayoutParams.MATCH_PARENT));
        separatorView.setBackgroundColor(Color.WHITE);
        mSegLineProgressView.addView(separatorView);
        //idle
        mRecordNextTv.setVisibility(View.VISIBLE);
        mRecordDeleteImg.setVisibility(View.VISIBLE);
        if (!isFinish) {
            mRemainRecordTimeMs -= mProgressTimeMs;
            mRecordTimeTv.setText("00:00");
            mRecordTv.setText("拍下一段");
        } else {
            Log.e(TAG, "拍摄完成");
            mRecordTv.setText("完成");
            mRemainRecordTimeMs = 0;
            mRecordProgressView.setClickable(false);
            Toast.makeText(this, "拍摄完成", Toast.LENGTH_LONG).show();
        }
        mProgressTimeMs = 0;
        //停止录制
        doStopRecord();
        //开始合成并跳转至编辑页面
        if (isFinish) {
            gotoEditPage();
        }
    }

    private void doStartRecord() {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("shortvideo_", ".mp4", new File(RecordConstant.RECORD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tempFile == null) {
            Toast.makeText(this, "创建临时文件失败，录制中断", Toast.LENGTH_LONG).show();
            finish();
        }
        String recordPath = tempFile.getAbsolutePath();
        mRecordView.setEncodeVideoPath(recordPath);
        mRecordView.startRecord();
        //记录录制视频路径
        mRecordPathList.add(recordPath);
    }

    private void doStopRecord() {
        mRecordView.stopRecord();
    }

    @OnClick(R.id.record_button_progress)
    void operateRecord() {
        if (mIsRecording) {
            stopRecord(false);
        } else {
            startRecord();
        }
        mIsRecording = !mIsRecording;
    }

    @OnClick(R.id.record_button_delete)
    void deleteSegVideo() {

    }

    @OnClick(R.id.record_button_next)
    void gotoEditPage() {
        showProgressDialog(true);
        new Thread(){
            @Override
            public void run() {
                super.run();
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("merge_shortvideo", ".mp4", new File(RecordConstant.RECORD_DIR));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (tempFile == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(CameraActivity.this, "创建临时文件失败，录制中断", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;

                }
                boolean isSuccess = VideoUtils.joinVideoForSameCodec(mRecordPathList, tempFile.getAbsolutePath());
                File finalTempFile = tempFile;
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, isSuccess ? "视频拼接成功" : "视频拼接失败", Toast.LENGTH_SHORT).show();
                    showProgressDialog(false);
                    if(isSuccess){
                        doStartEditPage(finalTempFile.getAbsolutePath());
                    }else{
                        finish();
                    }
                });
            }
        }.start();
    }

    private void showProgressDialog(boolean isShow){
        if(isShow && mProgressDialog != null && !mProgressDialog.isShowing()){
            mProgressDialog.show();
        }else if(isShow && mProgressDialog == null ){
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("开始拼接视频...");
            mProgressDialog.show();
        }else if(!isShow && mProgressDialog != null){
            mProgressDialog.dismiss();
        }
    }

    private void doStartEditPage(String mergePath){
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("dog://preview"));
        i.putExtra("uri", mergePath);
        i.putExtra("filterid", mRecordView.getFilterId());
        startActivity(i);
    }


}
