package com.dogcamera.base;

import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dogcamera.R;
import com.dogcamera.adapter.ChartPagerAdapter;
import com.dogcamera.utils.ViewUtils;
import com.dogcamera.widget.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public abstract class BaseChartFragment<T> extends BaseFragment {

    @BindView(R.id.preview_bottom_chart_tab)
    LinearLayout mTabContainer;
    @BindView(R.id.preview_bottom_chart_viewpager)
    ViewPager mChartViewPager;
    @BindView(R.id.preview_bottom_chart_indicator)
    CirclePageIndicator mIndicator;

    //贴图数据
    protected SimpleArrayMap<Integer, List<List<T>>> mChartDatas;
    //保存的点击item
    protected final int[] mSelectedItem = new int[]{-1, -1, -1};
    //当前选中的tab
    protected int mCurrentTabIndex;

    @Override
    protected void lazyLoad() {
        throw new IllegalStateException("不是懒加载的Fragment不能调用这个方法");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_preview_chart;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        initValues();
        setupChartPages(mCurrentTabIndex);
        initTabs();
    }

    private void setupChartPages(int index) {
        List<List<T>> dstList = mChartDatas.get(index);
        final List<RecyclerView> pagers = new ArrayList<>();
        for (int i = 0, size = dstList.size(); i < size; i++) {
            RecyclerView pager = createRecyclerView();
            pagers.add(pager);
        }
        bindAdapters(pagers, dstList);
        ChartPagerAdapter pageAdapter = new ChartPagerAdapter(pagers);
        mChartViewPager.setAdapter(pageAdapter);
        if (dstList.size() > 1) {
            mIndicator.setVisibility(View.VISIBLE);
        } else {
            mIndicator.setVisibility(View.GONE);
        }
        mIndicator.setViewPager(mChartViewPager);
    }

    protected void initValues(){
        mChartDatas = generateChartData();
        mCurrentTabIndex = setDefaultTabIndex();
    }

    protected int setDefaultTabIndex(){
        return 0;
    }

    protected void initTabs() {
        if (fillTabUIRes() == null) {
            mTabContainer.setVisibility(View.GONE);
            return;
        }
        mTabContainer.setVisibility(View.VISIBLE);
        String[] txts = (String[]) fillTabUIRes()[0];
        final Integer[] imgNormalIds = (Integer[]) fillTabUIRes()[1];
        final Integer[] imgSelectIds = (Integer[]) fillTabUIRes()[2];
        for (int i = 0; i < imgNormalIds.length; i++) {
            //tab layout
            final LinearLayout tabLayout = new LinearLayout(getContext());
            tabLayout.setOrientation(LinearLayout.VERTICAL);
            tabLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            //img
            final ImageView tabImg = new ImageView(getContext());
            tabImg.setImageResource(mCurrentTabIndex == i ? imgSelectIds[i] : imgNormalIds[i]);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(ViewUtils.dip2px(getContext(), 25), ViewUtils.dip2px(getContext(), 25));
            tabLayout.addView(tabImg, imgLp);
            //text
            final TextView tv = new TextView(getContext());
            tv.setTextSize(10);
            tv.setTextColor(mCurrentTabIndex == i ? getResources().getColor(R.color.colorAccent) : getResources().getColor(R.color.light_gray));
            tv.setText(txts[i]);
            LinearLayout.LayoutParams txtLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            txtLp.setMargins(0, ViewUtils.dip2px(getContext(), 1.5f), 0, 0);
            tv.setLayoutParams(txtLp);
            tabLayout.addView(tv);

            final int finalI = i;
            tabLayout.setOnClickListener(v -> {
                int currentIndex = finalI;
                if (mCurrentTabIndex == currentIndex)
                    return;
                LinearLayout lastTab = (LinearLayout) mTabContainer.getChildAt(mCurrentTabIndex);
                ((ImageView) lastTab.getChildAt(0)).setImageResource(imgNormalIds[mCurrentTabIndex]);
                ((TextView) lastTab.getChildAt(1)).setTextColor(getResources().getColor(R.color.light_gray));
                tabImg.setImageResource(imgSelectIds[currentIndex]);
                tv.setTextColor(getResources().getColor(R.color.colorAccent));
                //switch tab
                mCurrentTabIndex = currentIndex;
                switchTab(currentIndex);
            });
            mTabContainer.addView(tabLayout, tabLp);

        }
    }

    protected void switchTab(final int index) {
        ChartPagerAdapter pagerAdapter = (ChartPagerAdapter) mChartViewPager.getAdapter();
        if (pagerAdapter == null) {
            return;
        }
        List<RecyclerView> pagerViews = pagerAdapter.getData();
        //dst data
        List<List<T>> dstList = mChartDatas.get(index);
        //扩容
        if (pagerViews.size() < dstList.size()) {
            int deltaNum = dstList.size() - pagerViews.size();
            for (int i = 0; i < deltaNum; i++) {
                RecyclerView pager = createRecyclerView();
                pagerViews.add(pager);
            }
        //缩小
        } else if (pagerViews.size() > dstList.size()) {
            int deltaNum = pagerViews.size() - dstList.size();
            for (int i = 0; i < deltaNum; i++) {
                RecyclerView r = pagerViews.remove(0);

            }
        }
        bindAdapters(pagerViews, dstList);
        //page数目大于1才显示indicator
        if (dstList.size() > 1)
            mIndicator.setVisibility(View.VISIBLE);
        else
            mIndicator.setVisibility(View.INVISIBLE);

        //刷新数据
        pagerAdapter.notifyDataSetChanged();

        if (mSelectedItem[0] == mCurrentTabIndex) {
            //之前tab页面，因为数据变化了，重新返回时候要选中之前选中的item
            mChartViewPager.setCurrentItem(mSelectedItem[1]);
            RecyclerView selectPage = pagerViews.get(mSelectedItem[1]);
            BaseChartListAdapter selectAdatper = (BaseChartListAdapter) selectPage.getAdapter();
            selectAdatper.setSelectPosition(mSelectedItem[2]);
        } else {
            //之前tab页没有选中任何item
            mChartViewPager.setCurrentItem(0);
        }

    }

    protected RecyclerView createRecyclerView() {
        RecyclerView pager = new RecyclerView(getContext());
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        pager.setLayoutManager(layoutManager);
        pager.addItemDecoration(new BaseRecyclerViewAdapter.DividerGridItemDecoration(getResources().getDrawable(R.drawable.preview_chart_recycler_item_divider)));
        ViewGroup.LayoutParams pagerLp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        pager.setLayoutParams(pagerLp);
        return pager;
    }

    protected void bindAdapters(final List<RecyclerView> pagerViews, List<List<T>> dstList) {
        //以防万一，判断一下
        if (pagerViews.size() != dstList.size())
            throw new IndexOutOfBoundsException("pagerViews's size is not equals of dstList's size.");
        //重新给adapter填充数据
        final BaseChartListAdapter[] lastAdapter = new BaseChartListAdapter[1];//保存上一个adapter
        for (int i = 0, size = dstList.size(); i < size; i++) {
            if (pagerViews.get(i).getAdapter() == null) {
                final BaseChartListAdapter listAdapter = createChartListAdapter(dstList.get(i));
                listAdapter.setOnItemClickListener((parent, view, position, id) -> {
                    //lastAdapter[0]是局部变量，switchTab时候会让其变为null，所以当点击其他page item时候，上次残留的select item不会clear
                    if (mSelectedItem[0] == mCurrentTabIndex && mSelectedItem[1] != mChartViewPager.getCurrentItem()) {
                        BaseChartListAdapter remainAdapter = ((BaseChartListAdapter) pagerViews.get(mSelectedItem[1]).getAdapter());
                        remainAdapter.clearSelectView();
                        remainAdapter.clearSelectPosition();
                    }
                    mSelectedItem[0] = mCurrentTabIndex;
                    mSelectedItem[1] = mChartViewPager.getCurrentItem();
                    mSelectedItem[2] = position;

                    if (lastAdapter[0] != null && lastAdapter[0] != listAdapter) {
                        lastAdapter[0].clearSelectView();
                    }
                    lastAdapter[0] = listAdapter;
                    //clear other fragment's chart selected state
                    //change choosed chart
                    changeSelectChart(position);
                });
                pagerViews.get(i).setAdapter(listAdapter);
            } else {
                BaseChartListAdapter adapter = (BaseChartListAdapter) pagerViews.get(i).getAdapter();
                adapter.clearSelectView();
                adapter.clearSelectPosition();
                adapter.setData(dstList.get(i), true);
            }
        }
    }

    protected void changeSelectChart(int position) {
        int tabIndex = mSelectedItem[0] != -1 ? mSelectedItem[0] : 0;
        int pageIndex = mSelectedItem[1] != -1 ? mSelectedItem[1] : mChartViewPager.getCurrentItem();

        T selectItem = mChartDatas.get(tabIndex).get(pageIndex).get(position);
        onChartSelect(selectItem);
    }

    protected void clearChartSelectedState(){
        if(mSelectedItem[2] == -1)
            return;
        if (mCurrentTabIndex == mSelectedItem[0]) {
            List<RecyclerView> pagerViews = ((ChartPagerAdapter) mChartViewPager.getAdapter()).getData();
            BaseChartListAdapter remainAdapter = (BaseChartListAdapter) pagerViews.get(mSelectedItem[1]).getAdapter();
            remainAdapter.clearSelectView();
            remainAdapter.clearSelectPosition();
        }
        mSelectedItem[0] = -1;
        mSelectedItem[1] = -1;
        mSelectedItem[2] = -1;
        onChartClear();
    }

    @OnClick(R.id.preview_bottom_chart_cancel_tv)
    void clearChart(){
        clearChartSelectedState();
    }

    protected abstract Object[][] fillTabUIRes();

    protected abstract BaseChartListAdapter createChartListAdapter(List<T> list);

    protected abstract SimpleArrayMap<Integer, List<List<T>>> generateChartData();

    protected abstract void onChartSelect(T item);

    protected abstract void onChartClear();


}
