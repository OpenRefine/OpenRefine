/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import com.google.refine.importing.FormatGuesser;

public class TextFormatGuesser implements FormatGuesser {

    @Override
    public String guess(File file, String encoding, String seedFormat) {
        try {
            InputStream is = new FileInputStream(file);
            Reader reader = encoding != null ? new InputStreamReader(is, encoding) : new InputStreamReader(is);

            try {
                int totalBytes = 0;
                int openBraces = 0;
                int closeBraces = 0;
                int openAngleBrackets = 0;
                int closeAngleBrackets = 0;
                int wikiTableBegin = 0;
                int wikiTableRow = 0;
                int trailingPeriods = 0;
                
                char firstChar = ' ';
                boolean foundFirstChar = false;
                
                char[] chars = new char[4096];
                int c;
                while (totalBytes < 64 * 1024 && (c = reader.read(chars)) > 0) {
                    String chunk = String.valueOf(chars, 0, c);
                    openBraces += countSubstrings(chunk, "{");
                    closeBraces += countSubstrings(chunk, "}");
                    openAngleBrackets += countSubstrings(chunk, "<");
                    closeAngleBrackets += countSubstrings(chunk, ">");
                    wikiTableBegin += countSubstrings(chunk, "{|");
                    wikiTableRow += countSubstrings(chunk, "|-");
                    trailingPeriods += countLineSuffix(chunk, ".");
                    
                    if (!foundFirstChar) {
                        chunk = chunk.trim();
                        if (chunk.length() > 0) {
                            firstChar = chunk.charAt(0);
                            foundFirstChar = true;
                        }
                    }
                    totalBytes += c;
                }
                
                if (foundFirstChar) {
                    if (wikiTableBegin >= 1 && wikiTableRow >= 2) {
                        return "text/wiki";
                    } if ((firstChar == '{' || firstChar == '[') &&
                        openBraces >= 5 && closeBraces >= 5) {
                        return "text/json";
                    } else if (openAngleBrackets >= 5 && closeAngleBrackets >= 5) {
                        if (trailingPeriods > 0) {
                            return "text/rdf+n3";
                        } else if (firstChar == '<') {
                            return "text/xml";
                        }
                    }
                }
                return "text/line-based";
            } finally {
                reader.close();
                is.close();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public int countSubstrings(String s, String sub) {
        int count = 0;
        int from = 0;
        while (from < s.length()) {
            int i = s.indexOf(sub, from);
            if (i < 0) {
                break;
            } else {
                from = i + sub.length();
                count++;
            }
        }
        return count;
    }
    
    static public int countLineSuffix(String s, String suffix) {
        int count = 0;
        int from = 0;
        while (from < s.length()) {
            int lineEnd = s.indexOf('\n', from);
            if (lineEnd < 0) {
                break;
            } else {
                int i = lineEnd - 1;
                while (i >= from + suffix.length() - 1) {
                    if (Character.isWhitespace(s.charAt(i))) {
                        i--;
                    } else {
                        String suffix2 = s.subSequence(i - suffix.length() + 1, i + 1).toString();
                        if (suffix2.equals(suffix)) {
                            count++;
                        }
                        break;
                    }
                }
                from = lineEnd + 1;
            }
        }
        return count;
    }

}
