package com.tvsetup.mibutton;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Настройки приложения. Всё хранится локально на телевизоре,
 * сеть не используется.
 */
public final class Prefs {

    private static final String FILE = "settings";

    /** Клавиша кнопки Mi по умолчанию (KEYCODE_BUTTON_2). */
    public static final int DEFAULT_KEY = 191;

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int getKeyCode(Context c) {
        return sp(c).getInt("keycode", DEFAULT_KEY);
    }

    public static void setKeyCode(Context c, int code) {
        sp(c).edit().putInt("keycode", code).apply();
    }

    /** true — открывать штатный (системный) список входов вместо своего. */
    public static boolean useSystemPopup(Context c) {
        return sp(c).getBoolean("system_popup", false);
    }

    public static void setUseSystemPopup(Context c, boolean v) {
        sp(c).edit().putBoolean("system_popup", v).apply();
    }

    public static String getName(Context c, String inputId) {
        return sp(c).getString("name:" + inputId, null);
    }

    public static void setName(Context c, String inputId, String name) {
        SharedPreferences.Editor e = sp(c).edit();
        if (name == null) {
            e.remove("name:" + inputId);
        } else {
            e.putString("name:" + inputId, name);
        }
        e.apply();
    }

    public static boolean isHidden(Context c, String inputId) {
        return sp(c).getBoolean("hidden:" + inputId, false);
    }

    public static void setHidden(Context c, String inputId, boolean hidden) {
        sp(c).edit().putBoolean("hidden:" + inputId, hidden).apply();
    }

    /** true — перехватывать кнопку Mi; false — пауза (кнопка работает как обычно). */
    public static boolean isInterceptEnabled(Context c) {
        return sp(c).getBoolean("intercept", true);
    }

    public static void setInterceptEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("intercept", v).apply();
    }

    /** Язык интерфейса: en (по умолчанию) или ru. */
    public static String getLanguage(Context c) {
        String lang = sp(c).getString("lang", AppLocale.EN);
        return AppLocale.RU.equals(lang) ? AppLocale.RU : AppLocale.EN;
    }

    public static void setLanguage(Context c, String lang) {
        sp(c).edit().putString("lang", lang).apply();
    }

    public static String getVendorComponent(Context c) {
        return sp(c).getString("vendor", null);
    }

    public static void setVendorComponent(Context c, String value) {
        sp(c).edit().putString("vendor", value).apply();
    }
}
