package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.Soundex;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Phonetic implements Function {

    private DoubleMetaphone metaphone2 = new DoubleMetaphone();
    private Metaphone metaphone = new Metaphone();
    private Soundex soundex = new Soundex();

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o2 instanceof String) {
                String str = (o1 instanceof String) ? (String) o1 : o1.toString();
                String encoding = ((String) o2).toLowerCase();
                if ("doublemetaphone".equals(encoding)) {
                    return metaphone2.doubleMetaphone(str);
                } else if ("metaphone".equals(encoding)) {
                    return metaphone.metaphone(str);
                } else if ("soundex".equals(encoding)) {
                    return soundex.soundex(str);
                } else {
                    return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " doesn't know how to handle the '" + encoding + "' encoding.");
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 3 strings");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the a phonetic encoding of s (optionally indicating which encoding to use')");
        writer.key("params"); writer.value("string s, string encoding (optional, defaults to 'DoubleMetaphone')");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
