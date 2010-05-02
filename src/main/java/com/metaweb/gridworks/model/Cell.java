package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.metaweb.gridworks.util.Pool;

public class Cell implements HasFields, Jsonizable {
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
    
    public boolean fieldAlsoHasFields(String name) {
        return "recon".equals(name);
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
            writer.value(Long.toString(recon.id));
            
            Pool pool = (Pool) options.get("pool");
            pool.pool(recon);
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
    
    static public Cell loadStreaming(String s, Pool pool) throws Exception {
        JsonFactory jsonFactory = new JsonFactory(); 
        JsonParser jp = jsonFactory.createJsonParser(s);
        
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            return null;
        }
        
        return loadStreaming(jp, pool);
    }
    
    static public Cell loadStreaming(JsonParser jp, Pool pool) throws Exception {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL || t != JsonToken.START_OBJECT) {
            return null;
        }
        
        Serializable value = null;
        String type = null;
        Recon recon = null;
        
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            
            if ("r".equals(fieldName)) {
                if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                    String reconID = jp.getText();
                    
                    recon = pool.getRecon(reconID);
                } else {
                    // legacy
                    recon = Recon.loadStreaming(jp, pool);
                }
            } else if ("e".equals(fieldName)) {
                value = new EvalError(jp.getText());
            } else if ("v".equals(fieldName)) {
                JsonToken token = jp.getCurrentToken();
            
                if (token == JsonToken.VALUE_STRING) {
                    value = jp.getText();
                } else if (token == JsonToken.VALUE_NUMBER_INT) {
                    value = jp.getLongValue();
                } else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                    value = jp.getDoubleValue();
                } else if (token == JsonToken.VALUE_TRUE) {
                    value = true;
                } else if (token == JsonToken.VALUE_FALSE) {
                    value = false;
                }
            } else if ("t".equals(fieldName)) {
                type = jp.getText();
            }
        }
        
        if (value != null) {
            if (type != null) {
                if ("date".equals(type)) {
                    value = ParsingUtilities.stringToDate((String) value); 
                }
            }
            return new Cell(value, recon);
        } else {
            return null;
        }
    }
}
