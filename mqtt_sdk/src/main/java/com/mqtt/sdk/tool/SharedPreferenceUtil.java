package com.mqtt.sdk.tool;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by Administrator on 2017/9/29.
 */

public class SharedPreferenceUtil {

    public static final String CHAO_MENG_PREFERENCE = "mqtt_parameter";
    public static final String KEEP_ALIVE = "keep_alive";

    private final SharedPreferences preferences;
    private static SharedPreferenceUtil sInstance;
    private final SharedPreferences.Editor mEditor;

    private SharedPreferenceUtil(Context context) {
        preferences = context.getSharedPreferences(CHAO_MENG_PREFERENCE, Context.MODE_PRIVATE);
        mEditor = preferences.edit();
    }

    public static SharedPreferenceUtil getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SharedPreferenceUtil.class) {
                if (sInstance == null) {
                    sInstance = new SharedPreferenceUtil(context);
                }
            }
        }
        return sInstance;
    }

    public void putBoolean(String key, boolean value) {
        mEditor.putBoolean(key, value);
        mEditor.commit();
    }

    public void putString(String key, String value) {
        mEditor.putString(key, value);
        mEditor.commit();
    }

    public void putFloat(String key, float value) {
        mEditor.putFloat(key, value);
        mEditor.commit();
    }

    public void putInt(String key, int value) {
        mEditor.putInt(key, value);
        mEditor.commit();
    }

    public void putLong(String key, long value) {
        mEditor.putLong(key, value);
        mEditor.commit();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public String getString(String key) {
        return preferences.getString(key, "");
    }

    public float getFloat(String key, float defaultValue) {
        return preferences.getFloat(key, defaultValue);
    }

    public float getFloat(String key) {
        return preferences.getFloat(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    public int getInt(String key) {
        return preferences.getInt(key, 0);
    }

    public long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    public long getLong(String key) {
        return preferences.getLong(key, 0);
    }

    public void clearAllData() {
        mEditor.clear().commit();
    }

    public void removeValue(String key) {
        mEditor.remove(key).commit();
    }

}
