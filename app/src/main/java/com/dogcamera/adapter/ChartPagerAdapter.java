package com.dogcamera.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class ChartPagerAdapter<T extends View> extends PagerAdapter {

    private int mChildCount = 0;

    private List<T> mPagerViews;

    public ChartPagerAdapter(List<T> mPagerViews) {
        this.mPagerViews = mPagerViews;
    }

    @Override
    public int getCount() {
        return mPagerViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mPagerViews.get(position));
        return mPagerViews.get(position);
    }

    public void setData(List<T> data, boolean notify){
        mPagerViews = data;
        if(notify)
            notifyDataSetChanged();
    }

    public List<T> getData(){
        return mPagerViews;
    }

    @Override
    public void notifyDataSetChanged() {
        mChildCount = getCount();
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object)   {
        if ( mChildCount > 0) {
            mChildCount --;
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }

}
