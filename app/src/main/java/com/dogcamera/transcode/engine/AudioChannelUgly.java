package com.dogcamera.transcode.engine;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.dogcamera.transcode.compat.MediaCodecBufferCompatWrapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Channel of raw audio from decoder to encoder.
 * Performs the necessary conversion between different input & output audio formats.
 * <p>
 * We currently support upmixing from mono to stereo & downmixing from stereo to mono.
 * Sample rate conversion is not supported yet.
 */
class AudioChannelUgly {

    private static class AudioBuffer {
        int bufferIndex;
        long presentationTimeUs;
        ShortBuffer data;
    }

    public static final int BUFFER_INDEX_END_OF_STREAM = -1;

    private static final int BYTES_PER_SHORT = 2;
    private static final long MICROSECS_PER_SEC = 1000000;

    private final ArrayDeque<AudioBuffer> mEmptyBuffers = new ArrayDeque<>();
    private final ArrayDeque<AudioBuffer> mFilledBuffers = new ArrayDeque<>();

    private final MediaCodec mDecoder;
    private final MediaCodec mEncoder;
    private final MediaFormat mEncodeFormat;

    private int mInputSampleRate;
    private int mInputChannelCount;
    private int mOutputChannelCount;

    private AudioRemixer mRemixer;

    private final MediaCodecBufferCompatWrapper mDecoderBuffers;
    private final MediaCodecBufferCompatWrapper mEncoderBuffers;

    private final AudioBuffer mOverflowBuffer = new AudioBuffer();

    private MediaFormat mActualDecodedFormat;

    //混音相关配置
    private MediaFormat mSugarActualDecodedFormat;
    private MediaCodec mSugarDecoder;
    private MediaCodecBufferCompatWrapper mSugarDecoderBuffers;
    private int mSugarInputSampleRate;
    private int mSugarInputChannelCount;
    private AudioRemixer mConvertSugarToInputRemixer;
    private final ArrayDeque<AudioBuffer> mSugarFilledBuffers = new ArrayDeque<>();
    private static final int MIX_OVERFLOW_BUFFER_INDEX = Integer.MIN_VALUE;


    public AudioChannelUgly(final MediaCodec decoder,
                            final MediaCodec encoder, final MediaFormat encodeFormat) {
        mDecoder = decoder;
        mEncoder = encoder;
        mEncodeFormat = encodeFormat;

        mDecoderBuffers = new MediaCodecBufferCompatWrapper(mDecoder);
        mEncoderBuffers = new MediaCodecBufferCompatWrapper(mEncoder);
    }

    public void setSugarConfig(MediaCodec decoder) {
        mSugarDecoder = decoder;
        mSugarDecoderBuffers = new MediaCodecBufferCompatWrapper(mSugarDecoder);
    }

    public void setActualDecodedFormat(final MediaFormat decodedFormat) {
        mActualDecodedFormat = decodedFormat;

        mInputSampleRate = mActualDecodedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        if (mInputSampleRate != mEncodeFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
            throw new UnsupportedOperationException("Audio sample rate conversion not supported yet.");
        }

        mInputChannelCount = mActualDecodedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mOutputChannelCount = mEncodeFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        if (mInputChannelCount != 1 && mInputChannelCount != 2) {
            throw new UnsupportedOperationException("Input channel count (" + mInputChannelCount + ") not supported.");
        }

        if (mOutputChannelCount != 1 && mOutputChannelCount != 2) {
            throw new UnsupportedOperationException("Output channel count (" + mOutputChannelCount + ") not supported.");
        }

        if (mInputChannelCount > mOutputChannelCount) {
            mRemixer = AudioRemixer.DOWNMIX;
        } else if (mInputChannelCount < mOutputChannelCount) {
            mRemixer = AudioRemixer.UPMIX;
        } else {
            mRemixer = AudioRemixer.PASSTHROUGH;
        }

        mOverflowBuffer.presentationTimeUs = 0;
    }

    public void setSuagrActualDecodedFormat(final MediaFormat decodedFormat) {
        mSugarActualDecodedFormat = decodedFormat;

        mSugarInputSampleRate = mSugarActualDecodedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        if (mSugarInputSampleRate != mEncodeFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
            throw new UnsupportedOperationException("Audio sample rate conversion not supported yet.");
        }

        mSugarInputChannelCount = mSugarActualDecodedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mOutputChannelCount = mEncodeFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        if (mSugarInputChannelCount != 1 && mSugarInputChannelCount != 2) {
            throw new UnsupportedOperationException("Input channel count (" + mInputChannelCount + ") not supported.");
        }

        if (mOutputChannelCount != 1 && mOutputChannelCount != 2) {
            throw new UnsupportedOperationException("Output channel count (" + mOutputChannelCount + ") not supported.");
        }

        if (mSugarInputChannelCount > mInputChannelCount) {
            mConvertSugarToInputRemixer = AudioRemixer.DOWNMIX;
        } else if (mSugarInputChannelCount < mInputChannelCount) {
            mConvertSugarToInputRemixer = AudioRemixer.UPMIX;
        } else {
            mConvertSugarToInputRemixer = AudioRemixer.PASSTHROUGH;
        }

    }

    public void drainDecoderBufferAndQueue(final int bufferIndex, final long presentationTimeUs) {
        if (mActualDecodedFormat == null) {
            throw new RuntimeException("Buffer received before format!");
        }

        final ByteBuffer data =
                bufferIndex == BUFFER_INDEX_END_OF_STREAM ?
                        null : mDecoderBuffers.getOutputBuffer(bufferIndex);

        AudioBuffer buffer = mEmptyBuffers.poll();
        if (buffer == null) {
            buffer = new AudioBuffer();
        }

        buffer.bufferIndex = bufferIndex;
        buffer.presentationTimeUs = presentationTimeUs;
        buffer.data = data == null ? null : data.asShortBuffer();

        if (mOverflowBuffer.data == null) {
            mOverflowBuffer.data = ByteBuffer
                    .allocateDirect(data.capacity())
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();
            mOverflowBuffer.data.clear().flip();
        }

        mFilledBuffers.add(buffer);
    }

    public void drainSugarDecoderBufferAndQueue(final int bufferIndex, final long presentationTimeUs) {
        if (mSugarActualDecodedFormat == null) {
            throw new RuntimeException("Buffer received before format!");
        }

        final ByteBuffer data =
                bufferIndex == BUFFER_INDEX_END_OF_STREAM ?
                        null : mSugarDecoderBuffers.getOutputBuffer(bufferIndex);

        AudioBuffer buffer = mEmptyBuffers.poll();
        if (buffer == null) {
            buffer = new AudioBuffer();
        }

        buffer.bufferIndex = bufferIndex;
        buffer.presentationTimeUs = presentationTimeUs;
        buffer.data = data == null ? null : data.asShortBuffer();

        if (mOverflowBuffer.data != null) {
            int oldCapacity = mOverflowBuffer.data.capacity();
            if (data.capacity() > oldCapacity) {
                mOverflowBuffer.data.clear();
                mOverflowBuffer.data = ByteBuffer
                        .allocateDirect(data.capacity())
                        .order(ByteOrder.nativeOrder())
                        .asShortBuffer();
                mOverflowBuffer.data.clear().flip();
            }
        }

        mSugarFilledBuffers.add(buffer);
    }


    public boolean feedEncoder(long timeoutUs) {
        final boolean hasOverflow = mOverflowBuffer.data != null && mOverflowBuffer.data.hasRemaining();
        if (mFilledBuffers.isEmpty() && !hasOverflow) {
            // No audio data - Bail out
            return false;
        }

        final int encoderInBuffIndex = mEncoder.dequeueInputBuffer(timeoutUs);
        if (encoderInBuffIndex < 0) {
            // Encoder is full - Bail out
            return false;
        }

        // Drain overflow first
        final ShortBuffer outBuffer = mEncoderBuffers.getInputBuffer(encoderInBuffIndex).asShortBuffer();
        if (hasOverflow) {
            final long presentationTimeUs = drainOverflow(outBuffer);
            mEncoder.queueInputBuffer(encoderInBuffIndex,
                    0, outBuffer.position() * BYTES_PER_SHORT,
                    presentationTimeUs, 0);
            return true;
        }

        final AudioBuffer inBuffer = mFilledBuffers.poll();
        if (inBuffer.bufferIndex == BUFFER_INDEX_END_OF_STREAM) {
            mEncoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }

        final long presentationTimeUs = remixAndMaybeFillOverflow(inBuffer, outBuffer);
        mEncoder.queueInputBuffer(encoderInBuffIndex,
                0, outBuffer.position() * BYTES_PER_SHORT,
                presentationTimeUs, 0);
        if (inBuffer != null) {
            mDecoder.releaseOutputBuffer(inBuffer.bufferIndex, false);
            mEmptyBuffers.add(inBuffer);
        }

        return true;
    }

    public boolean feedEncoderWithSuagr(long timeoutUs) {
        final boolean hasOverflow = mOverflowBuffer.data != null && mOverflowBuffer.data.hasRemaining();
        if (mFilledBuffers.isEmpty() && !hasOverflow) {
            // No audio data - Bail out
            return false;
        }

        final int encoderInBuffIndex = mEncoder.dequeueInputBuffer(timeoutUs);
        if (encoderInBuffIndex < 0) {
            // Encoder is full - Bail out
            return false;
        }

        // Drain overflow first
        final ShortBuffer outBuffer = mEncoderBuffers.getInputBuffer(encoderInBuffIndex).asShortBuffer();
        if (hasOverflow) {
            final long presentationTimeUs = drainOverflow(outBuffer);
            mEncoder.queueInputBuffer(encoderInBuffIndex,
                    0, outBuffer.position() * BYTES_PER_SHORT,
                    presentationTimeUs, 0);
            return true;
        }
        final AudioBuffer srcBuffer = mFilledBuffers.poll();
        if (srcBuffer.bufferIndex == BUFFER_INDEX_END_OF_STREAM) {
            mEncoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }
        if (mSugarFilledBuffers.isEmpty()) {
            // No sugar data - don't mix
            return false;
        }
        final AudioBuffer sugarBuffer = mSugarFilledBuffers.poll();
        int srcCapacity = srcBuffer.data.remaining();
        int sugarCapacity = sugarBuffer.data.remaining();
        int minBufferReadNum = Math.min(srcCapacity, sugarCapacity);

        sugarBuffer.data.clear();
        ShortBuffer convertSugarShortBuffer = ByteBuffer.allocate(sugarCapacity * BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
        mConvertSugarToInputRemixer.remix(sugarBuffer.data, convertSugarShortBuffer);
        convertSugarShortBuffer.flip();

        srcBuffer.data.clear();
        ShortBuffer srcShortBuffer = srcBuffer.data;

        ShortBuffer mixInBuffer = ByteBuffer.allocate(minBufferReadNum * BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
        for (int i = 0; i < minBufferReadNum; i++) {
            int a = srcShortBuffer.get();
            int b = convertSugarShortBuffer.get();
            short result;
            if (a < 0 && b < 0) {
                int i1 = a + b - a * b / (-32768);
                if (i1 > 32767) {
                    result = 32767;
                } else if (i1 < -32768) {
                    result = -32768;
                } else {
                    result = (short) i1;
                }
            } else if (a > 0 && b > 0) {
                int i1 = a + b - a * b / 32767;
                if (i1 > 32767) {
                    result = 32767;
                } else if (i1 < -32768) {
                    result = -32768;
                } else {
                    result = (short) i1;
                }
            } else {
                int i1 = a + b;
                if (i1 > 32767) {
                    result = 32767;
                } else if (i1 < -32768) {
                    result = -32768;
                } else {
                    result = (short) i1;
                }
            }
            mixInBuffer.put(result);
        }
        int mixOverFlowNum = srcCapacity - sugarCapacity;
        if (mixOverFlowNum != 0) {
            ShortBuffer remixOverFlowBuffer = ByteBuffer.allocate(Math.abs(mixOverFlowNum) * BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
            ShortBuffer maxShortBuffer = mixOverFlowNum > 0 ? srcShortBuffer : convertSugarShortBuffer;
            remixOverFlowBuffer.put(maxShortBuffer);

            AudioBuffer buffer = mEmptyBuffers.poll();
            if (buffer == null) {
                buffer = new AudioBuffer();
            }
            final long consumedDurationUs =
                    sampleCountToDurationUs(maxShortBuffer.position(),
                            mixOverFlowNum > 0 ? mInputSampleRate : mSugarInputSampleRate,
                            mixOverFlowNum > 0 ? mInputChannelCount : mSugarInputChannelCount);
            remixOverFlowBuffer.flip();
            buffer.data = remixOverFlowBuffer;
            buffer.bufferIndex = MIX_OVERFLOW_BUFFER_INDEX;
            buffer.presentationTimeUs = (mixOverFlowNum > 0 ? srcBuffer : sugarBuffer).presentationTimeUs + consumedDurationUs;
            if (mixOverFlowNum > 0) {
                mFilledBuffers.addFirst(buffer);
            } else {
                mSugarFilledBuffers.addFirst(buffer);
            }


        }

        AudioBuffer inBuffer = mEmptyBuffers.poll();
        if(inBuffer == null) inBuffer = new AudioBuffer();
        inBuffer.bufferIndex = srcBuffer.bufferIndex;
        inBuffer.presentationTimeUs = srcBuffer.presentationTimeUs;
        inBuffer.data = mixInBuffer;

        final long presentationTimeUs = remixAndMaybeFillOverflow(inBuffer, outBuffer);
        mEncoder.queueInputBuffer(encoderInBuffIndex,
                0, outBuffer.position() * BYTES_PER_SHORT,
                presentationTimeUs, 0);
        if (srcBuffer != null && srcBuffer.bufferIndex != MIX_OVERFLOW_BUFFER_INDEX) {
            mDecoder.releaseOutputBuffer(srcBuffer.bufferIndex, false);
            mEmptyBuffers.add(srcBuffer);
        }
        if (sugarBuffer != null && sugarBuffer.bufferIndex != MIX_OVERFLOW_BUFFER_INDEX) {
            mSugarDecoder.releaseOutputBuffer(sugarBuffer.bufferIndex, false);
            mEmptyBuffers.add(sugarBuffer);
        }

        return true;
    }

    private static long sampleCountToDurationUs(final int sampleCount,
                                                final int sampleRate,
                                                final int channelCount) {
        return (sampleCount / (sampleRate * MICROSECS_PER_SEC)) / channelCount;
    }

    private long drainOverflow(final ShortBuffer outBuff) {
        final ShortBuffer overflowBuff = mOverflowBuffer.data;
        final int overflowLimit = overflowBuff.limit();
        final int overflowSize = overflowBuff.remaining();

        final long beginPresentationTimeUs = mOverflowBuffer.presentationTimeUs +
                sampleCountToDurationUs(overflowBuff.position(), mInputSampleRate, mOutputChannelCount);

        outBuff.clear();
        // Limit overflowBuff to outBuff's capacity
        overflowBuff.limit(outBuff.capacity());
        // Load overflowBuff onto outBuff
        outBuff.put(overflowBuff);

        if (overflowSize >= outBuff.capacity()) {
            // Overflow fully consumed - Reset
            overflowBuff.clear().limit(0);
        } else {
            // Only partially consumed - Keep position & restore previous limit
            overflowBuff.limit(overflowLimit);
        }

        return beginPresentationTimeUs;
    }

    private long remixAndMaybeFillOverflow(final AudioBuffer input,
                                           final ShortBuffer outBuff) {
        final ShortBuffer inBuff = input.data;
        final ShortBuffer overflowBuff = mOverflowBuffer.data;

        outBuff.clear();

        // Reset position to 0, and set limit to capacity (Since MediaCodec doesn't do that for us)
        inBuff.clear();

        if (inBuff.remaining() > outBuff.remaining()) {
            // Overflow
            // Limit inBuff to outBuff's capacity
            inBuff.limit(outBuff.capacity());
            mRemixer.remix(inBuff, outBuff);

            // Reset limit to its own capacity & Keep position
            inBuff.limit(inBuff.capacity());

            // Remix the rest onto overflowBuffer
            // NOTE: We should only reach this point when overflow buffer is empty
            final long consumedDurationUs =
                    sampleCountToDurationUs(inBuff.position(), mInputSampleRate, mInputChannelCount);
            mRemixer.remix(inBuff, overflowBuff);

            // Seal off overflowBuff & mark limit
            overflowBuff.flip();
            mOverflowBuffer.presentationTimeUs = input.presentationTimeUs + consumedDurationUs;
        } else {
            // No overflow
            mRemixer.remix(inBuff, outBuff);
        }

        return input.presentationTimeUs;
    }
}
