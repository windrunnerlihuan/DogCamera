package com.dogcamera.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.dogcamera.R;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.utils.PermissionUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by huanli on 2018/2/27.
 */

public class SplashActivity extends BaseActivity {

    private PermissionUtils.PermissionGrant mGrant = new PermissionUtils.PermissionGrant() {
        @Override
        public void onPermissionGranted(int requestCode) {
            gotoCameraPage();
        }

        @Override
        public void onPermissionCancel() {
            finish();
        }
    };

    private void setUpSplash() {
        Subscription splash = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    requestPermission();
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

    private void gotoCameraPage(){
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("dog://camera")));
        SplashActivity.this.finish();
    }

    private void requestPermission(){
        PermissionUtils.requestMultiPermissions(this,
                new String[]{
                PermissionUtils.PERMISSION_CAMERA,
                PermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE,
                PermissionUtils.PERMISSION_RECORD_AUDIO,
                PermissionUtils.PERMISSION_READ_EXTERNAL_STORAGE}, mGrant);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PermissionUtils.CODE_MULTI_PERMISSION){
            PermissionUtils.requestPermissionsResult(this, requestCode, permissions, grantResults, mGrant);
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PermissionUtils.REQUEST_CODE_SETTING){
            new Handler().postDelayed(() -> requestPermission(), 500);

        }

    }
}
