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

import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class NGramFingerprintKeyer extends FingerprintKeyer {

    static final Pattern alphanum = Pattern.compile("\\p{Punct}|\\p{Cntrl}|\\p{Space}");
    
    @Override
    public String key(String s, Object... o) {
        int ngram_size = 2;
        if (o != null && o.length > 0 && o[0] instanceof Number) {
            ngram_size = (Integer) o[0];
        }
        s = s.toLowerCase(); // then lowercase it
        s = alphanum.matcher(s).replaceAll(""); // then remove all punctuation and control chars
        TreeSet<String> set = ngram_split(s,ngram_size);
        StringBuffer b = new StringBuffer();
        Iterator<String> i = set.iterator();
        while (i.hasNext()) { // join ordered fragments back together
            b.append(i.next());
        }
        return asciify(b.toString()); // find ASCII equivalent to characters 
    }

    protected TreeSet<String> ngram_split(String s, int size) {
        TreeSet<String> set = new TreeSet<String>();
        char[] chars = s.toCharArray();
        for (int i = 0; i + size <= chars.length; i++) {
            set.add(new String(chars,i,size));
        }
        return set;
    }
}
