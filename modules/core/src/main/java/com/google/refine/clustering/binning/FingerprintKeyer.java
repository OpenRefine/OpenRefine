/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.clustering.binning;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;

/**
 * Fingerprint keyer where fingerprint is sorted list of unique words after case and diacritic folding,
 * punctuation removal, and language-specific stop word filtering.
 */
public class FingerprintKeyer extends Keyer {

    // Punctuation plus C0 & C1 controls (except for whitespace characters)
    static final Pattern punctctrl = Pattern.compile(
        "\\p{Punct}|[\\x00-\\x08\\x0E-\\x1F\\x7F\\x80-\\x84\\x86-\\x9F]",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    public static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile(
        "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+"
    );

    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final ImmutableMap<String, String> NONDIACRITICS = ImmutableMap.<String, String>builder()
        .put("ß", "ss").put("æ", "ae").put("ø", "oe").put("å", "aa").put("©", "c")
        .put("\u00F0", "d").put("\u0111", "d").put("\u0256", "d").put("\u00FE", "th").put("ƿ", "w")
        .put("\u0127", "h").put("\u0131", "i").put("\u0138", "k").put("\u0142", "l").put("\u014B", "n")
        .put("\u017F", "s").put("\u0167", "t").put("œ", "oe").put("ẜ", "s").put("ẝ", "s")
        .build();

    private static final Map<String, Set<String>> STOP_WORDS_MAP = new ConcurrentHashMap<>();

    static {
        loadStopWords();
    }

    private static void loadStopWords() {
        try {
            InputStream in = FingerprintKeyer.class
                    .getClassLoader()
                    .getResourceAsStream("modules/core/conf/stop_words.json");

            if (in == null) {
                System.err.println("stop_words.json not found in conf folder");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<String>> rawMap = mapper.readValue(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    Map.class
            );

            for (Map.Entry<String, List<String>> entry : rawMap.entrySet()) {
                STOP_WORDS_MAP.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }

        } catch (Exception e) {
            System.err.println("Failed to load stop_words.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String key(String s, Object... o) {
        if (s == null || (o != null && o.length > 1)) {
            throw new IllegalArgumentException("Fingerprint keyer accepts one string and optionally a language code");
        }

        String lang = (o != null && o.length == 1 && o[0] instanceof String) ? (String) o[0] : "en";
        Set<String> stopWords = STOP_WORDS_MAP.getOrDefault(lang, Collections.emptySet());

        return WHITESPACE.splitAsStream(normalize(s, true))
                .filter(token -> !stopWords.contains(token))
                .sorted()
                .distinct()
                .collect(Collectors.joining(" "));
    }

    protected String normalize(String s) {
        s = normalize(s, false); // letter transforms only for backward compatibility
        return s;
    }

    protected String normalize(String s, boolean strong) {
        if (strong) {
            s = CharMatcher.whitespace().trimFrom(s);  // first off, remove whitespace around the string
            s = s.toLowerCase();  // TODO: This is using the default locale. Is that what we want?
        }
        s = stripDiacritics(s);
        s = stripNonDiacritics(s);
        if (strong) {
            // TODO: Should these be converted to spaces instead of being removed?
            s = punctctrl.matcher(s).replaceAll("");
        }
        return s;
    }

    /**
     * @deprecated by tfmorris 2020-07-07 Use {@link #normalize(String)} or {{@link #normalize(String, boolean)}
     */
    @Deprecated
    protected String asciify(String s) {
        return normalize(s);
    }

    protected static String stripDiacritics(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFKD);
        str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
        return str;
    }

// Based on https://stackoverflow.com/a/1453284/167425 by Andreas Petersson    
    private static String stripNonDiacritics(String orig) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < orig.length(); i++) {
            String source = orig.substring(i, i + 1);
            String replace = NONDIACRITICS.get(source);
            result.append(replace == null ? source : replace);
        }
        return result.toString();
    }
}
