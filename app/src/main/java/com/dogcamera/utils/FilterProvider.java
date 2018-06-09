package com.dogcamera.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dogcamera.filter.GPUImageAntiqueFilter;
import com.dogcamera.filter.GPUImageBeautyFilter;
import com.dogcamera.filter.GPUImageExtTexFilter;
import com.dogcamera.filter.GPUImageFilter;
import com.dogcamera.filter.GPUImageLookupFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Deprecated
public class FilterProvider {

    private final ArrayList<FilterDes> mFilters = new ArrayList<>();

    public FilterProvider() {
        createFilterDes();
    }

    public ArrayList<FilterDes> getFilters() {

        return mFilters;
    }

    private void createFilterDes(){
        mFilters.add(new FilterDes.Builder().setFilterId("OR").setFilterName("原片").builder());
        mFilters.add(new FilterDes.Builder().setFilterId("F1").setFilterName("咖啡").builder());
        mFilters.add(new FilterDes.Builder().setFilterId("F2").setFilterName("新鲜").builder());
        mFilters.add(new FilterDes.Builder().setFilterId("A1").setFilterName("耀光").builder());
        mFilters.add(new FilterDes.Builder().setFilterId("BF").setFilterName("美丽").builder());
    }

    private FilterDes findFilterDesById(String id){
        for(FilterDes des : mFilters){
            if(id.equalsIgnoreCase(des.getFilterId())){
                return des;
            }
        }
        return null;
    }

    public GPUImageFilter createFilter(Context context, String id){
        switch (id){
            case "OR":
                break;
            case "F1":
                GPUImageLookupFilter filterF3 = new GPUImageLookupFilter();
                FilterDes desF3 = findFilterDesById("F1");
                Bitmap bmF3 = desF3.getFilterBitmap(context, "filters/kafei_lut.png");
                filterF3.setBitmap(bmF3);
                return filterF3;
            case "F2":
                GPUImageLookupFilter filterF5 = new GPUImageLookupFilter();
                FilterDes desF5 = findFilterDesById("F2");
                Bitmap bmF5 = desF5.getFilterBitmap(context, "filters/xinxian_lut.png");
                filterF5.setBitmap(bmF5);
                return filterF5;
            case "A1":
                return new GPUImageAntiqueFilter();
            case "BF":
                return new GPUImageBeautyFilter();
        }
        return new GPUImageFilter();
    }

    public static class FilterDes {

        private String filterId;

        private String filterName;

        private Bitmap bitmap;

        private FilterDes(){
        }

        public String getFilterId(){
            return filterId;
        }

        public String getFilterName(){
            return filterName;
        }

        public Bitmap getFilterBitmap(Context context, String uri){
            if(bitmap == null || bitmap.isRecycled()){
                InputStream is = null;
                try {
                    is = context.getResources().getAssets().open(uri);
                    bitmap = BitmapFactory.decodeStream(is);

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
            }
            return bitmap;
        }

        public static class Builder {

            private String filterId;

            private String filterName;

            public Builder(){

            }

            public Builder setFilterId(String id){
                this.filterId = id;
                return this;
            }

            public Builder setFilterName(String name){
                this.filterName = name;
                return this;
            }

            public FilterDes builder(){
                FilterDes filterDes = new FilterDes();
                filterDes.filterId = this.filterId;
                filterDes.filterName = this.filterName;
                return filterDes;
            }
        }

    }

}

