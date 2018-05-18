package com.dogcamera.filter;

import android.opengl.GLES20;

import com.dogcamera.DogApplication;
import com.dogcamera.R;
import com.dogcamera.utils.OpenGlUtils;

/**
 * Created by Administrator on 2016/5/22.
 */
public class GPUImageBeautyFilter extends GPUImageFilter {

    private static int BEAUTY_LEVEL = 5;

    private int mSingleStepOffsetLocation;
    private int mParamsLocation;

    public GPUImageBeautyFilter(){
        super(NO_FILTER_VERTEX_SHADER ,
                OpenGlUtils.readTextFromRawResource(DogApplication.getInstance(), R.raw.beauty));
    }

    public void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
        setBeautyLevel(BEAUTY_LEVEL);
    }

    private void setTexelSize(final float w, final float h) {
        setFloatVec2(mSingleStepOffsetLocation, new float[] {2.0f / w, 2.0f / h});
    }


    public void setBeautyLevel(int level){
        switch (level) {
            case 1:
                setFloat(mParamsLocation, 1.0f);
                break;
            case 2:
                setFloat(mParamsLocation, 0.8f);
                break;
            case 3:
                setFloat(mParamsLocation,0.6f);
                break;
            case 4:
                setFloat(mParamsLocation, 0.4f);
                break;
            case 5:
                setFloat(mParamsLocation,0.33f);
                break;
            default:
                break;
        }
    }

}
