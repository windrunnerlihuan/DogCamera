package com.dogcamera.av;

/**
 * Created by huanli on 2018/2/28.
 */

import android.opengl.EGLContext;

import java.io.File;


/**
 * Encoder configuration.
 * <p>
 * Object is immutable, which means we can safely pass it between threads without
 * explicit synchronization (and don't need to worry about it getting tweaked out from
 * under us).
 * <p>
 * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
 * with reasonable defaults for those and bit rate.
 */
public class EncoderConfig {
    EGLContext mEglContext;
    File mOutputFile;
    int mWidth;
    int mHeight;

    public EncoderConfig(EGLContext eglContext, File outputFile, int width, int height) {
        mEglContext = eglContext;
        mOutputFile = outputFile;
        mWidth = width;
        mHeight = height;
    }

}


