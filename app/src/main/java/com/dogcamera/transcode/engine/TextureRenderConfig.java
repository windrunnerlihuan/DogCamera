package com.dogcamera.transcode.engine;

public class TextureRenderConfig {
    public int outputVideoWidth;                    //输出视频的宽
    public int outputVideoHeight;                   //输出视频的高
    public String filterId;

    private TextureRenderConfig(){

    }

    public static class Builder {
        private int outputVideoWidth;
        private int outputVideoHeight;
        private String filterId;

        public Builder(){

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

        public TextureRenderConfig build(){
            TextureRenderConfig config = new TextureRenderConfig();
            config.outputVideoWidth = this.outputVideoWidth;
            config.outputVideoHeight = this.outputVideoHeight;
            config.filterId = this.filterId;
            return config;
        }
    }
}
