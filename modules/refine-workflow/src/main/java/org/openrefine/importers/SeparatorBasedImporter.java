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

package org.openrefine.importers;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import au.com.bytecode.opencsv.CSVParser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import org.apache.commons.text.StringEscapeUtils;

import org.openrefine.ProjectMetadata;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.*;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.JSONUtilities;

public class SeparatorBasedImporter extends ReaderImporter {

    private final TabularParserHelper tabularParserHelper;

    public SeparatorBasedImporter() {
        tabularParserHelper = new TabularParserHelper();
    }

    @Override
    public ObjectNode createParserUIInitializationData(Runner runner,
            ImportingJob job, List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(runner, job, fileRecords, format);
        tabularParserHelper.createParserUIInitializationData(options);

        String separator = guessSeparator(job, fileRecords);
        JSONUtilities.safePut(options, "separator", separator != null ? separator : "\\t");

        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "processQuotes", true);
        JSONUtilities.safePut(options, "quoteCharacter", String.valueOf(CSVParser.DEFAULT_QUOTE_CHARACTER));
        JSONUtilities.safePut(options, "trimStrings", true);
        JSONUtilities.safePut(options, "multiLine", false);

        return options;
    }

    @Override
    public Grid parseOneFile(
            Runner runner,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            String archiveFileName,
            Supplier<Reader> reader,
            long limit,
            ObjectNode options) throws Exception {
        String sep = JSONUtilities.getString(options, "separator", "\\t");
        if (sep == null || "".equals(sep)) {
            sep = "\\t";
        }
        sep = StringEscapeUtils.unescapeJava(sep);
        boolean processQuotes = JSONUtilities.getBoolean(options, "processQuotes", true);
        boolean strictQuotes = JSONUtilities.getBoolean(options, "strictQuotes", false);
        boolean multiLine = JSONUtilities.getBoolean(options, "multiLine", true);

        List<String> retrievedColumnNames = null;
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

                if (retrievedColumnNames.isEmpty()) {
                    retrievedColumnNames = null;
                }
            }
        }

        final List<String> columnNames = retrievedColumnNames;

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

        CloseableIterable<Row> rowIterable = () -> {
            Reader freshReader = reader.get();
            final LineNumberReader lnReader = new LineNumberReader(freshReader);
            return new CloseableIterator<Row>() {

                Row nextRow = null;

                @Override
                public boolean hasNext() {
                    prepareNextRow();
                    return nextRow != null;
                }

                @Override
                public Row next() {
                    prepareNextRow();
                    Row row = nextRow;
                    nextRow = null;
                    return row;
                }

                public void prepareNextRow() {
                    if (nextRow != null) {
                        return;
                    }
                    nextRow = null;
                    try {
                        List<String> cells = new ArrayList<>();
                        String line;
                        while ((line = lnReader.readLine()) != null) {
                            cells.addAll(Arrays.asList(parser.parseLineMulti(line)));
                            if (parser.isPending() && !multiLine) {
                                // if each line of the file should correspond to a row, but the parsing of this
                                // row is not finished because of an earlier quote character, we artificially
                                // complete the row by feeding the CSV parser the required escape character
                                cells.addAll(Arrays.asList(parser.parseLineMulti(quoteCharacter)));
                            }
                            if (!parser.isPending()) {
                                break;
                            }
                        }
                        if (line != null || !cells.isEmpty()) {
                            nextRow = new Row(cells.stream().map(str -> new Cell(str, null)).collect(Collectors.toList()));
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public void close() {
                    try {
                        freshReader.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            };
        };

        Grid grid = tabularParserHelper.parseOneFile(runner, fileSource, archiveFileName, rowIterable, limit, options);
        if (retrievedColumnNames != null) {
            ColumnModel columnModel = grid.getColumnModel();
            List<ColumnMetadata> columns = new ArrayList<>(columnModel.getColumns().size());
            for (String columnName : retrievedColumnNames) {
                if (columns.size() < columnModel.getColumns().size()) {
                    columns.add(new ColumnMetadata(columnName));
                }
            }
            while (columns.size() < columnModel.getColumns().size()) {
                columns.add(columnModel.getColumnByIndex(columns.size()));
            }
            grid = grid.withColumnModel(new ColumnModel(columns));
        }
        return grid;
    }

    static protected ArrayList<Object> getCells(String line, CSVParser parser, Iterator<IndexedRow> lines)
            throws IOException {

        ArrayList<Object> cells = new ArrayList<Object>();
        String[] tokens = parser.parseLineMulti(line);
        cells.addAll(Arrays.asList(tokens));
        while (parser.isPending() && lines.hasNext()) {
            tokens = parser.parseLineMulti((String) lines.next().getRow().getCellValue(0));
            cells.addAll(Arrays.asList(tokens));
        }
        return cells;
    }

    static public String guessSeparator(ImportingJob job, List<ImportingFileRecord> fileRecords) {
        for (int i = 0; i < 5 && i < fileRecords.size(); i++) {
            ImportingFileRecord fileRecord = fileRecords.get(i);
            String encoding = fileRecord.getDerivedEncoding();
            String location = fileRecord.getLocation();

            if (location != null && location.length() > 0) {
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
