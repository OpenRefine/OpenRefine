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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.CharMatcher;
import com.google.refine.importing.FormatGuesser;

public class TextFormatGuesser implements FormatGuesser {

    private static final int XML_BRACKETS_THRESHOLD = 5;
    private static final int JSON_BRACES_THRESHOLD = 5;
    private static final long CONTROLS_THRESHOLD = 10;

    @Override
    public String guess(File file, String encoding, String seedFormat) {
        try (InputStream fis = new FileInputStream(file)) {
            if (isCompressed(file)) {
                return "binary";
            }
            ;

            InputStream bis = new BoundedInputStream(fis, 64 * 1024); // TODO: This seems like a lot
            try (BufferedReader reader = new BufferedReader(
                    encoding != null ? new InputStreamReader(bis, encoding) : new InputStreamReader(bis))) {
                int totalChars = 0;
                long openBraces = 0;
                int closeBraces = 0;
                int openAngleBrackets = 0;
                int closeAngleBrackets = 0;
                int wikiTableBegin = 0;
                int wikiTableEnd = 0;
                int wikiTableRow = 0;
                int trailingPeriods = 0;
                int controls = 0;

                char firstChar = ' ';
                boolean foundFirstChar = false;

                String line;
                while ((line = reader.readLine()) != null && controls < CONTROLS_THRESHOLD) {
                    line = CharMatcher.whitespace().trimFrom(line);
                    controls += CharMatcher.javaIsoControl().and(CharMatcher.whitespace().negate()).countIn(line);
                    openBraces += line.chars().filter(ch -> ch == '{').count();
                    closeBraces += StringUtils.countMatches(line, "}");
                    openAngleBrackets += StringUtils.countMatches(line, "<");
                    closeAngleBrackets += StringUtils.countMatches(line, ">");
                    if (line.startsWith("{|")) {
                        wikiTableBegin++;
                    } else if (line.startsWith("|}")) {
                        wikiTableEnd++;
                    } else if (line.startsWith("|-")) {
                        wikiTableRow++;
                    }
                    if (line.endsWith(".")) {
                        trailingPeriods++;
                    }

                    if (!foundFirstChar) {
                        if (line.length() > 0) {
                            firstChar = line.charAt(0);
                            foundFirstChar = true;
                        }
                    }
                    totalChars += line.length();
                }

                // TODO: Make thresholds proportional to the amount of data read?
                if (controls >= CONTROLS_THRESHOLD) {
                    return "binary";
                }

                if (foundFirstChar) {
                    if (wikiTableBegin >= 1 && (wikiTableBegin - wikiTableEnd <= 1) && wikiTableRow >= 2) {
                        return "text/wiki";
                    }
                    if ((firstChar == '{' || firstChar == '[') &&
                            openBraces >= JSON_BRACES_THRESHOLD && closeBraces >= JSON_BRACES_THRESHOLD) {
                        return "text/json";
                    } else if (openAngleBrackets >= XML_BRACKETS_THRESHOLD
                            && closeAngleBrackets >= XML_BRACKETS_THRESHOLD) {
                        if (trailingPeriods > 0) {
                            return "text/rdf/n3";
                        } else if (firstChar == '<') {
                            return "text/xml";
                        }
                    }
                }
                return "text/line-based";
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isCompressed(File file) throws IOException {
        // Check for common compressed file types to protect ourselves from binary data
        try (InputStream is = new FileInputStream(file)) {
            byte[] magic = new byte[4];
            int count = is.read(magic);
            if (count == 4 && Arrays.equals(magic, new byte[] { 0x50, 0x4B, 0x03, 0x04 }) || // zip
                    Arrays.equals(magic, new byte[] { 0x50, 0x4B, 0x07, 0x08 }) ||
                    (magic[0] == 0x1F && magic[1] == (byte) 0x8B) // gzip
            ) {
                return true;
            }
        }
        return false;
    }

}
