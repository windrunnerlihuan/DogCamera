package com.dogcamera.av;

/**
 * Created by huanli on 2018/2/28.
 */

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

@TargetApi(18)
public class AndroidMuxer {

    private int mExpectedNumTracks = 1;

    private MediaMuxer mMuxer;

    private volatile boolean mStarted;

    private volatile int mNumTracks;
    private volatile int mNumReleases;


    public AndroidMuxer(String outputPath) {
        try {
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**生成视频的角度*/
    public void setOrientation(int orientation){
        mMuxer.setOrientationHint(orientation);
    }

    public void setExpectedNumTracks(int expectedNumTracks) {
        mExpectedNumTracks = expectedNumTracks;
    }

    public int addTrack(MediaFormat trackFormat) {
        if (mStarted) {
            throw new IllegalStateException();
        }

        synchronized (mMuxer) {
            int track = mMuxer.addTrack(trackFormat);

            if (++mNumTracks == mExpectedNumTracks) {
                mMuxer.start();
                mStarted = true;
            }

            return track;
        }
    }

    public boolean isStarted() {
        return mStarted;
    }

    @TargetApi(18)
    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        synchronized (mMuxer) {
            try {
                mMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @TargetApi(18)
    public boolean release() {
        synchronized (mMuxer) {
            if (++mNumReleases == mNumTracks) {
                try {
                    mMuxer.stop();
                    mMuxer.release();
                    return true;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}

