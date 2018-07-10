package com.dogcamera.transcode.engine;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;

import com.dogcamera.DogApplication;
import com.dogcamera.transcode.compat.MediaCodecBufferCompatWrapper;
import com.dogcamera.utils.VideoUtils;

import java.io.IOException;
/**
 *                             _ooOoo_
 *                            o8888888o
 *                            88" . "88
 *                            (| -_- |)
 *                            O\  =  /O
 *                         ____/`---'\____
 *                       .'  \\|     |//  `.
 *                      /  \\|||  :  |||//  \
 *                     /  _||||| -:- |||||-  \
 *                     |   | \\\  -  /// |   |
 *                     | \_|  ''\---/''  |   |
 *                     \  .-\__  `-`  ___/-. /
 *                   ___`. .'  /--.--\  `. . __
 *                ."" '<  `.___\_<|>_/___.'  >'"".
 *               | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *               \  \ `-.   \_ __\ /__ _/   .-` /  /
 *          ======`-.____`-.___\_____/___.-`____.-'======
 *                             `=---='
 *          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *                     佛祖保佑        永无BUG
 *            佛曰:
 *                   写字楼里写字间，写字间里程序员；
 *                   程序人员写程序，又拿程序换酒钱。
 *                   酒醒只在网上坐，酒醉还来网下眠；
 *                   酒醉酒醒日复日，网上网下年复年。
 *                   但愿老死电脑间，不愿鞠躬老板前；
 *                   奔驰宝马贵者趣，公交自行程序员。
 *                   别人笑我忒疯癫，我笑自己命太贱；
 *                   不见满街漂亮妹，哪个归得程序员？
 *
 *
 * 混音的类，但是这种实现方式不好，容易出bug，也比较耗内存。后期决定废弃这个类，将混音功能单独提出来。
 */
@Deprecated
public class AudioTrackTranscoderUgly implements TrackTranscoder {

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

    private AudioChannelUgly mAudioChannelUgly;

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
    private long mMixDuration = -1;
    private long mSugarExtractLoopCount = 0;
    private long mSugarDuration = -1;
    //混音模式
    private static final int MIX_NONE = 0;
    private static final int MIX_ORIGIN_ONLY = 1;
    private static final int MIX_MUSIC_ONLY = 2;
    private static final int MASK_FOR_MIX = 3;
    private int mMixFlags = 0;

    public AudioTrackTranscoderUgly(MediaExtractor extractor, int trackIndex,
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
        mMixDuration = mRenderConfig.duration;
        if(mMixDuration < 0){
            throw new IllegalStateException("Origin Audio Duration is less than 0.");
        }
        processMixFlags();
        if((mMixFlags & MASK_FOR_MIX) == MIX_NONE){
            return;
        }

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

        mAudioChannelUgly = new AudioChannelUgly(mDecoder, mEncoder, mOutputFormat);
        //处理混音文件
        setupSugar();
    }

    private boolean createSugarExtractor(){
        releaseSugarExtractor();
        mSugarExtractor = new MediaExtractor();
        AssetFileDescriptor afd;
        try {
            afd = DogApplication.getInstance().getAssets().openFd(mRenderConfig.audioPath);
            mSugarExtractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
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

            mSugarDuration = srcMediaFormat.getLong(MediaFormat.KEY_DURATION);
            if(mSugarDuration < 0){
                throw new IllegalStateException("Sugar Audio Duration is less than 0.");
            }
            mAudioChannelUgly.setSugarConfig(mSugarDecoder);
        }



    }

    private void processMixFlags() {
        if(mRenderConfig == null){
            mMixFlags = (mMixFlags &~ MASK_FOR_MIX) | (MIX_ORIGIN_ONLY & MASK_FOR_MIX);
        }else if(TextUtils.isEmpty(mRenderConfig.audioPath) && mRenderConfig.originMute){
            mMixFlags = (mMixFlags & ~MASK_FOR_MIX);
        }else{
            if(!TextUtils.isEmpty(mRenderConfig.audioPath))
                mMixFlags |= MIX_MUSIC_ONLY;
            if(!mRenderConfig.originMute)
                mMixFlags |= MIX_ORIGIN_ONLY;
        }
    }

    @Override
    public MediaFormat getDeterminedFormat() {
        return mInputFormat;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean stepPipeline() {
        boolean busy = false;
        //没有任何声音
        if((mMixFlags & MASK_FOR_MIX) == MIX_NONE){
            if(!mIsEncoderEOS){
                mIsEncoderEOS = true;
                mMuxer.setOutputFormat(SAMPLE_TYPE, null);
            }
            return busy;
        }
        int status = DRAIN_STATE_NONE;
        while (drainEncoder(0) != DRAIN_STATE_NONE) busy = true;
        do {
            //原声
            if((mMixFlags & MIX_ORIGIN_ONLY) != 0)
                status = drainDecoder(0);
            //音乐
            if((mMixFlags & MIX_MUSIC_ONLY) != 0)
                status = drainSugarDecoder(0);

            if (status != DRAIN_STATE_NONE) busy = true;
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);
        //仅仅原声音
        if((mMixFlags & MASK_FOR_MIX) == MIX_ORIGIN_ONLY)
            while (mAudioChannelUgly.feedEncoder(0, false)) busy = true;
        //仅仅音乐
        if((mMixFlags & MASK_FOR_MIX) == MIX_MUSIC_ONLY)
            while (mAudioChannelUgly.feedEncoder(0, true)) busy = true;
        //两个都有
        if((mMixFlags & MASK_FOR_MIX) == MIX_MUSIC_ONLY + MIX_ORIGIN_ONLY)
            while (mAudioChannelUgly.feedEncoderWithSuagr(0)) busy = true;

        //原声
        if((mMixFlags & MIX_ORIGIN_ONLY) != 0)
            while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true;
        //音乐
        if((mMixFlags & MIX_MUSIC_ONLY) != 0)
            while (drainSugarExtractor(0) != DRAIN_STATE_NONE) busy = true;

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
            mIsSurgarExtractorEOS = true;
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
            //one time sugar EOS
            mDecoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mSugarExtractLoopCount += 1;
            //读取完毕后，重新从头读取
            createSugarExtractor();
            mSugarExtractor.selectTrack(mSurgarTrackIndex);
            return DRAIN_STATE_NONE;
        }

        final int sampleSize = mSugarExtractor.readSampleData(mSugarDecoderBuffers.getInputBuffer(result), 0);
        final boolean isKeyFrame = (mSugarExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        mSugarDecoder.queueInputBuffer(result,  0, sampleSize, mSugarExtractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mSugarExtractor.advance();
        //如果解析长度超过了视频总时长，就不再读取
        long processPTS = mSugarDuration * mSugarExtractLoopCount + mSugarExtractor.getSampleTime();
        if (processPTS >= mMixDuration) {
            mIsSurgarExtractorEOS = true;
            return DRAIN_STATE_NONE;
        }
        return DRAIN_STATE_CONSUMED;
    }

    private int drainDecoder(long timeoutUs) {
        if (mIsDecoderEOS) return DRAIN_STATE_NONE;

        int result = mDecoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                mAudioChannelUgly.setActualDecodedFormat(mDecoder.getOutputFormat());
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsDecoderEOS = true;
            mIsSurgarDecoderEOS = true;
            mAudioChannelUgly.drainDecoderBufferAndQueue(AudioChannel.BUFFER_INDEX_END_OF_STREAM, 0);
        } else if (mBufferInfo.size > 0) {
            mAudioChannelUgly.drainDecoderBufferAndQueue(result, mBufferInfo.presentationTimeUs);
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
                boolean onlySugar = ((mMixFlags & MASK_FOR_MIX) == MIX_MUSIC_ONLY);
                mAudioChannelUgly.setSuagrActualDecodedFormat(mSugarDecoder.getOutputFormat(), onlySugar);
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if ((mSugarBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            //one time sugar EOS
            mAudioChannelUgly.drainSugarDecoderBufferAndQueue(AudioChannel.BUFFER_INDEX_END_OF_STREAM, 0);
        } else if (mSugarBufferInfo.size > 0) {
            //TODO drainDecoderBufferAndQueue
            mAudioChannelUgly.drainSugarDecoderBufferAndQueue(result, mSugarBufferInfo.presentationTimeUs);
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
            if (mSugarDecoderStarted) mSugarDecoder.stop();
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
