package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.util.ParsingUtilities;

public class Cell implements Serializable, HasFields, Jsonizable {
    private static final long serialVersionUID = -5891067829205458102L;
    
    final public Serializable   value;
    final public Recon          recon;
    
    public Cell(Serializable value, Recon recon) {
        this.value = value;
        this.recon = recon;
    }
    
    public Object getField(String name, Properties bindings) {
        if ("value".equals(name)) {
            return value;
        } else if ("recon".equals(name)) {
            return recon;
        }
        return null;
    }

    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        if (ExpressionUtils.isError(value)) {
            writer.key("e");
            writer.value(((EvalError) value).message);
        } else {
            writer.key("v");
            if (value != null) {
                if (value instanceof Calendar) {
                    writer.value(ParsingUtilities.dateToString(((Calendar) value).getTime()));
                    writer.key("t"); writer.value("date");
                } else if (value instanceof Date) {
                    writer.value(ParsingUtilities.dateToString((Date) value));
                    writer.key("t"); writer.value("date");
                } else {
                    writer.value(value);
                }
            } else {
                writer.value(null);
            }
        }
        
        if (recon != null) {
            writer.key("r");
            recon.write(writer, options);
        }
        writer.endObject();
    }
    
    public void save(Writer writer, Properties options) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public Cell load(String s) throws Exception {
        return load(ParsingUtilities.evaluateJsonStringToObject(s));
    }
    
    static public Cell load(JSONObject obj) throws Exception {
        Serializable value = null;
        Recon recon = null;
        
        if (obj.has("e")) {
            value = new EvalError(obj.getString("e"));
        } else if (obj.has("v")) {
            value = (Serializable) obj.get("v");
            if (obj.has("t")) {
                String type = obj.getString("t");
                if ("date".equals(type)) {
                    value = ParsingUtilities.stringToDate((String) value); 
                }
            }
        }
        
        if (obj.has("r")) {
            recon = Recon.load(obj.getJSONObject("r"));
        }
        
        return new Cell(value, recon);
    }
}
