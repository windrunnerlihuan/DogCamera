package com.dogcamera.utils;

import com.dogcamera.R;

import java.util.ArrayList;
import java.util.List;

public class AudioUtils {

    public static List<AudioItem> createAudioItems(){
        return new ArrayList<AudioItem>(){
            {
                add(new AudioItem("无配乐", "无配乐",
                        R.mipmap.preview_music_none_off, R.mipmap.preview_music_none_on,
                        null));
                add(new AudioItem("欢乐", "欢乐",
                        R.mipmap.preview_music_happy_off, R.mipmap.preview_music_happy_on,
                        "music/happy.m4a"));
                add(new AudioItem("活力", "活力",
                        R.mipmap.preview_music_energy_off, R.mipmap.preview_music_energy_on,
                        "music/energy.m4a"));
                add(new AudioItem("时尚", "时尚",
                        R.mipmap.preview_music_fantisy_off, R.mipmap.preview_music_fantisy_on,
                        "music/fantisy.m4a"));
                add(new AudioItem("悲伤", "悲伤",
                        R.mipmap.preview_music_sad_off, R.mipmap.preview_music_sad_on,
                        "XXX"));
                add(new AudioItem("搞笑", "搞笑",
                        R.mipmap.preview_music_comedy_off, R.mipmap.preview_music_comedy_on,
                        "XXX"));
                add(new AudioItem("爱情", "爱情",
                        R.mipmap.preview_music_love_off, R.mipmap.preview_music_love_on,
                        "XXX"));
            }
        };
    }

    public static class AudioItem {
        public String usStr;
        public String sStr;
        public int usImg;
        public int sImg;
        public String path;

        AudioItem(String usStr, String sStr, int usImg, int sImg, String path){
            this.usStr = usStr;
            this.sStr = sStr;
            this.usImg = usImg;
            this.sImg = sImg;
            this.path = path;
        }
    }

}
