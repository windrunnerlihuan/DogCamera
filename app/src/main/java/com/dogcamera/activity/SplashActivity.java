package com.dogcamera.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.dogcamera.R;
import com.dogcamera.base.BaseActivity;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by huanli on 2018/2/27.
 */

public class SplashActivity extends BaseActivity {

    private void setUpSplash() {
        Subscription splash = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                   startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("dog://camera")));
                   SplashActivity.this.finish();
                });
        mCompositeSubscription.add(splash);
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        setUpSplash();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }
}
