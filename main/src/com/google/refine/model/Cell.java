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

package com.google.refine.model;

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

import com.google.refine.Jsonizable;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFields;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;
import com.google.refine.util.StringUtils;

public class Cell implements HasFields, Jsonizable {
    final public Serializable   value;
    final public Recon          recon;
    
    public Cell(Serializable value, Recon recon) {
        this.value = value;
        this.recon = recon;
    }
    
    @Override
    public Object getField(String name, Properties bindings) {
        if ("value".equals(name)) {
            return value;
        } else if ("recon".equals(name)) {
            return recon;
        }
        return null;
    }
    
    @Override
    public boolean fieldAlsoHasFields(String name) {
        return "recon".equals(name);
    }

    @Override
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
                } else if (value instanceof Double 
                        && (((Double)value).isNaN() || ((Double)value).isInfinite())) {
                    // write as a string
                    writer.value(((Double)value).toString());
                } else if (value instanceof Float
                        && (((Float)value).isNaN() || ((Float)value).isInfinite())) {
                    // TODO: Skip?  Write as string?
                    writer.value(((Float)value).toString());
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
            if (type != null && "date".equals(type)) {
                value = ParsingUtilities.stringToDate((String) value); 
            }
            return new Cell(value, recon);
        } else {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(value);
    }
}
