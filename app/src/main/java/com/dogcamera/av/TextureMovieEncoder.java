/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dogcamera.av;

import android.annotation.TargetApi;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.dogcamera.filter.GPUImageExtTexFilter;
import com.dogcamera.filter.GPUImageFilter;
import com.dogcamera.filter.GPUImageFilterGroup;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
@TargetApi(18)
public class TextureMovieEncoder implements Runnable {
    private static final String TAG = "TextureMovieEncoder";

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_SCALE_MVP_MATRIX = 2;
    private static final int MSG_FRAME_AVAILABLE = 3;
    private static final int MSG_SET_TEXTURE_ID = 4;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 6;
    private static final int MSG_QUIT = 8;

    private static final int ERROR_INIT = 0;
    private static final int ERROR_ENCODE = 1;
    private static final int ERROR_RELEASE = 2;

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private int mTextureId;
    private VideoEncoderCore mVideoEncoder;
    private AudioEncoderCore mAudioEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private volatile Object mReadyFence = new Object();      // guards ready/running
    private volatile boolean mReady;
    private volatile boolean mRunning;

    //GPUImageFilter
    private GPUImageFilterGroup mGPUImageFilterGroup;
    private GPUImageFilter mCurrentFilter;

    private EncoderConfig mConfig;

    private double mRecordSpeed = 1;//录制的速度 1(正常速度)
    private boolean mRecordAudio = true;//是否录制声音

    private boolean mOccurError = false;


    public TextureMovieEncoder() {
    }

    public void setRecordSpeed(double recordSpeed) {
        mRecordSpeed = recordSpeed;
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        System.out.println(TAG + " startRecording" + " thread:" + Thread.currentThread() + " time:" + System.currentTimeMillis());

        synchronized (mReadyFence) {
            if (mRunning) {
                //上一个线程还没有退出 (这时候录制就会失败)
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }


    public void scaleMVPMatrix() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SCALE_MVP_MATRIX));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        System.out.println(TAG + " stopRecording" + " thread:" + Thread.currentThread() + " time:" + System.currentTimeMillis());
        mReady = mRunning = false;
        if (mHandler == null) {
            System.out.println(TAG + " mHandler null");
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        //mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));

        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(long timestamp) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, (int) (timestamp >> 32),
                (int) timestamp));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
            System.out.println(TAG + " run" + " thread:" + Thread.currentThread() + " time:" + System.currentTimeMillis());
        }
        Looper.loop();

//        synchronized (mReadyFence) {
//            mReady = mRunning = false;
//            mHandler = null;
//        }
        Log.d(TAG, "Encoder thread exiting");
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    Looper looper1 = Looper.myLooper();
                    if (looper1 != null) {
                        looper1.quit();
                    }
                    break;

                case MSG_SCALE_MVP_MATRIX:
                    encoder.handleSaleMVPMatrix();
                    break;

                case MSG_FRAME_AVAILABLE:
                    long timestamp =
                            (((long) inputMessage.arg1) << 32) | (((long) inputMessage.arg2)
                                    & 0xffffffffL);
                    encoder.handleFrameAvailable(timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;

                case MSG_QUIT:
                    Looper looper = Looper.myLooper();
                    if (looper != null) {
                        looper.quit();
                    }
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        mConfig = config;
        mOccurError = false;
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight,
                config.mOutputFile);
    }

    public void setCubeAndTextureBuffer(FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        mGLCubeBuffer = cubeBuffer;
        mGLTextureBuffer = textureBuffer;
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     *
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(long timestampNanos) {
        if (mOccurError) {
            return;
        }
        try {
            mVideoEncoder.start();
        } catch (Exception e) {
            if (mOnErrorListener != null) {
                mOnErrorListener.onError(ERROR_ENCODE);
            }
            mOccurError = true;
            return;
        }
        if (mRecordSpeed == 1 && mRecordAudio) {
            mAudioEncoder.start();
        }

        if (mGPUImageFilterGroup != null && mGLCubeBuffer != null && mGLTextureBuffer != null) {
            mGPUImageFilterGroup.onDraw(mTextureId, mGLCubeBuffer, mGLTextureBuffer);
        }

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        if (mVideoEncoder != null) {
            try {
                mVideoEncoder.stop();
            } catch (Exception e) {
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(ERROR_RELEASE);
                }
                mOccurError = true;
            }
        }
        if (mRecordSpeed == 1 && mRecordAudio && mAudioEncoder != null) {
            mAudioEncoder.stop();
        }
        releaseEncoder();
    }

    private void handleSaleMVPMatrix() {
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        mTextureId = id;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        if (mOccurError) {
            return;
        }
        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.

        initGPUImageFilter(mConfig.mWidth, mConfig.mHeight, null);

    }


    private void prepareEncoder(EGLContext sharedContext, int width, int height,
                                File outputFile) {
        AndroidMuxer muxer = new AndroidMuxer(outputFile.getPath());

        try {
            mVideoEncoder = new VideoEncoderCore(muxer, width, height);
            if (mRecordSpeed == 1 && mRecordAudio) {
                mAudioEncoder = new AudioEncoderCore(muxer);
                muxer.setExpectedNumTracks(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mVideoEncoder == null) { //init error
            if (mOnErrorListener != null) {
                mOnErrorListener.onError(ERROR_INIT);
            }
            mOccurError = true;
            return;
        }

        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);

        try {
            mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
            mInputWindowSurface.makeCurrent();
        } catch (Exception e) {
            if (mOnErrorListener != null) {
                mOnErrorListener.onError(ERROR_INIT);
            }
            mOccurError = true;
            return;
        }

        initGPUImageFilter(width, height, null);

    }

    public void initGPUImageFilter(int w, int h, GPUImageFilter filter) {
        if (mGPUImageFilterGroup != null) {
            mGPUImageFilterGroup.destroy();
            mGPUImageFilterGroup = null;
        }

        mGPUImageFilterGroup = new GPUImageFilterGroup();

        mGPUImageFilterGroup.addFilter(new GPUImageExtTexFilter());

        mGPUImageFilterGroup.addFilter(filter);

        mGPUImageFilterGroup.init();

        mGPUImageFilterGroup.onOutputSizeChanged(w, h);
    }

    private void releaseEncoder() {

        if (mVideoEncoder != null) {
            try {
                mVideoEncoder.release();
            } catch (Exception e) {
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(ERROR_RELEASE);
                }
                mOccurError = true;
            }
        }

        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (mGPUImageFilterGroup != null) {
            mGPUImageFilterGroup.destroy();
            mGPUImageFilterGroup = null;
        }
    }

    private OnErrorListener mOnErrorListener;

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    public interface OnErrorListener {
        void onError(int errorType);
    }

}
