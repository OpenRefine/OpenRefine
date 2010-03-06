package com.metaweb.gridworks.clustering.binning;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class FingerprintKeyer extends Keyer {

    static final Pattern alphanum = Pattern.compile("\\p{Punct}|\\p{Cntrl}");
    
    public String key(String s, Object... o) {
        s = s.trim(); // first off, remove whitespace around the string
        s = s.toLowerCase(); // then lowercase it
        s = alphanum.matcher(s).replaceAll(""); // then remove all punctuation and control chars
        String[] frags = StringUtils.split(s); // split by whitespace
        TreeSet<String> set = new TreeSet<String>();
        for (String ss : frags) {
            set.add(ss); // order fragments and dedupe
        }
        StringBuffer b = new StringBuffer();
        Iterator<String> i = set.iterator();
        while (i.hasNext()) {
            b.append(i.next());
            b.append(' ');
        }
        return b.toString(); // join ordered fragments back together
    }

}
