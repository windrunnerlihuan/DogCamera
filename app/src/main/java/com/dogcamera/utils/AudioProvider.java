package com.dogcamera.utils;

import com.dogcamera.R;

import java.util.ArrayList;
import java.util.List;

public class AudioProvider {

    public static List<AudioItem> createAudioItems(){
        return new ArrayList<AudioItem>(){
            {
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
                add(new AudioItem("无配乐", "无配乐", R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on));
            }
        };
    }

    public static class AudioItem {
        public String usStr;
        public String sStr;
        public int usImg;
        public int sImg;

        AudioItem(String usStr, String sStr, int usImg, int sImg){
            this.usStr = usStr;
            this.sStr = sStr;
            this.usImg = usImg;
            this.sImg = sImg;
        }

    }

}
