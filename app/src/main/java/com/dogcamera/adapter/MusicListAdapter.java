package com.dogcamera.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.dogcamera.base.BaseRecyclerViewAdapter;
import com.dogcamera.utils.AudioUtils;
import com.dogcamera.widget.AudioItemView;

import java.util.List;

public class MusicListAdapter extends BaseRecyclerViewAdapter<MusicListAdapter.MLViewHolder> {

    private List<AudioUtils.AudioItem> mList;

    private int mLastPosition = 0;

    public MusicListAdapter(List<AudioUtils.AudioItem> list){
        mList = list;
    }

    @Override
    public MLViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AudioItemView rootView = new AudioItemView(parent.getContext());
        MLViewHolder holder = new MLViewHolder(rootView);
        holder.itemView.setOnClickListener(v -> {
            if(mLastPosition == holder.getAdapterPosition()){
                return;
            }
            int oldPosition = mLastPosition;
            mLastPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            int newPosition = holder.getAdapterPosition();
            notifyItemChanged(newPosition);

            MusicListAdapter.this.onItemHolderClick(holder);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(MLViewHolder holder, int position) {
        if(mList == null || mList.size() == 0){
            return;
        }
        AudioUtils.AudioItem item = mList.get(position);
        holder.mRoot.setImg(item.usImg, item.sImg);
        holder.mRoot.setTv(item.usStr, item.sStr);
        if(position == mLastPosition){
            holder.mRoot.setSelected(true);
        }else{
            holder.mRoot.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class MLViewHolder extends RecyclerView.ViewHolder {

        AudioItemView mRoot;

        public MLViewHolder(View itemView) {
            super(itemView);
            mRoot = (AudioItemView) itemView;
        }
    }
}
