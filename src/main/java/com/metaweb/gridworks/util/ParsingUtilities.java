package com.metaweb.gridworks.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ParsingUtilities {

    static final public SimpleDateFormat s_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    static public Properties parseUrlParameters(HttpServletRequest request) {
        Properties options = new Properties();
        
        String query = request.getQueryString();
        if (query != null) {
            if (query.startsWith("?")) {
                query = query.substring(1);
            }
    
            parseParameters(options,query);
        }
        return options;
    }
    
    static public Properties parseParameters(Properties p, String str) {
        if (str != null) {
            String[] pairs = str.split("&");
            for (String pairString : pairs) {
                int equal = pairString.indexOf('=');
                String name = (equal >= 0) ? pairString.substring(0, equal) : "";
                String value = (equal >= 0) ? ParsingUtilities.decode(pairString.substring(equal + 1)) : "";
                p.put(name, value);
            }
        }
        return p;
    }
    
    static public Properties parseParameters(String str) {
        return (str == null) ? null : parseParameters(new Properties(),str);
    }
    
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
    
    static public String dateToString(Date d) {
        return s_sdf.format(d);
    }
    
    static public Date stringToDate(String s) {
        try {
            return s_sdf.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }
}
