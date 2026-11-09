
package com.google.refine.translate;

import java.util.Locale;

public enum TranslationLanguage {

    SPANISH("Spanish", "es"), MANDARIN("Mandarin", "zh-Hans"), TELUGU("Telugu", "te"), TAMIL("Tamil", "ta"), TAGALOG("Tagalog", "tl");

    private final String displayName;
    private final String code;

    TranslationLanguage(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public static TranslationLanguage fromName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unsupported translation language: " + name, e);
        }
    }
}
