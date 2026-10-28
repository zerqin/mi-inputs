package com.tvsetup.mibutton;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Язык интерфейса: английский (по умолчанию) или русский. Выбор хранится
 * в настройках и применяется ко всем экранам.
 */
public final class AppLocale {

    public static final String EN = "en";
    public static final String RU = "ru";

    private AppLocale() {
    }

    /** Контекст с выбранной локалью. */
    public static Context wrap(Context base) {
        String lang = Prefs.getLanguage(base);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }

    /** Название для кнопки в шапке (сами названия языков не переводим). */
    public static String shortName(String code) {
        return RU.equals(code) ? "Русский" : "English";
    }
}
