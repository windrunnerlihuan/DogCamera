package com.dogcamera.fragment;

import android.os.Bundle;

import com.dogcamera.R;
import com.dogcamera.base.BaseFragment;

public class MusicFragment extends BaseFragment {

    @Override
    protected void lazyLoad() {
        throw new IllegalStateException("不是懒加载的Activity不能调用这个方法");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_preview_music;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

    }
}
