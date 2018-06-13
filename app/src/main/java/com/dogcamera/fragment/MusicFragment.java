package com.dogcamera.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.dogcamera.R;
import com.dogcamera.adapter.MusicListAdapter;
import com.dogcamera.base.BaseFragment;
import com.dogcamera.utils.AudioProvider;
import com.dogcamera.widget.AudioItemView;

import butterknife.BindView;

public class MusicFragment extends BaseFragment {

    @BindView(R.id.preview_bottom_music_origin)
    AudioItemView mMusicOrginView;

    @BindView(R.id.preview_bottom_music_list)
    RecyclerView mMusicRecyclerView;

    @Override
    protected void lazyLoad() {
        throw new IllegalStateException("不是懒加载的Fragment不能调用这个方法");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_preview_music;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mMusicOrginView.setImg(R.mipmap.preview_bottom_origin_off, R.mipmap.preview_bottom_origin_on);
        mMusicOrginView.setTv("原声OFF", "原声ON");
        mMusicOrginView.setSelected(true);
        createRecycler();

    }

    private void createRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mMusicRecyclerView.setLayoutManager(layoutManager);
        //setData
        MusicListAdapter adapter = new MusicListAdapter(AudioProvider.createAudioItems());
        adapter.setOnItemClickListener((parent, view, position, id) -> {
            //TODO onItemClick

        });
        mMusicRecyclerView.setAdapter(adapter);
    }
}
