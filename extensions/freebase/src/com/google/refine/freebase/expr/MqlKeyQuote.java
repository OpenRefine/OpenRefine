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

package com.google.refine.freebase.expr;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;

public class MqlKeyQuote implements Function {
    final static private String keyStartChar = "A-Za-z0-9";
    final static private String keyInternalChar = "A-Za-z0-9_-";
    final static private String keyEndChar = "A-Za-z0-9_";
    final static private String fullValidKey = "^[" + keyStartChar + "][" + keyInternalChar + "]*[" + keyEndChar + "]$";
    final static private String keyCharMustQuote = "([^" + keyInternalChar + "])";
    
    final static private Pattern fullValidKeyPattern = Pattern.compile(fullValidKey);
    final static private Pattern keyCharMustQuotePattern = Pattern.compile(keyCharMustQuote);
    
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object o1 = args[0];
            if (o1 != null && o1 instanceof String) {
                return mqlKeyQuote((String) o1);
            }
        }
        return null;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Unquotes a MQL key");
        writer.key("params"); writer.value("string s");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
    
    static String mqlKeyQuote(String s) {
        if (fullValidKeyPattern.matcher(s).find()) {
            return s;
        }
        
        StringBuffer sb = new StringBuffer();
        
        int last = 0;
        Matcher m = keyCharMustQuotePattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            if (start > last) {
                sb.append(s.substring(last, start));
            }
            last = end;
            
            sb.append('$');
            sb.append(quote(s.charAt(start)));
        }
        
        if (last < s.length()) {
            sb.append(s.substring(last));
        }
        
        if (sb.length() > 0) { 
            if (sb.charAt(0) == '-' || sb.charAt(0) == '_') {
                char c = sb.charAt(0);
                sb.deleteCharAt(0);
                sb.insert(0, '$');
                sb.insert(1, quote(c));
            } 
        
            int length = sb.length();
            if (sb.charAt(length-1) == '-') {
                sb.deleteCharAt(length-1);
                sb.insert(length-1, '$');
                sb.insert(length, quote('-'));
            }
        }
        
        return sb.toString();
    }
    
    static String quote(char c) {
        return StringUtils.leftPad(Integer.toHexString(c).toUpperCase(), 4, '0');        
    }
}
