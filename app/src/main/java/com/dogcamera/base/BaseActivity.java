package com.dogcamera.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.dogcamera.DogApplication;
import com.squareup.leakcanary.RefWatcher;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by huanli on 2018/2/27.
 */

public abstract class BaseActivity extends AppCompatActivity {

    public String TAG = getClass().getSimpleName();
    private Unbinder bind;

    protected CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        bind = ButterKnife.bind(this);
        initViews(savedInstanceState);
    }

    protected abstract void initViews(Bundle savedInstanceState);

    protected abstract int getLayoutId();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = DogApplication.getRefWatcher(this);
        refWatcher.watch(this);
        bind.unbind();
        mCompositeSubscription.unsubscribe();
    }


    protected  <T extends View> T f(int resId) {
        return (T) super.findViewById(resId);
    }
}
