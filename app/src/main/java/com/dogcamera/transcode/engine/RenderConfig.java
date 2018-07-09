package com.dogcamera.transcode.engine;

public class RenderConfig {
    //公共参数
    public long duration;
    //视频相关
    public int outputVideoWidth;                    //输出视频的宽
    public int outputVideoHeight;                   //输出视频的高
    public String filterId;
    //音频相关
    public String audioPath;
    public boolean originMute;

    private RenderConfig(){

    }

    public static class Builder {
        private long duration;
        private int outputVideoWidth;
        private int outputVideoHeight;
        private String filterId;
        private String audioPath;
        public boolean originMute;

        public Builder(){

        }

        public Builder setDuration(long duration){
            this.duration = duration;
            return this;
        }
        public Builder setOutputWidth(int width){
            this.outputVideoWidth = width;
            return this;
        }
        public Builder setOutputHeight(int height){
            this.outputVideoHeight = height;
            return this;
        }
        public Builder setFilterId(String filterId){
            this.filterId = filterId;
            return this;
        }
        public Builder setAudioPath(String audioPath){
            this.audioPath = audioPath;
            return this;
        }
        public Builder setOriginMute(boolean originMute){
            this.originMute = originMute;
            return this;
        }

        public RenderConfig build(){
            RenderConfig config = new RenderConfig();
            config.duration = this.duration;
            config.outputVideoWidth = this.outputVideoWidth;
            config.outputVideoHeight = this.outputVideoHeight;
            config.filterId = this.filterId;
            config.audioPath = this.audioPath;
            config.originMute = this.originMute;
            return config;
        }
    }
}
