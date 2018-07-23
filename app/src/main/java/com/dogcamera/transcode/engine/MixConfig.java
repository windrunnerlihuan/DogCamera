package com.dogcamera.transcode.engine;

public interface MixConfig {

    public static final int MIX_NONE = 0;
    public static final int MIX_ORIGIN_ONLY = 1;
    public static final int MIX_MUSIC_ONLY = 2;
    public static final int MASK_FOR_MIX = 3;

    void setRenderConfig(RenderConfig config);

    void processMixFlags();
}
