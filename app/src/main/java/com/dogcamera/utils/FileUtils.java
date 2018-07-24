package com.dogcamera.utils;

import android.text.TextUtils;

import java.io.File;
import java.util.List;

public class FileUtils {

    public static boolean deleteFile(String filePath){
        if(!TextUtils.isEmpty(filePath)){
            File f = new File(filePath);
            try {
                f.delete();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static void deleteFiles(List<String> filePaths){
        if(filePaths == null || filePaths.size() == 0){
            return;
        }
        for(String filePath : filePaths){
            deleteFile(filePath);
        }
    }
}
