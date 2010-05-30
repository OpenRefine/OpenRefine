package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.clustering.binning.FingerprintKeyer;
import com.metaweb.gridworks.clustering.binning.Keyer;
import com.metaweb.gridworks.gel.Function;

public class Fingerprint implements Function {

    static Keyer fingerprint = new FingerprintKeyer();

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String s = (o instanceof String) ? (String) o : o.toString();
            return fingerprint.key(s);
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
