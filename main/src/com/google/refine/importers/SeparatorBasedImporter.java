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

package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.CharMatcher;
import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

import au.com.bytecode.opencsv.CSVParser;

public class SeparatorBasedImporter extends TabularImportingParserBase {

    public SeparatorBasedImporter() {
        super(false);
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        String separator = guessSeparator(job, fileRecords);
        JSONUtilities.safePut(options, "separator", separator != null ? separator : "\\t");

        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "processQuotes", true);
        JSONUtilities.safePut(options, "quoteCharacter", String.valueOf(CSVParser.DEFAULT_QUOTE_CHARACTER));
        JSONUtilities.safePut(options, "trimStrings", true);

        return options;
    }

    @Override
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            Reader reader,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        String sep = JSONUtilities.getString(options, "separator", "\\t");
        if (sep == null || "".equals(sep)) {
            sep = "\\t";
        }
        sep = StringEscapeUtils.unescapeJava(sep);
        boolean processQuotes = JSONUtilities.getBoolean(options, "processQuotes", true);
        boolean strictQuotes = JSONUtilities.getBoolean(options, "strictQuotes", false);

        List<Object> retrievedColumnNames = null;
        if (options.has("columnNames")) {
            String[] strings = JSONUtilities.getStringArray(options, "columnNames");
            if (strings.length > 0) {
                retrievedColumnNames = new ArrayList<Object>();
                for (String s : strings) {
                    s = CharMatcher.whitespace().trimFrom(s);
                    if (!s.isEmpty()) {
                        retrievedColumnNames.add(s);
                    }
                }

                if (!retrievedColumnNames.isEmpty()) {
                    JSONUtilities.safePut(options, "headerLines", 1);
                } else {
                    retrievedColumnNames = null;
                }
            }
        }

        final List<Object> columnNames = retrievedColumnNames;

        Character quote = CSVParser.DEFAULT_QUOTE_CHARACTER;
        String quoteCharacter = JSONUtilities.getString(options, "quoteCharacter", null);
        if (quoteCharacter != null && CharMatcher.whitespace().trimFrom(quoteCharacter).length() == 1) {
            quote = CharMatcher.whitespace().trimFrom(quoteCharacter).charAt(0);
        }

        final CSVParser parser = new CSVParser(
                sep,
                quote,
                (char) 0, // we don't want escape processing
                strictQuotes,
                CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                !processQuotes);

        final LineNumberReader lnReader = new LineNumberReader(reader);

        TableDataReader dataReader = new TableDataReader() {

            boolean usedColumnNames = false;

            @Override
            public List<Object> getNextRowOfCells() throws IOException {
                if (columnNames != null && !usedColumnNames) {
                    usedColumnNames = true;
                    return columnNames;
                } else {
                    String line = lnReader.readLine();
                    if (line == null) {
                        return null;
                    } else {
                        return getCells(line, parser, lnReader);
                    }
                }
            }
        };

        TabularImportingParserBase.readTable(project, job, dataReader, limit, options, exceptions);
    }

    static protected ArrayList<Object> getCells(String line, CSVParser parser, LineNumberReader lnReader)
            throws IOException {

        ArrayList<Object> cells = new ArrayList<Object>();
        String[] tokens = parser.parseLineMulti(line);
        cells.addAll(Arrays.asList(tokens));
        while (parser.isPending()) {
            tokens = parser.parseLineMulti(lnReader.readLine());
            cells.addAll(Arrays.asList(tokens));
        }
        return cells;
    }

    static public String guessSeparator(ImportingJob job, List<ObjectNode> fileRecords) {
        for (int i = 0; i < 5 && i < fileRecords.size(); i++) {
            ObjectNode fileRecord = fileRecords.get(i);
            String encoding = ImportingUtilities.getEncoding(fileRecord);
            String location = JSONUtilities.getString(fileRecord, "location", null);

            if (location != null) {
                File file = new File(job.getRawDataDir(), location);
                // Quotes are turned on by default, so use that for guessing
                Separator separator = guessSeparator(file, encoding, true);
                if (separator != null) {
                    return StringEscapeUtils.escapeJava(Character.toString(separator.separator));
                }
            }
        }
        return null;
    }

    static public class Separator {

        char separator;
        int totalCount = 0;
        int totalOfSquaredCount = 0;
        int currentLineCount = 0;

        double averagePerLine;
        double stddev;
    }

    static public Separator guessSeparator(File file, String encoding) {
        return guessSeparator(file, encoding, false); // quotes off for backward compatibility
    }

    // TODO: Move this to the CSV project?
    static public Separator guessSeparator(File file, String encoding, boolean handleQuotes) {
        try {
            InputStream is = new FileInputStream(file);
            Reader reader = encoding != null ? new InputStreamReader(is, encoding) : new InputStreamReader(is);
            LineNumberReader lineNumberReader = new LineNumberReader(reader);

            try {
                List<Separator> separators = new ArrayList<SeparatorBasedImporter.Separator>();
                Map<Character, Separator> separatorMap = new HashMap<Character, SeparatorBasedImporter.Separator>();

                int totalChars = 0;
                int lineCount = 0;
                boolean inQuote = false;
                String s;
                while (totalChars < 64 * 1024 &&
                        lineCount < 100 &&
                        (s = lineNumberReader.readLine()) != null) {

                    totalChars += s.length() + 1; // count the new line character
                    if (s.length() == 0) {
                        continue;
                    }
                    if (!inQuote) {
                        lineCount++;
                    }

                    for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        if ('"' == c) {
                            inQuote = !inQuote;
                        }
                        if (!Character.isLetterOrDigit(c)
                                && !"\"' .-".contains(s.subSequence(i, i + 1))
                                && (!handleQuotes || !inQuote)) {
                            Separator separator = separatorMap.get(c);
                            if (separator == null) {
                                separator = new Separator();
                                separator.separator = c;

                                separatorMap.put(c, separator);
                                separators.add(separator);
                            }
                            separator.currentLineCount++;
                        }
                    }

                    if (!inQuote) {
                        for (Separator separator : separators) {
                            separator.totalCount += separator.currentLineCount;
                            separator.totalOfSquaredCount += separator.currentLineCount * separator.currentLineCount;
                            separator.currentLineCount = 0;
                        }
                    }
                }

                if (separators.size() > 0) {
                    for (Separator separator : separators) {
                        separator.averagePerLine = separator.totalCount / (double) lineCount;
                        separator.stddev = Math.sqrt(
                                (((double) lineCount * separator.totalOfSquaredCount) - (separator.totalCount * separator.totalCount))
                                        / ((double) lineCount * (lineCount - 1)));
                    }

                    Collections.sort(separators, new Comparator<Separator>() {

                        @Override
                        public int compare(Separator sep0, Separator sep1) {
                            return Double.compare(sep0.stddev / sep0.averagePerLine,
                                    sep1.stddev / sep1.averagePerLine);
                        }
                    });

                    Separator separator = separators.get(0);
                    if (separator.stddev / separator.averagePerLine < 0.1) {
                        return separator;
                    }

                }
            } finally {
                lineNumberReader.close();
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
}
