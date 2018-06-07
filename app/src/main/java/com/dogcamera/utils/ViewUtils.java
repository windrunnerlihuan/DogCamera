package com.dogcamera.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by huanli on 2018/3/2.
 */

public class ViewUtils {

    private static int screenWidthPixels;
    private static int screenHeightPixels;

    /**
     * 得到屏幕像素宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidthPixels(Context context) {

        if (context == null) {
            //Log.e("Can't get screen size while the activity is null!");
            return 0;
        }

        if (screenWidthPixels > 0) {
            return screenWidthPixels;
        }
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Display display = manager.getDefaultDisplay();
        if (display != null) {
            display.getMetrics(dm);
            screenWidthPixels = dm.widthPixels;
        }
        return screenWidthPixels;
    }

    /**
     * 得到屏幕像素高度
     *
     * @param context
     * @return
     */
    public static int getScreenHeightPixels(Context context) {
        if (context == null) {
            //Log.e("Can't get screen size while the activity is null!");
            return 0;
        }

        if (screenHeightPixels > 0) {
            return screenHeightPixels;
        }
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Display display = manager.getDefaultDisplay();
        if (display != null) {
            display.getMetrics(dm);
            screenHeightPixels = dm.heightPixels;
        }
        return screenHeightPixels;
    }

    /**
     * dip转px
     *
     * @param context
     * @param dipValue
     * @return
     */
    public static int dip2px(Context context, float dipValue) {
        if (context == null) {
            return (int) dipValue;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (displayMetrics != null) {
            final float scale = displayMetrics.density;
            return (int) (dipValue * scale + 0.5f);
        } else {
            return (int) (dipValue * 3 + 0.5f) /* 使用主流手机的 density */;
        }
    }

    /**
     * px转dip
     *
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        if (context == null) {
            return (int) pxValue;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (displayMetrics != null) {
            final float scale = displayMetrics.density;
            return (int) (pxValue / scale + 0.5f);
        } else {
            return (int) (pxValue / 3 + 0.5f) /* 使用主流手机的 density */;
        }
    }
}
