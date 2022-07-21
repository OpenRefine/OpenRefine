package com.google.refine.util;

import java.util.Locale;
import java.util.Map;

public class LocaleUtils {
    public static void setLocale(String code) {
        if (code == null) {
            return;
        }
        Map<String, String> languageCodeMap = Map.of(
                "bn", "ben",
                "jp", "ja",
                "zh", "zh_CN",
                "zh_Hant", "zh_TW"
        );

        String localeCode = languageCodeMap.getOrDefault(code, code);

        String[] localeParts = localeCode.split("_");
        Locale.Builder localeBuilder = new Locale.Builder();
        switch (localeParts.length) {
            case 1: localeBuilder.setLanguage(localeParts[0]); break;
            case 2: localeBuilder.setLanguage(localeParts[0]).setRegion(localeParts[1]); break;
            default: return;
        }
        Locale.setDefault(localeBuilder.build());
    }
}
