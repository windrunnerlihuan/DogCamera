package com.dogcamera.widget;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;

import java.io.IOException;

import static com.dogcamera.av.Rotation.ROTATION_180;
import static com.dogcamera.av.Rotation.ROTATION_270;
import static com.dogcamera.av.Rotation.ROTATION_90;

public class PlayView extends BaseGLSurfaceView {

    private static final String TAG = PlayView.class.getSimpleName();
    private int mVideoWidth = -1;
    private int mVideoHeight = -1;

    private String mPlayVideoPath;

    private boolean mLooping = false;

    private boolean mMute = false;

    private MediaPlayer mMediaPlayer;

    private OnPlayerStatusListener mListener;

    public PlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPlayVideoPath(String path) {

        if (TextUtils.isEmpty(path)) {
            return;
        }
        mPlayVideoPath = path;

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {

            String tmp;

            mmr.setDataSource(path);
            String rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            tmp = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            if (!TextUtils.isEmpty(tmp)) {
                mVideoWidth = Integer.parseInt(tmp);
            }
            tmp = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (!TextUtils.isEmpty(tmp)) {
                mVideoHeight = Integer.parseInt(tmp);
            }

            if (!TextUtils.isEmpty(rotation)) {
                if (rotation.equals("90")) {
                    mRotation = ROTATION_90;
                } else if (rotation.equals("270")) {
                    mRotation = ROTATION_270;
                } else if (rotation.equals("180")) {
                    mRotation = ROTATION_180;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            mmr.release();
        }
    }

    @Override
    protected void onSurfaceInit() {
        adjustSurfaceSize();
        startPlay();
    }

    private void adjustSurfaceSize() {
        if (mVideoWidth < 0 || mVideoHeight < 0) {
            return;
        }
        int originWidth = this.getMeasuredWidth();
        int originHeight = this.getMeasuredHeight();

        int outputWidth = mVideoWidth;
        int outputHeight = mVideoHeight;
        if (mRotation == ROTATION_90 || mRotation == ROTATION_270) {
            outputWidth = mVideoHeight;
            outputHeight = mVideoWidth;
        }
        float radio1 = outputWidth * 1f / originWidth;
        float radio2 = outputHeight * 1f / originHeight;
        if (radio1 > radio2) {
            mSurfaceWidth = originWidth;
            mSurfaceHeight = (int) (originWidth * (outputHeight * 1f / outputWidth) + 0.5f);
        } else {
            mSurfaceHeight = originHeight;
            mSurfaceWidth = (int) (originHeight * (outputWidth * 1f / outputHeight) + 0.5f);
        }

        post(this::requestLayout);

    }

    public void setLooping(boolean looping){
        mLooping = looping;
    }

    public void setMute(boolean mute) {
        if(mMute == mute)
            return;
        mMute = mute;
        if (mMediaPlayer != null) {
            if (mute) {
                mMediaPlayer.setVolume(0f, 0f);
            } else {
                mMediaPlayer.setVolume(1f, 1f);
            }
        }
    }

    public void startPlay(){
        if (TextUtils.isEmpty(mPlayVideoPath)) {
            return;
        }
        if(mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            if (mMute) {
                mMediaPlayer.setVolume(0f, 0f);
            }
            mMediaPlayer.setOnCompletionListener(mp -> {
                if(mLooping){
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();
                }
                if(mListener != null){
                    mListener.onCompletion();
                }

            });
            mMediaPlayer.setOnPreparedListener(mp -> mMediaPlayer.start());
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> false);

            Surface surface = new Surface(mSurfaceTexture);
            mMediaPlayer.setSurface(surface);
            surface.release();
            try {
                mMediaPlayer.setDataSource(mPlayVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaPlayer.prepareAsync();
        }else{
            mMediaPlayer.start();
        }

    }

    public void stopPlay(){
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }
    }

    public void restartPlay(){
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            mMediaPlayer.seekTo(0);
        }else{
            stopPlay();
            startPlay();
        }
    }

    public void setOnPlayStatusListener(OnPlayerStatusListener listener){
        mListener = listener;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPlay();
    }

    @Override
    protected int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    protected int getVideoHeight() {
        return mVideoHeight;
    }

    public interface OnPlayerStatusListener {
        void onCompletion();
        //需要哪个可以继续加

    }

    public static class OnPlayStatusListenerAdapter implements OnPlayerStatusListener {
        @Override
        public void onCompletion() {

        }
    }

}
