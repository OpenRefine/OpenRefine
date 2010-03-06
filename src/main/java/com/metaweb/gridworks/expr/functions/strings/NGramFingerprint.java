package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.clustering.binning.Keyer;
import com.metaweb.gridworks.clustering.binning.NGramFingerprintKeyer;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class NGramFingerprint implements Function {

    static Keyer ngram_fingerprint = new NGramFingerprintKeyer();
    
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 || args.length == 2) {
            if (args[0] != null) {
                int ngram_size = 1;
                if (args.length == 2 && args[1] != null) {
                    ngram_size = (args[1] instanceof Number) ? ((Number) args[1]).intValue() : Integer.parseInt(args[1].toString());
                }
                Object o = args[0];
                String s = (o instanceof String) ? (String) o : o.toString();
                return ngram_fingerprint.key(s,ngram_size);
            }
            return null;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects at least a string");
    }

    protected TreeSet<String> ngram_split(String s, int size) {
        TreeSet<String> set = new TreeSet<String>();
        char[] chars = s.toCharArray();
        for (int i = 0; i + size <= chars.length; i++) {
            set.add(new String(chars,i,size));
        }
        return set;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the n-gram fingerprint of s");
        writer.key("params"); writer.value("string s, number n");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
