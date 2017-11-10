package com.crypho.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * Helper class to deal with SharedPreferences. Only deals with String values
 */
class SharedPreferencesUtil {

    public static String getString(Activity activity, String prefFileName, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity, prefFileName);
        return sharedPreferences.getString(key, null);
    }

    static void putString(Activity activity, String prefFileName, String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity, prefFileName);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static void remove(Activity activity, String prefFileName, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity, prefFileName);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    static Map<String, ?> getAll(Activity activity, String prefFileName) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity, prefFileName);
        return sharedPreferences.getAll();
    }

    static void clear(Activity activity, String prefFileName) {
        SharedPreferences sharedPreferences = getSharedPreferences(activity, prefFileName);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Gets an instance of SharedPreferences.
     *
     * @param activity     - Context
     * @param prefFileName (string) - Preference file name. Pass empty string if activity scoped pref
     *                     file is sufficient.
     * @return
     */
    private static SharedPreferences getSharedPreferences(Activity activity, String prefFileName) {
        SharedPreferences sharedPreferences;
        if (prefFileName.equals("")) {
            sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        } else {
            sharedPreferences = activity.getSharedPreferences(prefFileName, Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }
}
