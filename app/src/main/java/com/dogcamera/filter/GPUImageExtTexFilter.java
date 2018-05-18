package com.dogcamera.filter;

/**
 * Created by huanli on 2018/2/27.
 */

/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import com.dogcamera.utils.OpenGlUtils;
import java.nio.FloatBuffer;

/**
 * 因为Camera或者MediaCodec产出的Texture都是OES的，所以专门写一个来转。
 * GL program and supporting functions for textured 2D shapes.
 */
public class GPUImageExtTexFilter extends GPUImageFilter {
    private static final String TAG = "GPUImageExtTexFilter";
    private int mGLvf;

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform samplerExternalOES inputImageTexture;\n" +
                    "uniform lowp float vf;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(inputImageTexture, textureCoordinate) * vf;\n" +
                    "}\n";

    public GPUImageExtTexFilter() {
        super(NO_FILTER_VERTEX_SHADER, FRAGMENT_SHADER_EXT);
    }

    @Override
    public void onInit() {
        super.onInit();
        mGLvf = GLES20.glGetUniformLocation(mGLProgId, "vf");
    }

    private volatile float myAlpha = 1.0f;

    public void setAlpha(float alpha) {
        myAlpha = alpha;
    }

    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glUniform1f(mGLvf, myAlpha);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}

