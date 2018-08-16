package com.dogcamera.module;

import android.support.v4.util.SimpleArrayMap;

public class PreviewRestartParams {

    public Boolean isNotify;

    public Boolean isMute;

    private PreviewRestartParams(){

    }

    public static class Builder{
        private Boolean isNotify;
        private Boolean isMute;

        public Builder setIsMute(boolean mute){
            isMute = mute;
            return this;
        }
        public Builder setIsNotify(boolean notify){
            isNotify = notify;
            return this;
        }

        public PreviewRestartParams build(){
            PreviewRestartParams p = new PreviewRestartParams();
            p.isNotify = this.isNotify;
            p.isMute = this.isMute;
            return p;
        }
    }


    public interface PreviewRestartListener {
        void onPreviewRestart();
        void onPreviewStop();
        SimpleArrayMap<Integer, Object> onPreviewGetPropSet();
    }


}
