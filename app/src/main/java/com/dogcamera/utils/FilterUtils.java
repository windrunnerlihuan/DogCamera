package com.dogcamera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dogcamera.DogApplication;
import com.dogcamera.filter.GPUImageAntiqueFilter;
import com.dogcamera.filter.GPUImageBeautyFilter;
import com.dogcamera.filter.GPUImageColorInvertFilter;
import com.dogcamera.filter.GPUImageFilter;
import com.dogcamera.filter.GPUImageLookupFilter;

import java.io.IOException;
import java.io.InputStream;

public class FilterUtils {

    public static GPUImageFilter createFilter(String id) {
        switch (id) {
            case "OR":
                return new GPUImageFilter();
            case "F1":
                GPUImageLookupFilter filterF3 = new GPUImageLookupFilter();
                Bitmap bmF3 = getFilterBitmap("filters/kafei_lut.png");
                filterF3.setBitmap(bmF3);
                return filterF3;
            case "F2":
                GPUImageLookupFilter filterF5 = new GPUImageLookupFilter();
                Bitmap bmF5 = getFilterBitmap("filters/xinxian_lut.png");
                filterF5.setBitmap(bmF5);
                return filterF5;
            case "A1":
                return new GPUImageAntiqueFilter();
            case "BF":
                return new GPUImageBeautyFilter();
            case "IV":
                return new GPUImageColorInvertFilter();

        }
        return null;
    }

    public static Bitmap getFilterBitmap(String uri) {
        InputStream is = null;
        try {
            is = DogApplication.getInstance().getResources().getAssets().open(uri);
        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return BitmapFactory.decodeStream(is);
    }

}
