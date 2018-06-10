package com.dogcamera.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.dogcamera.R;
import com.dogcamera.utils.FilterProvider;
import com.dogcamera.utils.ViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VideoFilterSwitcher extends FrameLayout implements ViewPager.OnPageChangeListener {

    private static final String TAG = VideoFilterSwitcher.class.getSimpleName();

    private TextView mFilterHintView;
    private ViewPager mFilterDetailView;
    private ImageView mToggleView;

    private FilterDetailAdapter mFilterDetailAdapter;

    private OnFilterChangedListener mOnFilterChangedListener;

    private static final int ITEM_WIDTH = 40;// unit dp
    private static final int ITEM_HEIGHT = 16;// unit dp
    private static final int MAX_ITEM_COUNT = 5;

    private int mFocusedFilterIndex = 0;
    private int mFilterIndex = 0;
    private boolean mIsDetailShown = true;

    public VideoFilterSwitcher(@NonNull Context context) {
        this(context, null);
    }

    public VideoFilterSwitcher(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    private void initViews() {
        FrameLayout toggleLayout = new FrameLayout(getContext());
        FrameLayout.LayoutParams toggleLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleLayoutParams.topMargin = ViewUtils.dip2px(getContext(), 20);
        toggleLayoutParams.bottomMargin = ViewUtils.dip2px(getContext(), 20);
        toggleLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(toggleLayout, toggleLayoutParams);


        mToggleView = new ImageView(getContext());
        mToggleView.setImageResource(R.mipmap.record_filter_icon);
        mToggleView.setOnClickListener((v) -> showDetail(!mIsDetailShown));

        FrameLayout.LayoutParams toggleViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleViewParams.gravity = Gravity.CENTER;
        toggleLayout.addView(mToggleView, toggleViewParams);


        mFilterHintView = new TextView(getContext());
        mFilterHintView.setTypeface(null, Typeface.BOLD);
        mFilterHintView.setTextSize(10);
        mFilterHintView.setTextColor(getContext().getResources().getColor(android.R.color.white));
        mFilterHintView.setShadowLayer(ViewUtils.dip2px(getContext(), 2), ViewUtils.dip2px(getContext(), 1),
                ViewUtils.dip2px(getContext(), 1), Color.parseColor("#80000000"));
        FrameLayout.LayoutParams filterHintViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        filterHintViewParams.gravity = Gravity.CENTER;
        toggleLayout.addView(mFilterHintView, filterHintViewParams);


        mFilterDetailView = new ViewPager(getContext());
        FrameLayout.LayoutParams filterDetailViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewUtils.dip2px(getContext(), 50));
        filterDetailViewParams.topMargin = ViewUtils.dip2px(getContext(), 50);
        filterDetailViewParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(mFilterDetailView, filterDetailViewParams);
        mFilterDetailView.setOnPageChangeListener(this);

        mFilterDetailAdapter = new FilterDetailAdapter();
        mFilterDetailView.setAdapter(mFilterDetailAdapter);

        showDetail(false);
    }

    public void showDetail(boolean shown) {

        if (this.mIsDetailShown == shown) {
            return;
        }

        this.mIsDetailShown = shown;

        if (this.mIsDetailShown) {
            setBackgroundColor(Color.parseColor("#B4000000"));

            mFilterDetailView.setVisibility(View.VISIBLE);


        } else {
            setBackgroundColor(Color.parseColor("#00000000"));

            mFilterDetailView.setVisibility(View.GONE);


        }

    }

    public void setFilters(List<FilterProvider.FilterDes> filters){
        if(filters == null || filters.size() == 0){
            return;
        }
        int showItemCount = (filters.size() & 1) == 0 ? Math.min(MAX_ITEM_COUNT, filters.size() - 1) : Math.min(MAX_ITEM_COUNT, filters.size());

        FrameLayout.LayoutParams filterDetailViewParams = (FrameLayout.LayoutParams) mFilterDetailView.getLayoutParams();
        filterDetailViewParams.width = showItemCount * ViewUtils.dip2px(getContext(), ITEM_WIDTH);
        mFilterDetailView.setLayoutParams(filterDetailViewParams);

        mFilterDetailAdapter.setFilterInfos(filters, showItemCount);
        mFilterIndex = mFilterDetailAdapter.getCount() / 2 - showItemCount / 2;//select OR
        mFocusedFilterIndex = mFilterIndex + showItemCount / 2;
        mFilterDetailView.setCurrentItem(mFilterIndex);

        String id = mFilterDetailAdapter.getFilterInfo(mFocusedFilterIndex).getFilterId();
        mFilterHintView.setText(id);
    }

    public void flingPageOffset(int offset){
        mFilterDetailView.setCurrentItem(mFilterIndex + offset);
    }

    public interface OnFilterChangedListener {
        void onFilterChanged(FilterProvider.FilterDes filterModel, FilterProvider.FilterDes lFilterModel, FilterProvider.FilterDes rFilterModel);
    }

    public void setOnFilterChangedListener(OnFilterChangedListener listener) {
        this.mOnFilterChangedListener = listener;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mFocusedFilterIndex = mFocusedFilterIndex + position - mFilterIndex;
        mFilterIndex = position;

        String id = mFilterDetailAdapter.getFilterInfo(mFocusedFilterIndex).getFilterId();
        mFilterHintView.setText(id);
        Log.e(TAG, "onPageSelected position=" + position + " id=" + id);

        if (mOnFilterChangedListener != null) {
            mOnFilterChangedListener.onFilterChanged(mFilterDetailAdapter.getFilterInfo(mFocusedFilterIndex),
                    mFilterDetailAdapter.getFilterInfo(mFocusedFilterIndex - 1),
                    mFilterDetailAdapter.getFilterInfo(mFocusedFilterIndex + 1));

        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {

            // disable focused filter
            mFilterDetailAdapter.unSetFocusedView();

        } else if (state == ViewPager.SCROLL_STATE_IDLE) {

            mFilterDetailAdapter.setFocused(mFocusedFilterIndex);

        }
    }

    private class FilterDetailAdapter extends PagerAdapter implements View.OnClickListener {

        private int mShowCount = 1;
        private List<FilterProvider.FilterDes> mFilters = new ArrayList<>();

        private final HashMap<Integer, TextView> mCacheViews = new HashMap<Integer, TextView>();
        private TextView mLastFocusedView = null;

        private static final int MaxLoop = 10000;//default

        public void setFilterInfos(List<FilterProvider.FilterDes> filters, int showCount){
            mShowCount = showCount;
            mFilters.clear();
            mFilters = filters;
            notifyDataSetChanged();
        }

        public int getFilterCount() {
            return mFilters.size();
        }


        public FilterProvider.FilterDes getFilterInfo(int filterIndex){
            return mFilters.get(filterIndex % mFilters.size());
        }

        public HashMap<Integer, TextView> getCacheViews() {
            return mCacheViews;
        }

        public void setFocused(int focusedIndex) {

            //reset all cached views
            Set<Map.Entry<Integer, TextView>> filterViewSet = mCacheViews.entrySet();
            Iterator<Map.Entry<Integer, TextView>> filterViewIterator = filterViewSet.iterator();
            while (filterViewIterator.hasNext()) {

                Map.Entry<Integer, TextView> entry = filterViewIterator.next();

                entry.getValue().setTextColor(Color.parseColor("#66FFFFFF"));
            }

            if(mLastFocusedView != null){
                mLastFocusedView.setTextColor(Color.parseColor("#66FFFFFF"));
            }

            // focus the right one
            TextView focusedFilterView = mCacheViews.get(focusedIndex);
            if (focusedFilterView != null) {
                focusedFilterView.setTextColor(Color.parseColor("#FFFFFFFF"));

                mLastFocusedView = focusedFilterView;
            }

        }

        public void unSetFocusedView() {
            if (mLastFocusedView != null) {
                mLastFocusedView.setTextColor(Color.parseColor("#66FFFFFF"));
                mLastFocusedView = null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return mFilters.size() * MaxLoop;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public float getPageWidth(int position) {
            return 1.0f / mShowCount;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            mCacheViews.remove(position);
            if (mLastFocusedView == object) {
                mLastFocusedView = null;
            }

            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            TextView view = new TextView(container.getContext());
            view.setTag(position);
            view.setOnClickListener(this);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewUtils.dip2px(container.getContext(), ITEM_WIDTH), ViewPager.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);

            String id = mFilters.get(position % mFilters.size()).getFilterId();
            view.setGravity(Gravity.CENTER);
            view.setText(id);
            view.setTypeface(null, Typeface.BOLD);
            view.setTextSize(13);

            if (mFocusedFilterIndex == position) {
                mLastFocusedView = view;
                view.setTextColor(Color.parseColor("#FFFFFFFF"));
            } else {
                view.setTextColor(Color.parseColor("#66FFFFFF"));
            }

            container.addView(view, 0);

            mCacheViews.put(position, view);

            return view;
        }

        @Override
        public void onClick(View v) {
            Object tag = v.getTag();
            if (tag instanceof Integer) {
                int position = (Integer) tag;

                int newPosition = mFilterIndex + position - mFocusedFilterIndex;

                unSetFocusedView();

                setFocused(position);

                mFilterDetailView.setCurrentItem(newPosition);

            }
        }
    }

}
