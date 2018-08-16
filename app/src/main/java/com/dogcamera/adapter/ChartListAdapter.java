package com.dogcamera.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.dogcamera.R;
import com.dogcamera.base.BaseChartListAdapter;
import com.dogcamera.module.ChartBean;
import com.dogcamera.utils.ViewUtils;

import java.util.List;

public class ChartListAdapter extends BaseChartListAdapter<ChartListAdapter.ChartViewHolder, ChartBean> {

    public ChartListAdapter(List<ChartBean> list) {
        super(list);
    }

    @Override
    protected ChartViewHolder createHolder(ViewGroup parent, int viewType) {
        FrameLayout rootView = new FrameLayout(parent.getContext());
        rootView.setPadding(ViewUtils.dip2px(parent.getContext(), 10), ViewUtils.dip2px(parent.getContext(), 10), ViewUtils.dip2px(parent.getContext(), 10), ViewUtils.dip2px(parent.getContext(), 10));
        RecyclerView.LayoutParams rootLp = new RecyclerView.LayoutParams(ViewUtils.dip2px(parent.getContext(), 72.5f), ViewUtils.dip2px(parent.getContext(), 60));
        rootView.setLayoutParams(rootLp);
        ImageView image = new ImageView(parent.getContext());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams imageLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        rootView.addView(image, imageLp);
        //点击选中框
        rootView.setBackgroundResource(R.drawable.preview_chart_recycler_item_bg);
        return new ChartViewHolder(rootView);
    }

    @Override
    protected void bindHolder(ChartViewHolder holder, ChartBean item) {
        holder.mImage.setImageResource(item.imgRes);
    }

    public static class ChartViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImage;

        public ChartViewHolder(View itemView) {
            super(itemView);
            mImage = (ImageView) ((FrameLayout) itemView).getChildAt(0);
        }
    }


}
