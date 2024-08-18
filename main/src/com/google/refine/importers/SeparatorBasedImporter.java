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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.univocity.parsers.common.AbstractParser;
import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.UnescapedQuoteHandling;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.commons.text.StringEscapeUtils;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class SeparatorBasedImporter extends TabularImportingParserBase {

    // Excel limits: 1M rows x 16K columns, 32K characters max per cell
    public static final int MAX_COLUMNS = 16 * 1024; // default 512
    // TODO: Perhaps use a lower default and make user configurable?
    public static final int MAX_CHARACTERS_PER_CELL = 32 * 1024; // default 4096
    public static final int GUESSER_LINE_COUNT = 100;
    char DEFAULT_QUOTE_CHAR = new CsvParserSettings().getFormat().getQuote();

    public SeparatorBasedImporter() {
        super(false);
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        String separator = guessSeparator(job, fileRecords);
        String nonNullSeparator = separator != null ? separator : "\\t";
        JSONUtilities.safePut(options, "separator", nonNullSeparator);

        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "processQuotes", !nonNullSeparator.equals("\\t"));
        JSONUtilities.safePut(options, "quoteCharacter", String.valueOf(DEFAULT_QUOTE_CHAR));
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

        // TODO: Perhaps ask user to declare explicitly if they want TSV or weird CSV with \t separator hybrid?
        boolean tsv = "\t".equals(sep) && !processQuotes && !strictQuotes;

        List<Object> retrievedColumnNames = null;
        if (options.has("columnNames")) {
            String[] strings = JSONUtilities.getStringArray(options, "columnNames");
            if (strings.length > 0) {
                retrievedColumnNames = new ArrayList<>();
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

        Character quote = DEFAULT_QUOTE_CHAR;
        String quoteCharacter = JSONUtilities.getString(options, "quoteCharacter", null);
        if (quoteCharacter != null && CharMatcher.whitespace().trimFrom(quoteCharacter).length() == 1) {
            quote = CharMatcher.whitespace().trimFrom(quoteCharacter).charAt(0);
        }

        AbstractParser parser;
        if (tsv) {
            TsvParserSettings settings = new TsvParserSettings();
            settings.setMaxCharsPerColumn(MAX_CHARACTERS_PER_CELL);
            settings.setMaxColumns(MAX_COLUMNS);
            settings.setIgnoreLeadingWhitespaces(false);
            settings.setIgnoreTrailingWhitespaces(false);
            parser = new TsvParser(settings);
        } else {
            CsvParserSettings settings = new CsvParserSettings();
            CsvFormat format = settings.getFormat();
            format.setDelimiter(sep);
            format.setQuote(quote);
            format.setLineSeparator("\n");
            settings.setIgnoreLeadingWhitespaces(false);
            settings.setIgnoreTrailingWhitespaces(false);
            if (strictQuotes) {
                settings.setUnescapedQuoteHandling(UnescapedQuoteHandling.RAISE_ERROR);
            }
            settings.setKeepQuotes(!processQuotes);
            settings.setMaxCharsPerColumn(MAX_CHARACTERS_PER_CELL);
            settings.setMaxColumns(MAX_COLUMNS);
            parser = new CsvParser(settings);
        }
        try (final LineNumberReader lnReader = new LineNumberReader(reader);) {

            parser.beginParsing(lnReader);

            TableDataReader dataReader = new TableDataReader() {

                boolean usedColumnNames = false;

                @Override
                public List<Object> getNextRowOfCells() throws IOException {
                    if (columnNames != null && !usedColumnNames) {
                        usedColumnNames = true;
                        return columnNames;
                    } else {
                        Record record = parser.parseNextRecord();
                        if (record != null) {
                            return Arrays.asList(record.getValues());
                        } else {
                            return null;
                        }
                    }
                }
            };

            TabularImportingParserBase.readTable(project, job, dataReader, limit, options, exceptions);
        } catch (TextParsingException e) {
            exceptions.add(e);
        } catch (IOException e) {
            exceptions.add(e);
        }
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
                CsvFormat format = guessFormat(file, encoding);
                if (format != null) {
                    if (separator != null) {
                        if (format.getDelimiter() != separator.separator) {
                            logger.warn("Delimiter guesses disagree - uniVocity: '{}' - internal: '{}'", format.getDelimiter(),
                                    separator.separator);
                        }
                        // Even if they disagree, use our guess for backward compatibility
                        return StringEscapeUtils.escapeJava(Character.toString(separator.separator));
                    } else {
                        // We got a guess from CsvParser, but not ours, so let's use that
                        return StringEscapeUtils.escapeJava(format.getDelimiterString());
                    }
                } else {
                    if (separator != null) {
                        // Our guesser worked when CsvParser's didn't
                        return StringEscapeUtils.escapeJava(Character.toString(separator.separator));
                    }
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

    static public CsvFormat guessFormat(File file, String encoding) {
        try (InputStream is = new FileInputStream(file);
                Reader reader = ImportingUtilities.getInputStreamReader(is, encoding);
                LineNumberReader lineNumberReader = new LineNumberReader(reader)) {
            CsvParserSettings settings = new CsvParserSettings();
            // We could provide a set of delimiters to consider below if we wanted to restrict this
            settings.detectFormatAutomatically();
            settings.setFormatDetectorRowSampleCount(GUESSER_LINE_COUNT); // default is 20, but let's match our guesser
            CsvParser parser = new CsvParser(settings);
            parser.beginParsing(lineNumberReader);
            // Format guesser result is available as soon as beginParsing() completes
            return parser.getDetectedFormat();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static public Separator guessSeparator(File file, String encoding) {
        return guessSeparator(file, encoding, false); // quotes off for backward compatibility
    }

    static public Separator guessSeparator(File file, String encoding, boolean handleQuotes) {
        try {
            try (InputStream is = new FileInputStream(file);
                    Reader reader = ImportingUtilities.getInputStreamReader(is, encoding);
                    LineNumberReader lineNumberReader = new LineNumberReader(reader)) {

                List<Separator> separators = new ArrayList<>();
                Map<Character, Separator> separatorMap = new HashMap<>();

                int totalChars = 0;
                int lineCount = 0;
                boolean inQuote = false;
                String s;
                while (totalChars < 64 * 1024 &&
                        lineCount < GUESSER_LINE_COUNT &&
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

                    Collections.sort(separators, Comparator.comparingDouble(sep0 -> sep0.stddev / sep0.averagePerLine));

                    Separator separator = separators.get(0);
                    if (separator.stddev / separator.averagePerLine < 0.1) {
                        return separator;
                    }

                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
