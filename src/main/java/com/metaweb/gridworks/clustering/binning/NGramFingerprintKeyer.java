package com.metaweb.gridworks.clustering.binning;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class NGramFingerprintKeyer extends Keyer {

    static final Pattern alphanum = Pattern.compile("\\p{Punct}|\\p{Cntrl}|\\p{Space}");
    
    public String key(String s, Object... o) {
        int ngram_size = 1;
        if (o != null && o.length > 0 && o[0] instanceof Number) {
            ngram_size = (Integer) o[0];
        }
        s = s.toLowerCase(); // then lowercase it
        s = alphanum.matcher(s).replaceAll(""); // then remove all punctuation and control chars
        TreeSet<String> set = ngram_split(s,ngram_size);
        StringBuffer b = new StringBuffer();
        Iterator<String> i = set.iterator();
        while (i.hasNext()) {
            b.append(i.next());
        }
        return b.toString(); // join ordered fragments back together
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
