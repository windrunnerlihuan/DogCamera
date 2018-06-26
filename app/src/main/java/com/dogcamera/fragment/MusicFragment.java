package com.dogcamera.fragment;

import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.dogcamera.R;
import com.dogcamera.activity.PreviewActivity;
import com.dogcamera.adapter.MusicListAdapter;
import com.dogcamera.base.BaseFragment;
import com.dogcamera.base.BaseRecyclerViewAdapter;
import com.dogcamera.module.PreviewRestartParams;
import com.dogcamera.utils.AudioProvider;
import com.dogcamera.utils.AudioUtils;
import com.dogcamera.utils.DogConstants;
import com.dogcamera.widget.AudioItemView;

import java.util.List;

import butterknife.BindView;

public class MusicFragment extends BaseFragment implements PreviewRestartParams.PreviewRestartListener {

    @BindView(R.id.preview_bottom_music_origin)
    AudioItemView mMusicOrginView;

    @BindView(R.id.preview_bottom_music_list)
    RecyclerView mMusicRecyclerView;

    private AudioProvider mAudioProvider;
    private List<AudioUtils.AudioItem> mAudioItems;
    private SimpleArrayMap<Integer, Object> mRetPropSet;

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
        initValues();
        mMusicOrginView.setImg(R.mipmap.preview_music_origin_off, R.mipmap.preview_music_origin_on);
        mMusicOrginView.setTv("原声OFF", "原声ON");
        mMusicOrginView.setSelected(true);
        mMusicOrginView.setOnItemClickListener(afteSselected -> sendToRestart(new PreviewRestartParams.Builder()
                .setIsMute(!afteSselected)
                .build()));
        createRecycler();

    }

    private void initValues() {
        mAudioProvider = new AudioProvider();
        mAudioItems = AudioUtils.createAudioItems();
        mRetPropSet = new SimpleArrayMap<>();
    }

    private void createRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mMusicRecyclerView.setLayoutManager(layoutManager);
        mMusicRecyclerView.addItemDecoration(new BaseRecyclerViewAdapter.DividerGridItemDecoration(
                getResources().getDrawable(R.drawable.preview_music_recycler_item_divider)));
        //setData
        MusicListAdapter adapter = new MusicListAdapter(mAudioItems);
        adapter.setOnItemClickListener((parent, view, position, id) -> {
            //TODO onItemClick
            String path = mAudioItems.get(position).path;
            if("XXX".equalsIgnoreCase(path)){
                Toast.makeText(getContext(), "主人很懒，暂时不想开发新功能～。～", Toast.LENGTH_SHORT).show();
                mAudioProvider.startPlay(null);
                mRetPropSet.put(DogConstants.PREVIEW_KEY_MUSIC, null);
            }else{
                mAudioProvider.startPlay(path);
                mRetPropSet.put(DogConstants.PREVIEW_KEY_MUSIC, path);
            }
            sendToRestart(null);
        });
        mMusicRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onPreviewRestart() {
        mAudioProvider.restartPlay();
    }

    @Override
    public void onPreviewStop() {
        mAudioProvider.stopPlay();
    }

    @Override
    public SimpleArrayMap<Integer, Object> onPreviewGetPropSet() {
        return mRetPropSet;
    }

    private void sendToRestart(PreviewRestartParams params){
        if(getActivity() != null && getActivity() instanceof PreviewActivity){
            PreviewActivity activity = (PreviewActivity) getActivity();
            activity.restartPlay(params);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAudioProvider != null){
            mAudioProvider.exitPlay();
        }
    }
}
