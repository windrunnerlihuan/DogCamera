package com.dogcamera.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

public class ROMUtils {
    private static final String TAG = ROMUtils.class.getSimpleName();



    public enum ROM_TYPE {
        MIUI_ROM,
        EMUI_ROM,
        FLYME_ROM,
        FUNTOUCH_ROM,
        COLOROS_ROM,
        OTHER_ROM
    }

    /**
     * 小米 MIUI ROM标识
     * "ro.miui.ui.version.code" -> "5"
     * "ro.miui.ui.version.name" -> "V7"
     * <p>
     * 华为 EMUI ROM标识
     * "ro.build.version.emui" -> "EmotionUI_1.6"
     * <p>
     * 魅族 Flyme ROM标识
     * "ro.build.user" -> "flyme"
     * "persist.sys.use.flyme.icon" -> "true"
     * "ro.flyme.published" -> "true"
     * "ro.build.display.id" -> "Flyme OS 5.1.2.0U"
     * "ro.meizu.setupwizard.flyme" -> "true"
     * <p>
     * VIVO Funtouch ROM标识
     * "ro.vivo.os.build.display.id" -> "Funtouch OS_2.1"
     * <p>
     * OPPO ColorOS ROM标识
     * "ro.rom.different.version" -> "ColorOS2.1"
     */

    //小米 MIUI标识
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";

    //华为 EMUI标识
    private static final String KEY_EMUI_VERSION_CODE = "ro.build.version.emui";

    //魅族 Flyme标识
    private static final String KEY_FLYME_ICON_FALG = "persist.sys.use.flyme.icon";
    private static final String KEY_FLYME_SETUP_FALG = "ro.meizu.setupwizard.flyme";
    private static final String KEY_FLYME_PUBLISH_FALG = "ro.flyme.published";

    private static final String KEY_FLYME_ID_FALG_KEY = "ro.build.display.id";
    private static final String KEY_FLYME_ID_FALG_VALUE_KEYWORD = "Flyme";
    private static final String KEY_FLYME_DISPLAY_ID = "ro.build.display.id";

    //VIVO Funtouch标识
    private static final String KEY_FUNTOUCH_DISPLAY_ID = "ro.vivo.os.build.display.id";

    //OPPO ColorOS标识
    private static final String KEY_COLOROS_VERSION = "ro.rom.different.version";

    public static ROM_TYPE getROMType() {
        ROM_TYPE rom_type = ROM_TYPE.OTHER_ROM;
        try {
            BuildProperties buildProperties = BuildProperties.getInstance();
            //小米
            if (buildProperties.containsKey(KEY_MIUI_VERSION_CODE)
                    || buildProperties.containsKey(KEY_MIUI_VERSION_NAME)
                    || buildProperties.containsKey(KEY_MIUI_INTERNAL_STORAGE)) {
                return ROM_TYPE.MIUI_ROM;
            }
            //华为
            if (buildProperties.containsKey(KEY_EMUI_VERSION_CODE)) {
                return ROM_TYPE.EMUI_ROM;
            }
            //魅族
            if (buildProperties.containsKey(KEY_FLYME_ICON_FALG)
                    || buildProperties.containsKey(KEY_FLYME_SETUP_FALG)
                    || buildProperties.containsKey(KEY_FLYME_PUBLISH_FALG)) {
                return ROM_TYPE.FLYME_ROM;
            }
            if (buildProperties.containsKey(KEY_FLYME_ID_FALG_KEY)) {
                String romName = buildProperties.getProperty(KEY_FLYME_ID_FALG_KEY);
                if (!TextUtils.isEmpty(romName) && romName.contains(KEY_FLYME_ID_FALG_VALUE_KEYWORD)) {
                    return ROM_TYPE.FLYME_ROM;
                }
            }
            //VIVO
            if (buildProperties.containsKey(KEY_FUNTOUCH_DISPLAY_ID)) {
                return ROM_TYPE.FUNTOUCH_ROM;
            }
            //OPPO
            if (buildProperties.containsKey(KEY_COLOROS_VERSION)) {
                return ROM_TYPE.COLOROS_ROM;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rom_type;
    }

    public static String getROMVersion() {
        String result = "";
        try {
            BuildProperties buildProperties = BuildProperties.getInstance();
            switch (getROMType()) {
                case MIUI_ROM:
                    result = "miui " + buildProperties.getProperty(KEY_MIUI_VERSION_NAME, "");
                    break;
                case EMUI_ROM:
                    result = buildProperties.getProperty(KEY_EMUI_VERSION_CODE, "");
                    break;
                case FLYME_ROM:
                    result = buildProperties.getProperty(KEY_FLYME_DISPLAY_ID, "");
                    break;
                case FUNTOUCH_ROM:
                    result = buildProperties.getProperty(KEY_FUNTOUCH_DISPLAY_ID, "");
                    break;
                case COLOROS_ROM:
                    result = buildProperties.getProperty(KEY_COLOROS_VERSION, "");
                    break;
                default:
                    result = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "ROM_VERSION = " + result);
        return result;
    }

    public static boolean isMIUI() {
        try {
            BuildProperties properties = BuildProperties.getInstance();
            return (properties.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                    || properties.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                    || properties.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null)
                    || isX("Xiaomi");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isHuawei() {
        return "HUAWEI".equalsIgnoreCase(Build.MANUFACTURER) && !"google".equalsIgnoreCase(Build.BRAND);
    }


    public static boolean isX(String x) {
        return Build.MANUFACTURER.equalsIgnoreCase(x);
    }


    public static boolean isVivo(){
        try {
            return Build.BRAND.toLowerCase().contains("vivo") && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isOppo(Context context) {
        try {
            return Build.BRAND.toLowerCase().contains("oppo") && Build.VERSION.SDK_INT < Build.VERSION_CODES.N ;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isMeiZu() {
        try {
            BuildProperties properties = BuildProperties.getInstance();
            return properties.getProperty(KEY_FLYME_ICON_FALG, null) != null
                    || properties.getProperty(KEY_FLYME_SETUP_FALG, null) != null
                    || properties.getProperty(KEY_FLYME_PUBLISH_FALG, null) != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

