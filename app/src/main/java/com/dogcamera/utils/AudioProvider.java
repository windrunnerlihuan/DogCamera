package com.dogcamera.utils;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.text.TextUtils;

import com.dogcamera.DogApplication;

import java.io.IOException;

public class AudioProvider {

    private MediaPlayer mMediaPlayer;
    private String mCurrentPath;

    public void startPlay(String path) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }
        stopPlay();
        if(!TextUtils.isEmpty(path)){
            try {
                AssetFileDescriptor afd = DogApplication.getInstance().getAssets().openFd(path);
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                mCurrentPath = path;
            }catch (IOException e){
                e.printStackTrace();
            }
        }


    }

    public void stopPlay() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }
        mCurrentPath = null;
    }

    public void restartPlay(){
        if(mMediaPlayer != null && mMediaPlayer.isPlaying() && mCurrentPath != null){
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
        }
    }

    public void exitPlay(){
        stopPlay();
        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}
