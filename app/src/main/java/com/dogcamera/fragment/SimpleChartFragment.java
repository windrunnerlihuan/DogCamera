package com.dogcamera.fragment;

import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.RecyclerView;
import com.dogcamera.adapter.ChartListAdapter;
import com.dogcamera.base.BaseChartFragment;
import com.dogcamera.base.BaseChartListAdapter;
import com.dogcamera.module.ChartBean;
import com.dogcamera.utils.ChartConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 单页数据示例
 */
public class SimpleChartFragment extends BaseChartFragment<ChartBean> {

    //保存上一个Adapter
    private ChartListAdapter mLastAdapter;

    /**
     * 如果不设tab就返回null
     *
     * @return
     */
    @Override
    protected Object[][] fillTabUIRes() {
        return null;
    }

    @Override
    protected BaseChartListAdapter createChartListAdapter(List<ChartBean> list) {
        return new ChartListAdapter(list);
    }

    private ChartBean[] testDatas() {
        int[] chartRes = ChartConstants.CHART_PIC;
        ChartBean[] ret = new ChartBean[chartRes.length];
        for (int i = 0; i < chartRes.length; i++) {
            ChartBean b = new ChartBean();
            b.imgRes = chartRes[i];
            ret[i] = b;
        }
        return ret;
    }

    /**
     * 数据设置成一维数组
     *
     * @return
     */
    @Override
    protected SimpleArrayMap<Integer, List<List<ChartBean>>> generateChartData() {
        SimpleArrayMap<Integer, List<List<ChartBean>>> ret = new SimpleArrayMap<>();
        ChartBean[] srcCharts = testDatas();
        List<List<ChartBean>> dstChartList = new ArrayList<>();
        int pageNum = (int) Math.ceil(srcCharts.length / 8.0f);
        for (int i = 0; i < pageNum; i++) {
            List<ChartBean> picPageData = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(srcCharts, i * 8, (srcCharts.length - i * 8) / 8 > 0 ? (i + 1) * 8 : i * 8 + srcCharts.length % 8)));
            dstChartList.add(picPageData);
        }
        ret.put(0, dstChartList);
        return ret;
    }

    @Override
    protected void onChartSelect(ChartBean item) {
        //TODO 参考ChartFragment
    }

    @Override
    protected void onChartClear() {
        //TODO 参考ChartFragment
    }

    /*----------下面两个方法也可以不重写，不影响单页数据的逻辑，重写是为了看起来逻辑更清晰------------*/

    @Override
    protected void bindAdapters(final List<RecyclerView> pagerViews, List<List<ChartBean>> dstList) {
        //以防万一，判断一下
        if (pagerViews.size() != dstList.size())
            throw new IndexOutOfBoundsException("pagerViews's size is not equals of dstList's size.");
        //重新给adapter填充数据
        for (int i = 0, size = dstList.size(); i < size; i++) {
            final ChartListAdapter listAdapter = new ChartListAdapter(dstList.get(i));
            listAdapter.setOnItemClickListener((parent, view, position, id) -> {
                if (mLastAdapter != null && mLastAdapter != listAdapter) {
                    mLastAdapter.clearSelectView();
                }
                mLastAdapter = listAdapter;
                //change choosed poi
                changeSelectChart(position);
            });
            pagerViews.get(i).setAdapter(listAdapter);
        }
    }

    @Override
    public void clearChartSelectedState() {
        if(mLastAdapter != null){
            mLastAdapter.clearSelectView();
            mLastAdapter.clearSelectPosition();
            mLastAdapter = null;
        }
        onChartClear();
    }

}
