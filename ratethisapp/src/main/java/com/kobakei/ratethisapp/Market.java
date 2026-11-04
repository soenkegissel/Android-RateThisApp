package com.kobakei.ratethisapp;

/**
 * Created by Sönke Gissel on 25.05.2020.
 */
import java.util.Locale;

public enum Market {
    GOOGLE,
    AMAZON,
    HUAWEI,
    SAMSUNG;

    public static Market fromName(String name) {
        if (name == null) return GOOGLE;
        try {
            return Market.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GOOGLE;
        }
    }
}
