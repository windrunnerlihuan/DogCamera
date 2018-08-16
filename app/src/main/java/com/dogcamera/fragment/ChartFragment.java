package com.dogcamera.fragment;

import android.support.v4.util.SimpleArrayMap;

import com.dogcamera.activity.PreviewActivity;
import com.dogcamera.adapter.ChartListAdapter;
import com.dogcamera.base.BaseChartFragment;
import com.dogcamera.base.BaseChartListAdapter;
import com.dogcamera.module.ChartBean;
import com.dogcamera.module.PreviewRestartParams;
import com.dogcamera.utils.ChartConstants;
import com.dogcamera.utils.DogConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ChartFragment extends BaseChartFragment<ChartBean> implements PreviewRestartParams.PreviewRestartListener {

    private SimpleArrayMap<Integer, Object> mRetPropSet = new SimpleArrayMap<>();

    @Override
    protected Object[][] fillTabUIRes() {
        Object[][] tabRes = new Object[3][2];
        String[] txts = ChartConstants.CHART_TAB_NAME;
        Integer[] imgNormalIds = ChartConstants.CHART_TAB_IMG_NORMAL;
        Integer[] imgSelectIds = ChartConstants.CHART_TAB_IMG_SELECT;
        tabRes[0] = txts;
        tabRes[1] = imgNormalIds;
        tabRes[2] = imgSelectIds;
        return tabRes;
    }

    @Override
    protected BaseChartListAdapter createChartListAdapter(List<ChartBean> list) {
        return new ChartListAdapter(list);
    }

    private ChartBean[][] testDatas(){
        int[][] chartRes = {ChartConstants.CHART_PIC, ChartConstants.CHART_TXT};
        ChartBean[][] ret = new ChartBean[chartRes.length][];
        for(int i = 0; i < chartRes.length; i++){
            ret[i] = new ChartBean[chartRes[i].length];
            for(int j = 0; j < chartRes[i].length; j++){
                ChartBean b = new ChartBean();
                b.imgRes = chartRes[i][j];
                ret[i][j] = b;
            }
        }
        return ret;
    }

    @Override
    protected SimpleArrayMap<Integer, List<List<ChartBean>>> generateChartData() {
        SimpleArrayMap<Integer, List<List<ChartBean>>> ret = new SimpleArrayMap<>();
        ChartBean[][] srcCharts = testDatas();
        for(int i = 0; i < srcCharts.length; i++){
            List<List<ChartBean>> dstChartList = new ArrayList<>();
            int pageNum = (int) Math.ceil(srcCharts[i].length / 8.0f);
            for (int j = 0; j < pageNum; j++) {
                List<ChartBean> picPageData = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(srcCharts[i], j * 8, (srcCharts[i].length - j * 8) / 8 > 0 ? (j + 1) * 8 : j * 8 + srcCharts[i].length % 8)));
                dstChartList.add(picPageData);
            }
            ret.put(i, dstChartList);
        }
        return ret;
    }

    @Override
    protected void onChartSelect(ChartBean item) {
        if (getActivity() != null && getActivity() instanceof PreviewActivity) {
            PreviewActivity activity = (PreviewActivity) getActivity();
            activity.addChart(item);
            mRetPropSet.put(DogConstants.PREVIEW_KEY_CHART, item != null ? item.imgRes : null);
        }
    }

    @Override
    protected void onChartClear() {
        onChartSelect(null);
    }


    @Override
    public SimpleArrayMap<Integer, Object> onPreviewGetPropSet() {
        return mRetPropSet;
    }

    @Override
    public void onPreviewRestart() {

    }

    @Override
    public void onPreviewStop() {

    }
}
