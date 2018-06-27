package com.dogcamera.transcode.engine;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;

import com.dogcamera.DogApplication;
import com.dogcamera.transcode.compat.MediaCodecBufferCompatWrapper;
import com.dogcamera.utils.VideoUtils;

import java.io.FileDescriptor;
import java.io.IOException;

public class AudioTrackTranscoderAdvance implements TrackTranscoder {

    private static final QueuedMuxer.SampleType SAMPLE_TYPE = QueuedMuxer.SampleType.AUDIO;

    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaExtractor mExtractor;
    private final QueuedMuxer mMuxer;
    private long mWrittenPresentationTimeUs;

    private final int mTrackIndex;
    private final MediaFormat mInputFormat;
    private final MediaFormat mOutputFormat;

    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private MediaFormat mActualOutputFormat;

    private MediaCodecBufferCompatWrapper mDecoderBuffers;
    private MediaCodecBufferCompatWrapper mEncoderBuffers;

    private boolean mIsExtractorEOS;
    private boolean mIsDecoderEOS;
    private boolean mIsEncoderEOS;
    private boolean mDecoderStarted;
    private boolean mEncoderStarted;

    private AudioChannelAdvance mAudioChannelAdvance;

    //混音配置相关变量如下
    private RenderConfig mRenderConfig;
    private int mSurgarTrackIndex;
    private MediaExtractor mSugarExtractor;
    private MediaCodec mSugarDecoder;
    private MediaCodecBufferCompatWrapper mSugarDecoderBuffers;
    private boolean mSugarDecoderStarted;
    private boolean mIsSurgarExtractorEOS;
    private boolean mIsSurgarDecoderEOS;
    private final MediaCodec.BufferInfo mSugarBufferInfo = new MediaCodec.BufferInfo();

    public AudioTrackTranscoderAdvance(MediaExtractor extractor, int trackIndex,
                                       MediaFormat outputFormat, QueuedMuxer muxer) {
        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mOutputFormat = outputFormat;
        mMuxer = muxer;

        mInputFormat = mExtractor.getTrackFormat(mTrackIndex);
    }

    /**
     *  设置混音等等
     */
    public void setRenderConfig(RenderConfig config){
        mRenderConfig = config;
    }

    @Override
    public void setup() {
        mExtractor.selectTrack(mTrackIndex);
        try {
            mEncoder = MediaCodec.createEncoderByType(mOutputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        mEncoder.configure(mOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
        mEncoderStarted = true;
        mEncoderBuffers = new MediaCodecBufferCompatWrapper(mEncoder);

        final MediaFormat inputFormat = mExtractor.getTrackFormat(mTrackIndex);
        try {
            mDecoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        mDecoder.configure(inputFormat, null, null, 0);
        mDecoder.start();
        mDecoderStarted = true;
        mDecoderBuffers = new MediaCodecBufferCompatWrapper(mDecoder);

        mAudioChannelAdvance = new AudioChannelAdvance(mDecoder, mEncoder, mOutputFormat);
        //处理混音文件
        setupSugar();
    }

    private boolean createSugarExtractor(){
        releaseSugarExtractor();
        mSugarExtractor = new MediaExtractor();
        FileDescriptor fd;
        try {
            fd = DogApplication.getInstance().getAssets().openFd(mRenderConfig.audioPath).getFileDescriptor();
            mSugarExtractor.setDataSource(fd);
        } catch (IOException e) {
            e.printStackTrace();
            releaseSugarExtractor();
            return false;
        }
        return true;
    }

    private void setupSugar() {
        if(mRenderConfig == null){
            return;
        }
        if(!TextUtils.isEmpty(mRenderConfig.audioPath)){
            if(!createSugarExtractor()) return;
            int audioTrackIndex = VideoUtils.findTrackIndex(mSugarExtractor, "audio/");
            if(audioTrackIndex == -1){
                releaseSugarExtractor();
                return;
            }
            mSurgarTrackIndex = audioTrackIndex;
            mSugarExtractor.selectTrack(audioTrackIndex);
            MediaFormat srcMediaFormat = mSugarExtractor.getTrackFormat(audioTrackIndex);
            try {
                mSugarDecoder = MediaCodec.createDecoderByType(srcMediaFormat.getString(MediaFormat.KEY_MIME));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            mSugarDecoder.configure(srcMediaFormat, null, null, 0);
            mSugarDecoder.start();
            mSugarDecoderStarted = true;
            mSugarDecoderBuffers = new MediaCodecBufferCompatWrapper(mSugarDecoder);
        }

    }

    @Override
    public MediaFormat getDeterminedFormat() {
        return mInputFormat;
    }

    @Override
    public boolean stepPipeline() {
        boolean busy = false;

        int status;
        while (drainEncoder(0) != DRAIN_STATE_NONE) busy = true;
        do {
            status = drainDecoder(0);
            if (status != DRAIN_STATE_NONE) busy = true;
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);

        while (mAudioChannelAdvance.feedEncoder(0)) busy = true;
        while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true;

        return busy;
    }

    private int drainExtractor(long timeoutUs) {
        if (mIsExtractorEOS) return DRAIN_STATE_NONE;
        int trackIndex = mExtractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != mTrackIndex) {
            return DRAIN_STATE_NONE;
        }

        final int result = mDecoder.dequeueInputBuffer(timeoutUs);
        if (result < 0) return DRAIN_STATE_NONE;
        if (trackIndex < 0) {
            mIsExtractorEOS = true;
            mDecoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }

        final int sampleSize = mExtractor.readSampleData(mDecoderBuffers.getInputBuffer(result), 0);
        final boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        mDecoder.queueInputBuffer(result, 0, sampleSize, mExtractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mExtractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    private int drainSugarExtractor(long timeoutUs){
        if(mIsSurgarExtractorEOS) return DRAIN_STATE_NONE;
        int trackIndex = mSugarExtractor.getSampleTrackIndex();
        if(trackIndex >= 0 && trackIndex != mSurgarTrackIndex) {
            return DRAIN_STATE_NONE;
        }
        final int result = mSugarDecoder.dequeueInputBuffer(timeoutUs);
        if(result < 0) return DRAIN_STATE_NONE;
        /** 读取完毕后，重新从头读取 */
        if(trackIndex < 0){
            createSugarExtractor();
            mSugarExtractor.selectTrack(mSurgarTrackIndex);
        }
        final int sampleSize = mSugarExtractor.readSampleData(mSugarDecoderBuffers.getInputBuffer(result), 0);
        final boolean isKeyFrame = (mSugarExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        mSugarDecoder.queueInputBuffer(result,  0, sampleSize, mSugarExtractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mSugarExtractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    private int drainDecoder(long timeoutUs) {
        if (mIsDecoderEOS) return DRAIN_STATE_NONE;

        int result = mDecoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                mAudioChannelAdvance.setActualDecodedFormat(mDecoder.getOutputFormat());
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsDecoderEOS = true;
            mAudioChannelAdvance.drainDecoderBufferAndQueue(AudioChannel.BUFFER_INDEX_END_OF_STREAM, 0);
        } else if (mBufferInfo.size > 0) {
            mAudioChannelAdvance.drainDecoderBufferAndQueue(result, mBufferInfo.presentationTimeUs);
        }

        return DRAIN_STATE_CONSUMED;
    }

    private int drainSugarDecoder(long timeoutUs){
        if(mIsSurgarDecoderEOS) return DRAIN_STATE_NONE;

        int result = mSugarDecoder.dequeueOutputBuffer(mSugarBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                //TODO setSugarActualDecodedFormat
                mAudioChannelAdvance.setSuagrActualDecodedFormat(mSugarDecoder.getOutputFormat());
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if (mSugarBufferInfo.size > 0) {
            //TODO drainDecoderBufferAndQueue
            mAudioChannelAdvance.drainSugarDecoderBufferAndQueue(result, mSugarBufferInfo.presentationTimeUs);
        }
        return DRAIN_STATE_CONSUMED;

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int drainEncoder(long timeoutUs) {
        if (mIsEncoderEOS) return DRAIN_STATE_NONE;

        int result = mEncoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                if (mActualOutputFormat != null) {
                    throw new RuntimeException("Audio output format changed twice.");
                }
                mActualOutputFormat = mEncoder.getOutputFormat();
                mMuxer.setOutputFormat(SAMPLE_TYPE, mActualOutputFormat);
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mEncoderBuffers = new MediaCodecBufferCompatWrapper(mEncoder);
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if (mActualOutputFormat == null) {
            throw new RuntimeException("Could not determine actual output format.");
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsEncoderEOS = true;
            mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
            mEncoder.releaseOutputBuffer(result, false);
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        mMuxer.writeSampleData(SAMPLE_TYPE, mEncoderBuffers.getOutputBuffer(result), mBufferInfo);
        mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;
        mEncoder.releaseOutputBuffer(result, false);
        return DRAIN_STATE_CONSUMED;
    }

    @Override
    public long getWrittenPresentationTimeUs() {
        return mWrittenPresentationTimeUs;
    }

    @Override
    public boolean isFinished() {
        return mIsEncoderEOS;
    }

    @Override
    public void release() {
        if (mDecoder != null) {
            if (mDecoderStarted) mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mEncoder != null) {
            if (mEncoderStarted) mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        releaseSuagrDecoder();
    }

    private void releaseSuagrDecoder(){
        if (mSugarDecoder != null) {
            if (mDecoderStarted) mSugarDecoder.stop();
            mSugarDecoder.release();
            mSugarDecoder = null;
        }
    }

    private void releaseSugarExtractor(){
        if(mSugarExtractor != null){
            mSugarExtractor.release();
            mSugarExtractor = null;
        }
    }
}
