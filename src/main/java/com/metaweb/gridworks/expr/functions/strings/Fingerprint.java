package com.metaweb.gridworks.expr.functions.strings;

import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class Fingerprint implements Function {

    static final Pattern alphanum = Pattern.compile("\\p{Punct}|\\p{Cntrl}");
    
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String s = (o instanceof String) ? (String) o : o.toString(); 
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
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the fingerprint of s, a derived string that aims to be a more canonical form of it (this is mostly useful for finding clusters of strings related to the same information).");
        writer.key("params"); writer.value("string s");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
