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
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;

public class FingerprintKeyer extends Keyer {

    // Punctuation and control characters (except for TAB which we need for split to work)
    static final Pattern punctctrl = Pattern.compile("\\p{Punct}|[\\x00-\\x08\\x0A-\\x1F\\x7F]",
            Pattern.UNICODE_CHARACTER_CLASS);

    public static final Pattern DIACRITICS_AND_FRIENDS = Pattern
            .compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

    private static final ImmutableMap<String, String> NONDIACRITICS = ImmutableMap.<String, String>builder()
            //Replace non-diacritics as their equivalent characters
            .put("\u0141", "l") // BiaLystock
            .put("\u0142", "l") // Bialystock
            .put("ß", "ss")
            .put("æ", "ae")
            .put("ø", "oe") // oe or just o?
            .put("å", "aa")
            .put("©", "c")
            .put("\u00D0", "d") // Icelandic eth All Ð ð from http://de.wikipedia.org/wiki/%C3%90
            .put("\u00F0", "d")
            .put("\u0110", "d")
            .put("\u0111", "d")
            .put("\u0189", "d")
            .put("\u0256", "d")
            .put("\u00DE", "th") // Icelandic thorn Þ
            .put("\u00FE", "th") // thorn þ
            .build();


    @Override
    public String key(String s, Object... o) {
        if (s == null || o !=null && o.length > 0) {
            throw new IllegalArgumentException("Fingerprint keyer accepts a single string parameter");
        }
        s = s.trim(); // first off, remove whitespace around the string
        s = s.toLowerCase(); // TODO: This is using the default locale. Is that what we want?
        s = punctctrl.matcher(s).replaceAll(""); // then remove all punctuation and control chars
        s = normalize(s);
        String[] frags = StringUtils.split(s); // split by whitespace (excluding supplementary characters)
        TreeSet<String> set = new TreeSet<String>();
        for (String ss : frags) {
            set.add(ss); // order fragments and dedupe
        }
        StringBuffer b = new StringBuffer();
        Iterator<String> i = set.iterator();
        while (i.hasNext()) {  // join ordered fragments back together
            b.append(i.next());
            if (i.hasNext()) {
                b.append(' ');
            }
        }
        return b.toString();
    }

    protected String normalize(String s) {
        s = stripDiacritics(s);
        s = stripNonDiacritics(s);
        return s;
    }

    @Deprecated
    protected String asciify(String s) {
        return normalize(s);
    }

    protected static String stripDiacritics(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
        return str;
    }

    // Based on https://stackoverflow.com/a/1453284/167425 by Andreas Petersson
    private static String stripNonDiacritics(String orig) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < orig.length(); i++) {
            String source = orig.substring(i, i + 1);
            String replace = NONDIACRITICS.get(source);
            result.append(replace == null ? String.valueOf(source) : replace);
        }
        return result.toString();
    }

}