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

package com.google.refine.util;

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

    static final private ThreadLocal<SimpleDateFormat> ISO8601_FORMAT = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        }

    };

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
        return inputStreamToString(is, "UTF-8");
    }
    
    static public String inputStreamToString(InputStream is, String encoding) throws IOException {
        Reader reader = new InputStreamReader(is, encoding);
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
        if( s == null ) {
            throw new IllegalArgumentException("parameter 's' should not be null");
        }
        JSONTokener t = new JSONTokener(s);
        Object o = t.nextValue();
        if (o instanceof JSONObject) {
            return (JSONObject) o;
        } else {
            throw new JSONException(s + " couldn't be parsed as JSON object");
        }
    }

    static public JSONArray evaluateJsonStringToArray(String s) throws JSONException {
        JSONTokener t = new JSONTokener(s);
        Object o = t.nextValue();
        if (o instanceof JSONArray) {
            return (JSONArray) o;
        } else {
            throw new JSONException(s + " couldn't be parsed as JSON array");
        }
    }

    private static final URLCodec codec = new URLCodec();
    /**
     * Encode a string as UTF-8.
     */
    static public String encode(String s) {
        try {
            return codec.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // should not happen
        }
    }

    /**
     * Decode a string from UTF-8 encoding.
     */
    static public String decode(String s) {
        try {
            return codec.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // should not happen
        } catch (DecoderException e) {
            return s; // should not happen
        }
    }

    /**
     * Convert a date/time to an ISO 8601 string
     * 
     * @param d the date to be written
     * @return string with ISO 8601 formatted date & time
     */
    static public String dateToString(Date d) {
        return ISO8601_FORMAT.get().format(d);
    }

    /**
     * Parse an ISO 8601 formatted string into a Java Date.
     * 
     * @param s the string to be parsed
     * @return Date or null if the parse failed
     */
    static public Date stringToDate(String s) {
        try {
            return ISO8601_FORMAT.get().parse(s);
        } catch (ParseException e) {
            return null;
        }
    }
}
