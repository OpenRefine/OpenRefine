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

import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fingerprint keyer which generates a fingerprint from a sorted list of unique character N-grams after removing all
 * whitespace, control characters, and punctuation. N-grams are concatenated to form a single output key.
 *
 */
public class NGramFingerprintKeyer extends FingerprintKeyer {

    static final Pattern ctrlspace = Pattern.compile("\\p{Cntrl}|\\p{Space}", Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public String key(String s, Object... o) {
        int ngram_size = 2;
        if (o != null && o.length > 0 && o[0] instanceof Number) {
            ngram_size = (Integer) o[0];
        }
        s = normalize(s, true);
        s = ctrlspace.matcher(s).replaceAll(""); // then remove all control chars & whitespace
        return sorted_ngrams(s, ngram_size).collect(Collectors.joining());
    }

    /**
     * Generate a stream of sorted unique character N-grams from a string
     * 
     * @param s
     *            String to generate N-grams from
     * @param size
     *            number of characters per N-gram
     * @return a stream of sorted unique N-gram Strings
     */
    protected Stream<String> sorted_ngrams(String s, int size) {
        return IntStream.rangeClosed(0, s.length() - size)
                .mapToObj(i -> s.substring(i, i + size))
                .sorted()
                .distinct();
    }

    /**
     * @deprecated 2020-10-17 by tfmorris. Use {@link #sorted_ngrams(String, int)}
     */
    @Deprecated
    protected TreeSet<String> ngram_split(String s, int size) {
        TreeSet<String> set = new TreeSet<String>();
        int length = s.length();
        for (int i = 0; i + size <= length; i++) {
            set.add(s.substring(i, i + size));
        }
        return set;
    }

}
