package com.dogcamera.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.dogcamera.av.Rotation;
import com.dogcamera.av.ScaleType;
import com.dogcamera.filter.GPUImageExtTexFilter;
import com.dogcamera.filter.GPUImageFilter;
import com.dogcamera.filter.GPUImageFilterGroup;
import com.dogcamera.utils.OpenGlUtils;
import com.dogcamera.utils.TextureRotationUtil;
import com.dogcamera.utils.VertexUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by huanli on 2018/2/27.
 */
@SuppressLint("NewApi")
public abstract class BaseGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener{

    //顶点坐标
    protected FloatBuffer mGLCubeBuffer;
    //纹理坐标
    protected FloatBuffer mGLTextureBuffer;
    protected int genTextureID = OpenGlUtils.NO_TEXTURE;

    protected SurfaceTexture mSurfaceTexture;

    private Queue<Runnable> mRunOnDraw = new LinkedList<>();
    private Queue<Runnable> mRunAfterSurfaceCreated = new LinkedList<>();

    //控件是有效的，有效的生命周期应该在onSurfaceCreated,onPause之间
    protected volatile boolean surfaceIsValid = false;
    //surface的大小，如果不为0就限定view大小为surface的大小
    protected int mSurfaceWidth;
    protected int mSurfaceHeight;

    protected Rotation mRotation = Rotation.NORMAL;
    protected boolean mFlipHorizontal;
    protected boolean mFlipVertical;
    protected ScaleType mScaleType = ScaleType.CENTER_CROP;

    protected GPUImageFilter mCurrentGPUImageFilter = null;
    protected GPUImageFilterGroup gpuImageFilter;

    public BaseGLSurfaceView(Context context) {
        this(context, null);
    }

    public BaseGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        //surface创建之后做一些操作
        runAll(mRunAfterSurfaceCreated);
        //初始化gpu相关
        gpuInit();
        //初始化Surface
        onSurfaceInit();
        //调整预览大小
        adapterPreviewSize();
        //添加当前滤镜
        changeGpuImageFilter(mCurrentGPUImageFilter);
        surfaceIsValid = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mSurfaceTexture.updateTexImage();
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        runAll(mRunOnDraw);
        if(gpuImageFilter != null){
            gpuImageFilter.onDraw(genTextureID, mGLCubeBuffer, mGLTextureBuffer);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        OpenGlUtils.deleteValidTexture();
        surfaceIsValid = false;
        mRunAfterSurfaceCreated.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(mSurfaceWidth == 0 || mSurfaceHeight == 0){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }else{
            setMeasuredDimension(mSurfaceWidth, mSurfaceHeight);
        }
    }

    private void gpuInit(){
        mGLCubeBuffer = ByteBuffer.allocateDirect(VertexUtils.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(VertexUtils.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        // 指定生成N个纹理（第一个参数指定生成1个纹理），
        // textures数组将负责存储所有纹理的代号
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        genTextureID = textures[0];
        //设置纹理过滤参数
        // 设置纹理被缩小（距离视点很远时被缩小）时候的滤波方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        // 设置纹理被放大（距离视点很近时被方法）时候的滤波方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 设置在横向、纵向上都是平铺纹理
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        mSurfaceTexture = new SurfaceTexture(genTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    private void adapterPreviewSize() {
        if (mSurfaceWidth <= 0 || mSurfaceHeight <= 0) {
            mSurfaceWidth = this.getMeasuredWidth();
            mSurfaceHeight = this.getMeasuredHeight();
        }
        adjustImageScaling();
    }

    public void adjustImageScaling() {

        int surfaceWidth = getSurfaceWidth();
        int surfaceHeight = getSurfaceHeight();
        //如果旋转了90或者270度，就要吧宽高调换
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            surfaceWidth = getSurfaceHeight();
            surfaceHeight = getSurfaceWidth();
        }
        //获取视频原始宽高
        int imageWidth = getImageWidth();
        int imageHeight = getImageHeight();

        float ratio1 = (float) surfaceWidth / imageWidth;
        float ratio2 = (float) surfaceHeight / imageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);

        float ratioWidth = (float) imageWidthNew / surfaceWidth;
        float ratioHeight = (float) imageHeightNew / surfaceHeight;

        float[] cube = VertexUtils.CUBE;
        float[] textureCords;
        textureCords = TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
        //center_crop缩放图片后剪裁，剩余内容居中
        if (mScaleType == ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            cube = new float[]{
                    VertexUtils.CUBE[0] / ratioHeight, VertexUtils.CUBE[1] / ratioWidth,
                    VertexUtils.CUBE[2] / ratioHeight, VertexUtils.CUBE[3] / ratioWidth,
                    VertexUtils.CUBE[4] / ratioHeight, VertexUtils.CUBE[5] / ratioWidth,
                    VertexUtils.CUBE[6] / ratioHeight, VertexUtils.CUBE[7] / ratioWidth,
            };
        }else if (mScaleType == ScaleType.FIT_XY){

        }

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }

    protected float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    public void changeGpuImageFilter(final GPUImageFilter filter) {
        mCurrentGPUImageFilter = filter;
        runOnDraw(() -> initGPUImageFilter(mSurfaceWidth, mSurfaceHeight, filter));
    }

    public GPUImageFilter getCurrentGPUImageFilter() {
        return mCurrentGPUImageFilter;
    }

    private void initGPUImageFilter(int w, int h, GPUImageFilter filter){
        if(gpuImageFilter != null){
            gpuImageFilter.destroy();
            gpuImageFilter = null;
        }
        gpuImageFilter = new GPUImageFilterGroup();
        gpuImageFilter.addFilter(new GPUImageExtTexFilter());
        //用户添加的滤镜
        if(filter instanceof GPUImageFilterGroup){
            //分开加入gpuImageFilter的好处在于：便于之后对每个滤镜做额外的处理，比如入场动画
            for(GPUImageFilter imageFilter : ((GPUImageFilterGroup)filter).getFilters()){
                gpuImageFilter.addFilter(imageFilter);
            }
        }else{
            gpuImageFilter.addFilter(filter);
        }

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        gpuImageFilter.init();
        GLES20.glUseProgram(gpuImageFilter.getProgram());
        // width&height not changed by filter
        gpuImageFilter.onOutputSizeChanged(w, h);

        requestRender();
    }

    public void setRotation(final Rotation rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }

    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        mFlipHorizontal = flipHorizontal;
        mFlipVertical = flipVertical;
        setRotation(rotation);
    }

    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
    }

    public boolean isFlippedHorizontally() {
        return mFlipHorizontal;
    }

    public boolean isFlippedVertically() {
        return mFlipVertical;
    }

    protected void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    private void doAfterSurfaceCreated(final Runnable runnable) {
        if (surfaceIsValid) {
            runnable.run();
        } else {
            synchronized (mRunAfterSurfaceCreated) {
                mRunAfterSurfaceCreated.add(runnable);
            }
        }
    }

    public void setSurfaceSize(final int width, final int height){
        if(width == mSurfaceWidth && height == mSurfaceHeight){
            return;
        }
        if(surfaceIsValid){
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            adapterPreviewSize();
            changeGpuImageFilter(mCurrentGPUImageFilter);
            requestLayout();
        }else{
            doAfterSurfaceCreated(new Runnable() {
                @Override
                public void run() {
                    mSurfaceWidth = width;
                    mSurfaceHeight = height;
                }
            });
        }
    }

    public int getSurfaceWidth() {
        return mSurfaceWidth;
    }

    public int getSurfaceHeight() {
        return mSurfaceHeight;
    }

    protected abstract void onSurfaceInit();

    protected abstract int getImageWidth();

    protected abstract int getImageHeight();
}
