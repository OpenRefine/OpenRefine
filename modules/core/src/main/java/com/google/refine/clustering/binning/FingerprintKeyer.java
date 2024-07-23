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

import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;

/**
 * Fingerprint keyer where fingerprint is sorted list of unique words after case and diacritic folding and removing all
 * punctuation. Word boundary is any whitespace character, while output key has words joined with a single ASCII space
 * character.
 *
 */
public class FingerprintKeyer extends Keyer {

    // Punctuation plus C0 & C1 controls (except for whitespace characters which we need for split to work)
    // Added LF, VT, FF, CR, NEL to the control characters not stripped - tfm 2020-10-17
    static final Pattern punctctrl = Pattern.compile("\\p{Punct}|[\\x00-\\x08\\x0E-\\x1F\\x7F\\x80-\\x84\\x86-\\x9F]",
            Pattern.UNICODE_CHARACTER_CLASS);

    public static final Pattern DIACRITICS_AND_FRIENDS = Pattern
            // Lm = modifier letter, Sk = modifier symbol
            .compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+",
            Pattern.UNICODE_CHARACTER_CLASS);
    // First part of table based on https://stackoverflow.com/a/1453284/167425 by Andreas Petersson
    private static final ImmutableMap<String, String> NONDIACRITICS = ImmutableMap.<String, String> builder()
            // Replace non-diacritics with their equivalent characters
            .put("ß", "ss")
            .put("æ", "ae")
            .put("ø", "oe")
            .put("å", "aa") // TODO: We'll never see this after decomposition
            .put("©", "c") // copyright character
            .put("\u00F0", "d") // Small letter Icelandic eth
            .put("\u0111", "d") // Small letter D with stroke
            .put("\u0256", "d") // Small letter African D
            .put("\u00FE", "th") // Lower case Icelandic thorn þ
            .put("ƿ", "w") // Lower case Wynn from Old English modernly transliterated to w
            // Visually similar replacements from our private former asciify() method
            // (only need lower case forms since we're already downcased)
            .put("\u0127", "h") // small H with stroke
            .put("\u0131", "i") // dotless I
            .put("\u0138", "k") // small letter Kra
            .put("\u0142", "l") // Bialystock
            .put("\u014B", "n") // Small letter Eng
            .put("\u017F", "s") // long s
            .put("\u0167", "t") // small letter T with stroke
            // Additional characters following the same principle
            .put("œ", "oe")
            .put("ẜ", "s") // more long S forms
            .put("ẝ", "s")
            .build();

    @Override
    public String key(String s, Object... o) {
        if (s == null || o != null && o.length > 0) {
            throw new IllegalArgumentException("Fingerprint keyer accepts a single string parameter");
        }
        return WHITESPACE.splitAsStream(normalize(s, true)).sorted().distinct().collect(Collectors.joining(" "));
    }

    protected String normalize(String s) {
        s = normalize(s, false); // letter transforms only for backward compatibility
        return s;
    }

    protected String normalize(String s, boolean strong) {
        if (strong) {
            s = CharMatcher.whitespace().trimFrom(s); // first off, remove whitespace around the string
            s = s.toLowerCase(); // TODO: This is using the default locale. Is that what we want?
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
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < orig.length(); i++) {
            String source = orig.substring(i, i + 1);
            String replace = NONDIACRITICS.get(source);
            result.append(replace == null ? source : replace);
        }
        return result.toString();
    }

}
