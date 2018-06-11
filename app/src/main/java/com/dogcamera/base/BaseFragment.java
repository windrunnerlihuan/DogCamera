package com.dogcamera.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dogcamera.DogApplication;
import com.squareup.leakcanary.RefWatcher;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by huanli on 2017/11/24.
 *
 * 懒加载的Fragment，适用于ViewPager重存放Fragment<br/>
 * 继承之后必须有如下配置：<br/>
 * private boolean mIsPrepared;<br/>
 * <br/>
 * public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {<br/>
 * //TODO initViews <br/>
 * mIsPrepared = true;<br/>
 * lazyLoad();<br/>
 * return root;<br/>
 * }<br/>
 * <br/>
 * protected void lazyLoad() {<br/>
 * if (!mIsPrepared || !mIsVisible) {<br/>
 * return;<br/>
 * }<br/>
 * //TODO implements bussiness<br/>
 * }<br/>
 *<br/>
 * 普通Fragment无效，不用上面配置
 */
public abstract class BaseFragment extends Fragment{

    protected boolean mIsVisible;
    protected final CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private Unbinder bind;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getUserVisibleHint()) {
            mIsVisible = true;
            onVisible();
        } else {
            mIsVisible = false;
            onInvisible();
        }
    }

    protected void onVisible() {
        lazyLoad();
    }

    protected abstract void lazyLoad();

    protected void onInvisible() {

    }

    protected abstract int getLayoutId();

    protected abstract void initViews(Bundle savedInstanceState);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(getLayoutId(), container, false);
        bind = ButterKnife.bind(this, root);
        initViews(savedInstanceState);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RefWatcher refWatcher = DogApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
        bind.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }

    protected <T extends View> T f(View view, int resId) {
        return (T) view.findViewById(resId);
    }
}
