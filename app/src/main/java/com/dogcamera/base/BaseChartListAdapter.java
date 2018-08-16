package com.dogcamera.base;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;


public abstract class BaseChartListAdapter<K extends RecyclerView.ViewHolder, T> extends BaseRecyclerViewAdapter<K> {

    private List<T> mList;

    private View mLastSelectView;

    private int mSelectPosition = -1;

    public BaseChartListAdapter(List<T> list) {
        mList = list;
    }

    public void setSelectPosition(int position) {
        if (position != mSelectPosition) {
            mSelectPosition = position;
            notifyItemChanged(position);
        }
    }

    public void clearSelectPosition() {
        mSelectPosition = -1;
    }

    public List<T> getData() {
        return mList;
    }

    public void setData(List<T> data, boolean notify) {
        mList = data;
        if (notify)
            notifyDataSetChanged();
    }

    public void clearSelectView() {
        if (mLastSelectView != null) {
            mLastSelectView.setSelected(false);
            mLastSelectView = null;
        }
    }
    @Override
    public int getItemCount() {
        return mList.size();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public K onCreateViewHolder(ViewGroup parent, int viewType) {
        K viewHolder = createHolder(parent, viewType);
        //设置点击监听
        viewHolder.itemView.setOnClickListener(v -> {
            if (mLastSelectView != null) {
                if (mLastSelectView == viewHolder.itemView)
                    return;
                mLastSelectView.setSelected(false);
            }
            viewHolder.itemView.setSelected(true);
            mLastSelectView = viewHolder.itemView;
            mSelectPosition = viewHolder.getAdapterPosition();
            BaseChartListAdapter.this.onItemHolderClick(viewHolder);
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(K holder, int position) {
        if (mList == null || mList.size() == 0)
            return;
        T item = mList.get(position);
        bindHolder(holder, item);
        //处理从别的tab返回这个tab后将之前选中的itemView重新设置为选中状态
        if (position == mSelectPosition) {
            holder.itemView.setSelected(true);
            mLastSelectView = holder.itemView;
        } else {
            holder.itemView.setSelected(false);
        }
    }

    protected abstract K createHolder(ViewGroup parent, int viewType);

    protected abstract void bindHolder(K holder, T item);

}
