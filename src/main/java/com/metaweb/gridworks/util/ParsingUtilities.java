package com.metaweb.gridworks.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ParsingUtilities {
    static public String inputStreamToString(InputStream is) throws IOException {
        Reader reader = new InputStreamReader(is, "UTF-8");
        try {
            return readerToString(reader);
        } finally {
            reader.close();
        }
    }

    static public String readerToString(Reader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        
        char[] chars = new char[8192];
        int c;
        
        while ((c = reader.read(chars)) > 0) {
            sb.insert(sb.length(), chars, 0, c);
        }
        
        return sb.toString();
    }

    static public JSONObject evaluateJsonStringToObject(String s) throws JSONException {
        JSONTokener t = new JSONTokener(s);
        JSONObject o = (JSONObject) t.nextValue();
        return o;
    }
    
    static public JSONArray evaluateJsonStringToArray(String s) throws JSONException {
        JSONTokener t = new JSONTokener(s);
        JSONArray a = (JSONArray) t.nextValue();
        return a;
    }
    
    private static final URLCodec codec = new URLCodec();
    static public String encode(String s) {
        try {
            return codec.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // should not happen
        }
    }
    static public String decode(String s) {
        try {
            return codec.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // should not happen
        } catch (DecoderException e) {
            return s; // should not happen
        }
    }
}
