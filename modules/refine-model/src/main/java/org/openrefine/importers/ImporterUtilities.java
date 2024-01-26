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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import org.mozilla.universalchardet.UnicodeBOMInputStream;

import org.openrefine.importing.EncodingGuesser;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.messages.OpenRefineMessage;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.process.ProgressReporter;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TrackingInputStream;

public class ImporterUtilities {

    static public Serializable parseCellValue(String text) {
        if (text.length() > 0) {
            String text2 = CharMatcher.whitespace().trimFrom(text);
            if (text2.length() > 0) {
                try {
                    return Long.parseLong(text2);
                } catch (NumberFormatException e) {
                }

                try {
                    double d = Double.parseDouble(text2);
                    if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                        return d;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return text;
    }

    static public int getIntegerOption(String name, Properties options, int def) {
        int value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            if (s != null) {
                try {
                    value = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                }
            }
        }
        return value;
    }

    static public boolean getBooleanOption(String name, Properties options, boolean def) {
        boolean value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            if (s != null) {
                value = "on".equalsIgnoreCase(s) || "1".equals(s) || Boolean.parseBoolean(s);
            }
        }
        return value;
    }

    static public void appendColumnName(List<String> columnNames, int index, String name) {
        name = CharMatcher.whitespace().trimFrom(name);

        while (columnNames.size() <= index) {
            columnNames.add("");
        }

        if (!name.isEmpty()) {
            String oldName = columnNames.get(index);
            if (!oldName.isEmpty()) {
                name = oldName + " " + name;
            }

            columnNames.set(index, name);
        }
    }

    static public ColumnModel setupColumns(List<String> columnNames) {
        Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
        for (int c = 0; c < columnNames.size(); c++) {
            String cell = CharMatcher.whitespace().trimFrom(columnNames.get(c));
            if (cell.isEmpty()) {
                cell = "Column";
            } else if (cell.startsWith("\"") && cell.endsWith("\"")) {
                // FIXME: is trimming quotation marks appropriate?
                cell = CharMatcher.whitespace().trimFrom(cell.substring(1, cell.length() - 1));
            }

            if (nameToIndex.containsKey(cell)) {
                int index = nameToIndex.get(cell);
                nameToIndex.put(cell, index + 1);

                cell = cell.contains(" ") ? (cell + " " + index) : (cell + index);
            } else {
                nameToIndex.put(cell, 2);
            }

            columnNames.set(c, cell);
        }

        return new ColumnModel(columnNames.stream().map(name -> new ColumnMetadata(name)).collect(Collectors.toList()));

    }

    /**
     * A proxy for reporting the progress of reading individual files back into the global progress of an importing job.
     * The importing process is split into two stages:
     * <ul>
     * <li>the initial reading of the files required to create the corresponding {@link Grid};</li>
     * <li>the saving of the concatenated grids into the workspace.</li>
     * </ul>
     * We consider that the first phase accounts for the first 50% of progress and the saving phase accounts for the
     * second 50% of the global progress. In the first phase, progress is measured by bytes read from the original
     * files. In the second phase, progress is measured by proportion of rows saved (over the total number of rows).
     */
    static public MultiFileReadingProgress createMultiFileReadingProgress(
            final ImportingJob job,
            List<ImportingFileRecord> fileRecords) {
        long totalSize = 0;
        for (ImportingFileRecord fileRecord : fileRecords) {
            totalSize += fileRecord.getSize(job.getRawDataDir());
        }

        final long totalSize2 = totalSize;
        return new MultiFileReadingProgress() {

            long totalBytesRead = 0L;
            String lastFileSource = null;
            long lastBytesRead = 0L;
            boolean batchEnded = false;

            void setProgress(String fileSource, long bytesRead) {
                if (!batchEnded) {
                    if (lastFileSource != null && !lastFileSource.equals(fileSource)) {
                        totalBytesRead += lastBytesRead;
                    }
                    job.setProgress(totalSize2 == 0 ? -1 : (int) (50 * (totalBytesRead + bytesRead) / totalSize2),
                            "Reading " + fileSource);
                    lastBytesRead = bytesRead;
                    lastFileSource = fileSource;
                }
            }

            @Override
            public void startFile(String fileSource) {
                setProgress(fileSource, 0);
            }

            @Override
            public void readingFile(String fileSource, long bytesRead) {
                setProgress(fileSource, bytesRead);
            }

            @Override
            public void endFiles() {
                batchEnded = true;
            }
        };
    }

    /**
     * Creates a progress reporter for the second phase of project import: saving the project in the workspace.
     */
    static public ProgressReporter createProgressReporterForProjectSave(final ImportingJob job) {
        return new ProgressReporter() {

            @Override
            public void reportProgress(int percentage) {
                job.setProgress(50 + percentage / 2, "Saving project in the workspace");
            }
        };
    }

    static public InputStream openAndTrackFile(
            final String fileSource,
            final File file,
            final MultiFileReadingProgress progress) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(file);
        return progress == null ? inputStream : new TrackingInputStream(inputStream) {

            @Override
            protected long track(long bytesRead) {
                long l = super.track(bytesRead);

                progress.readingFile(fileSource, this.bytesRead);

                return l;
            }

            @Override
            public void close() throws IOException {
                super.close();
            }
        };
    }

    /**
     * Adds columns to a column model if it misses one column to store a cell at a given index.
     */
    public static ColumnModel expandColumnModelIfNeeded(ColumnModel columnModel, int c) {
        List<ColumnMetadata> columns = columnModel.getColumns();
        if (c < columns.size()) {
            return columnModel;
        } else if (c == columns.size()) {
            String prefix = OpenRefineMessage.importer_utilities_column() + " ";
            int i = c + 1;
            while (true) {
                String columnName = prefix + i;
                ColumnMetadata column = columnModel.getColumnByName(columnName);
                if (column != null) {
                    i++;
                } else {
                    column = new ColumnMetadata(columnName);
                    return columnModel.appendUnduplicatedColumn(column);
                }
            }
        } else {
            throw new IllegalStateException(String.format("Column model has too few columns to store cell at index %d", c));
        }
    }

    static public Reader getReaderFromStream(InputStream inputStream, ImportingFileRecord fileRecord, String commonEncoding) {
        String encoding = fileRecord.getDerivedEncoding();
        if (encoding == null) {
            encoding = commonEncoding;
        }
        if (encoding != null) {

            // Special case for UTF-8 with BOM
            if (EncodingGuesser.UTF_8_BOM.equals(encoding)) {
                try {
                    return new InputStreamReader(new UnicodeBOMInputStream(inputStream, true), UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Exception from UnicodeBOMInputStream", e);
                }
            } else {
                try {
                    return new InputStreamReader(inputStream, encoding);
                } catch (UnsupportedEncodingException e) {
                    // This should never happen since they picked from a list of supported encodings
                    throw new RuntimeException("Unsupported encoding: " + encoding, e);
                }
            }
        }
        return new InputStreamReader(inputStream);
    }

    /**
     * Given a list of importing file records, return the most common format in them, or null if none of the records
     * contain a format.
     * 
     * @param records
     *            the importing file records to read formats from
     * @return the most common format, or null
     */
    public static String mostCommonFormat(List<ImportingFileRecord> records) {
        Map<String, Integer> formatToCount = new HashMap<>();
        List<String> formats = new ArrayList<>();
        for (ImportingFileRecord fileRecord : records) {
            String format = fileRecord.getFormat();
            if (format != null) {
                if (formatToCount.containsKey(format)) {
                    formatToCount.put(format, formatToCount.get(format) + 1);
                } else {
                    formatToCount.put(format, 1);
                    formats.add(format);
                }
            }
        }
        Collections.sort(formats, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return formatToCount.get(o2) - formatToCount.get(o1);
            }
        });

        return formats.size() > 0 ? formats.get(0) : null;
    }

    static public ArrayNode convertErrorsToJsonArray(List<Exception> exceptions) {
        ArrayNode a = ParsingUtilities.mapper.createArrayNode();
        for (Exception e : exceptions) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            ObjectNode o = ParsingUtilities.mapper.createObjectNode();
            JSONUtilities.safePut(o, "message", e.getLocalizedMessage());
            JSONUtilities.safePut(o, "stack", sw.toString());
            JSONUtilities.append(a, o);
        }
        return a;
    }

    public static boolean isCompressed(File file) throws IOException {
        // Check for common compressed file types to protect ourselves from binary data
        try (InputStream is = new FileInputStream(file)) {
            byte[] magic = new byte[4];
            int count = is.read(magic);
            if (count == 4 && Arrays.equals(magic, new byte[] { 0x50, 0x4B, 0x03, 0x04 }) || // zip
                    Arrays.equals(magic, new byte[] { 0x50, 0x4B, 0x07, 0x08 }) ||
                    (magic[0] == 0x1F && magic[1] == (byte) 0x8B) || // gzip
                    (magic[0] == 'B' && magic[1] == 'Z' && magic[2] == 'h') // bzip2
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given two grids with potentially different columns, unify the two into a single grid by adding columns of the
     * second grid which are not present in the first at the end.
     */
    protected static Grid mergeGrids(List<Grid> grids) {
        if (grids.isEmpty()) {
            throw new IllegalArgumentException("Impossible to merge an empty list of grids");
        } else if (grids.size() == 1) {
            return grids.get(0);
        }

        // first, compute for each grid a reordering of columns,
        // such that all grids can be mapped to a unified column model.
        List<Map<Integer, Integer>> columnMappings = new ArrayList<>();
        List<ColumnMetadata> currentColumns = new ArrayList<>();
        Map<String, Integer> columnNameToIndex = new HashMap<>();

        for (Grid grid : grids) {
            List<ColumnMetadata> originalColumns = grid.getColumnModel().getColumns();
            Map<Integer, Integer> reordering = new HashMap<>();
            for (int i = 0; i != originalColumns.size(); i++) {
                ColumnMetadata column = originalColumns.get(i);
                String columnName = column.getName();
                Integer currentIndex = columnNameToIndex.get(columnName);
                if (currentIndex == null) {
                    currentIndex = currentColumns.size();
                    currentColumns.add(column);
                    columnNameToIndex.put(column.getName(), currentIndex);
                }
                reordering.put(currentIndex, i);
            }
            columnMappings.add(reordering);
        }

        // Compute the new common column model of all grids after reordering their columns
        ColumnModel unifiedColumnModel = new ColumnModel(currentColumns);

        // Map each grid to the new column model
        List<Grid> finalGrids = new ArrayList<>();
        for (int i = 0; i != grids.size(); i++) {
            Grid originalGrid = grids.get(i);
            Map<Integer, Integer> mapping = columnMappings.get(i);
            finalGrids.add(originalGrid.mapRows(translateSecondGrid(unifiedColumnModel.getColumns().size(), mapping), unifiedColumnModel));
        }
        Grid finalGrid = finalGrids.get(0).concatenate(finalGrids.subList(1, finalGrids.size()));
        return finalGrid;
    }

    private static RowMapper translateSecondGrid(int nbColumns, Map<Integer, Integer> positions) {
        return new RowMapper() {

            private static final long serialVersionUID = 5925580892216279741L;

            @Override
            public Row call(long rowId, Row row) {
                List<Cell> cells = new ArrayList<>(nbColumns);
                for (int i = 0; i != nbColumns; i++) {
                    Integer mappedIndex = positions.get(i);
                    cells.add(mappedIndex == null ? null : row.getCell(mappedIndex));
                }
                return new Row(cells, row.flagged, row.starred);
            }

        };
    }
}
