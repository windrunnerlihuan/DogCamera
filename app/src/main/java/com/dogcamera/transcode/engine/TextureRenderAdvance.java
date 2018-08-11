/*
 * Copyright (C) 2013 The Android Open Source Project
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
// from: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/TextureRender.java
// blob: 4125dcfcfed6ed7fddba5b71d657dec0d433da6a
// modified: removed unused method bodies
// modified: use GL_LINEAR for GL_TEXTURE_MIN_FILTER to improve quality.
package com.dogcamera.transcode.engine;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.dogcamera.filter.GPUImageExtTexFilter;
import com.dogcamera.filter.GPUImageFilterGroup;
import com.dogcamera.filter.GPUImageWaterMarkFilter;
import com.dogcamera.utils.FilterUtils;
import com.dogcamera.utils.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.dogcamera.utils.TextureRotationUtil.TEXTURE_NO_ROTATION;
import static com.dogcamera.utils.VertexUtils.CUBE;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
public class TextureRenderAdvance extends AbsTextureRender{

    private static final String TAG = "TextureRenderAdvance";

    private GPUImageFilterGroup mFilterGroup;

    private RenderConfig mRenderConfig;

    private int mTextureID = OpenGlUtils.NO_TEXTURE;
    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;


    public TextureRenderAdvance(RenderConfig config) {
        mRenderConfig = config;
    }
    @Override
    public int getTextureId() {
        return mTextureID;
    }
    @Override
    public void drawFrame(SurfaceTexture st) {
        mFilterGroup.onDraw(mTextureID, mGLCubeBuffer, mGLTextureBuffer);
    }
    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    @Override
    public void surfaceCreated() {

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

        // init base filter
        initFilters();

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("glBindTexture mTextureID");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");
        mTextureID = textures[0];
    }

    private void initFilters(){
        mFilterGroup = new GPUImageFilterGroup();
        mFilterGroup.addFilter(new GPUImageExtTexFilter());
        //add yourself filter
        mFilterGroup.addFilter(FilterUtils.createFilter(mRenderConfig.filterId));
        GPUImageWaterMarkFilter waterMarkFilter = new GPUImageWaterMarkFilter();
        waterMarkFilter.setDefaultWaterMark();
        mFilterGroup.addFilter(waterMarkFilter);

        mFilterGroup.init();
        GLES20.glUseProgram(mFilterGroup.getProgram());
        mFilterGroup.onOutputSizeChanged(mRenderConfig.outputVideoWidth, mRenderConfig.outputVideoHeight);
    }

}
