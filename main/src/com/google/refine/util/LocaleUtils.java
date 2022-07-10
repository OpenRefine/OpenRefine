package com.google.refine.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocaleUtils {
    public static void setLocale(String code) {
        String localeCode;
        try {
            ResourceBundle langResource = ResourceBundle.getBundle("language_codes");
            localeCode = (String) langResource.getObject(code);
        } catch (NullPointerException | MissingResourceException e) {
            return;
        }

        String[] localeParts = localeCode.split("_");
        Locale.Builder localeBuilder = new Locale.Builder();
        switch (localeParts.length) {
            case 1: localeBuilder.setLanguage(localeParts[0]); break;
            case 2: localeBuilder.setLanguage(localeParts[0]).setRegion(localeParts[1]);break;
            default: return;
        }
        Locale.setDefault(localeBuilder.build());
        System.out.println(Locale.getDefault());
    }
}
