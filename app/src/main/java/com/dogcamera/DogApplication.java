package com.dogcamera;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

/**
 * Created by huanli on 2018/2/26.
 */

public class DogApplication extends Application{

    private static DogApplication sInstance;

    protected RefWatcher refWatcher;

    public static DogApplication getInstance(){
        return sInstance;
    }

    public static RefWatcher getRefWatcher(Context context){
        DogApplication application = (DogApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        refWatcher = LeakCanary.install(sInstance);
    }
}
