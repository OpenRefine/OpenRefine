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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import au.com.bytecode.opencsv.CSVParser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringEscapeUtils;

import org.openrefine.ProjectMetadata;
import org.openrefine.importers.TabularParserHelper.TableDataReader;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowMapper;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.JSONUtilities;

public class SeparatorBasedImporter extends LineBasedImporterBase {

    private final TabularParserHelper tabularParserHelper;

    public SeparatorBasedImporter(DatamodelRunner runner) {
        super(runner);
        tabularParserHelper = new TabularParserHelper(runner);
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);
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
    public GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource, String archiveFileName,
            String sparkURI, long limit, ObjectNode options, MultiFileReadingProgress progress)
            throws Exception {
        boolean processQuotes = JSONUtilities.getBoolean(options, "processQuotes", true);

        // the default 'multiLine' setting is set to true for backwards-compatibility
        // although the UI now proposes 'false' by default for performance reasons (similarly to Spark)
        boolean multiLine = JSONUtilities.getBoolean(options, "multiLine", true);

        // If we use quotes, then a line of the original file does not necessarily correspond
        // to a row in the grid, so we unfortunately cannot use the logic from LineBasedImporterBase.
        // We resort to loading the whole file in memory.
        if (processQuotes && multiLine) {
            GridState lines = limit > 0 ? runner.loadTextFile(sparkURI, progress, limit) : runner.loadTextFile(sparkURI, progress);
            TableDataReader dataReader = createTableDataReader(metadata, job, lines, options);
            return tabularParserHelper.parseOneFile(metadata, job, fileSource, archiveFileName, dataReader, limit, options);
        } else {
            // otherwise, go for the efficient route, using line-based parsing
            return super.parseOneFile(metadata, job, fileSource, archiveFileName, sparkURI, limit, options, progress);
        }
    }

    // Only used when no escape character is set
    @Override
    protected RowMapper getRowMapper(ObjectNode options) {
        return new RowParser(options);
    }

    protected static CSVParser getCSVParser(ObjectNode options) {
        return (new RowParser(options)).getCSVParser();
    }

    protected static CSVParser getCSVParser(String sep, boolean processQuotes, boolean strictQuotes, String quoteCharacter) {
        if (sep == null || "".equals(sep)) {
            sep = "\\t";
        }
        sep = StringEscapeUtils.unescapeJava(sep);

        Character quote = CSVParser.DEFAULT_QUOTE_CHARACTER;
        if (quoteCharacter != null && quoteCharacter.trim().length() == 1) {
            quote = quoteCharacter.trim().charAt(0);
        }

        return new CSVParser(
                sep,
                quote,
                (char) 0, // we don't want escape processing
                strictQuotes,
                CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                !processQuotes);
    }

    public TableDataReader createTableDataReader(
            ProjectMetadata metadata,
            ImportingJob job,
            GridState grid,
            ObjectNode options) {
        List<Object> retrievedColumnNames = null;
        if (options.has("columnNames")) {
            String[] strings = JSONUtilities.getStringArray(options, "columnNames");
            if (strings.length > 0) {
                retrievedColumnNames = new ArrayList<Object>();
                for (String s : strings) {
                    s = s.trim();
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

        final CSVParser parser = getCSVParser(options);

        Iterator<IndexedRow> lines = grid.iterateRows(RowFilter.ANY_ROW, SortingConfig.NO_SORTING).iterator();

        TableDataReader dataReader = new TableDataReader() {

            boolean usedColumnNames = false;

            @Override
            public List<Object> getNextRowOfCells() throws IOException {
                if (columnNames != null && !usedColumnNames) {
                    usedColumnNames = true;
                    return columnNames;
                } else {
                    if (!lines.hasNext()) {
                        return null;
                    } else {
                        String line = (String) lines.next().getRow().getCellValue(0);
                        return getCells(line, parser, lines);
                    }
                }
            }
        };

        return dataReader;
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

    /**
     * Row mapper used to split lines when parsing without quote handling or without multi-line support
     */
    static protected class RowParser implements RowMapper {

        private static final long serialVersionUID = -3376815677470571001L;

        private final String _sep;
        private final boolean _processQuotes;
        private final boolean _strictQuotes;
        private final String _quoteCharacter;

        protected RowParser(ObjectNode options) {
            _sep = JSONUtilities.getString(options, "separator", "\\t");
            _processQuotes = JSONUtilities.getBoolean(options, "processQuotes", true);
            _strictQuotes = JSONUtilities.getBoolean(options, "strictQuotes", false);
            _quoteCharacter = JSONUtilities.getString(options, "quoteCharacter", null);
        }

        // transient cache to ensure serializability
        private transient CSVParser _csvParser = null;

        protected CSVParser getCSVParser() {
            if (_csvParser != null) {
                return _csvParser;
            }
            _csvParser = SeparatorBasedImporter.getCSVParser(_sep, _processQuotes, _strictQuotes, _quoteCharacter);
            return _csvParser;
        }

        @Override
        public Row call(long rowId, Row row) {
            CSVParser parser = getCSVParser();
            String[] cellStrings;
            try {
                if (_processQuotes) {
                    cellStrings = parser.parseLineMulti((String) row.getCellValue(0));
                    if (parser.isPending()) {
                        // the last cell value was unterminated
                        // we retrieve the cell content by artificially parsing a line containing an escape character
                        // only
                        String[] remainingCell = parser.parseLineMulti(_quoteCharacter);
                        cellStrings = ArrayUtils.addAll(cellStrings, remainingCell);
                    }
                } else {
                    cellStrings = parser.parseLine((String) row.getCellValue(0));
                }
            } catch (IOException e) {
                // this can only happen when a quoted cell is unterminated:
                // we have reached the end of the line without a terminating quote character
                throw new IllegalStateException(e);
            }
            return new Row(Arrays.asList(cellStrings).stream().map(s -> new Cell(s, null)).collect(Collectors.toList()));
        }
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
