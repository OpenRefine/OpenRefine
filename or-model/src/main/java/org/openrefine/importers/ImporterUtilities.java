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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.hadoop.fs.FileSystem;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TrackingInputStream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class ImporterUtilities {

    static public Serializable parseCellValue(String text) {
        if (text.length() > 0) {
            String text2 = text.trim();
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
        name = name.trim();

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
            String cell = columnNames.get(c).trim();
            if (cell.isEmpty()) {
                cell = "Column";
            } else if (cell.startsWith("\"") && cell.endsWith("\"")) {
                // FIXME: is trimming quotation marks appropriate?
                cell = cell.substring(1, cell.length() - 1).trim();
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
    
    static public interface MultiFileReadingProgress {
        public void startFile(String fileSource);
        public void readingFile(String fileSource, long bytesRead);
        public void endFile(String fileSource, long bytesRead);
    }
    
    static public MultiFileReadingProgress createMultiFileReadingProgress(
            final ImportingJob job,
            List<ImportingFileRecord> fileRecords,
            FileSystem hdfs) {
        long totalSize = 0;
        for (ImportingFileRecord fileRecord : fileRecords) {
			totalSize += fileRecord.getSize(job.getRawDataDir(), hdfs);
        }
        
        final long totalSize2 = totalSize;
        return new MultiFileReadingProgress() {
            long totalBytesRead = 0;
            
            void setProgress(String fileSource, long bytesRead) {
                job.setProgress(totalSize2 == 0 ? -1 : (int) (100 * (totalBytesRead + bytesRead) / totalSize2),
                    "Reading " + fileSource);
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
            public void endFile(String fileSource, long bytesRead) {
                totalBytesRead += bytesRead;
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
        };
    }

    /**
     * Adds columns to a column model if it misses one column
     * to store a cell at a given index.
     * 
     * @param columnModel
     * @param c
     * @return
     */
	public static ColumnModel expandColumnModelIfNeeded(ColumnModel columnModel, int c) {
		List<ColumnMetadata> columns = columnModel.getColumns();
		if(c < columns.size()) {
			return columnModel;
		} else if (c == columns.size()){
			List<ColumnMetadata> newColumns = new LinkedList<>(columns);
			String prefix = "Column ";
            int i = c + 1;
            while (true) {
                String columnName = prefix + i;
                ColumnMetadata column = columnModel.getColumnByName(columnName);
                if (column != null) {
                	i++;
                } else {
                    column = new ColumnMetadata(columnName);
                    newColumns.add(column);
                    return new ColumnModel(newColumns);
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
            try {
                return new InputStreamReader(inputStream, encoding);
            } catch (UnsupportedEncodingException e) {
                // Ignore and fall through
            }
        }
        return new InputStreamReader(inputStream);
    }

    /**
     * Given a list of importing file records, return the most
     * common format in them, or null if none of the records contain a format.
     * 
     * @param records
     *     the importing file records to read formats from
     * @return
     *     the most common format, or null
     */
    public static String mostCommonFormat(List<ImportingFileRecord> records) {
    	Map<String, Integer> formatToCount = new HashMap<>();
    	List<String> formats = new ArrayList<>();
    	for(ImportingFileRecord fileRecord : records) {
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
    
    /**
     * Given two grid states with potentially different columns,
     * unify the two into a single grid state by adding columns of the
     * second grid which are not present in the first at the end.
     * 
     * @param state1
     * @param state2
     * @return
     */
    protected static GridState mergeGridStates(GridState state1, GridState state2) {
        Map<Integer, Integer> positions = new HashMap<>();
        List<ColumnMetadata> mergedColumns = new ArrayList<>(state1.getColumnModel().getColumns());
        List<ColumnMetadata> columns2 = state2.getColumnModel().getColumns();
        ColumnModel columnModel1 = state1.getColumnModel();
        for (int i = 0; i != columns2.size(); i++) {
            String columnName = columns2.get(i).getName();
            int position = columnModel1.getColumnIndexByName(columnName);
            if (position == -1) {
                position = mergedColumns.size();
                mergedColumns.add(columns2.get(i));
            } else {
                mergedColumns.set(position, mergedColumns.get(position).merge(columns2.get(i)));
            }
            positions.put(i, position);
        }
        ColumnModel mergedColumnModel = new ColumnModel(mergedColumns);
        
        int origSizeState1 = columnModel1.getColumns().size();
        GridState translatedState1 = state1.mapRows(translateFirstGrid(origSizeState1, mergedColumns.size() - origSizeState1), mergedColumnModel);
        GridState translatedState2 = state2.mapRows(translateSecondGrid(mergedColumns.size(), positions), mergedColumnModel);
        return translatedState1.concatenate(translatedState2);
    }
    
    private static RowMapper translateFirstGrid(int oldColumns, int additionalColumns) {
        List<Cell> nullCells = Arrays.asList(new Cell[additionalColumns]);
        return new RowMapper() {

            private static final long serialVersionUID = 7085491437207390723L;

            @Override
            public Row call(long rowId, Row row) {
                return row.insertCells(oldColumns, nullCells);
            }
            
        };
    }
    
    private static RowMapper translateSecondGrid(int nbColumns, Map<Integer, Integer> positions) {
        return new RowMapper() {

            private static final long serialVersionUID = 5925580892216279741L;

            @Override
            public Row call(long rowId, Row row) {
                 List<Cell> cells = Arrays.asList(new Cell[nbColumns]);
                 for (int i = 0; i != row.getCells().size(); i++) {
                     cells.set(positions.get(i), row.getCell(i));
                 }
                 return new Row(cells, row.flagged, row.starred);
            }
            
        };
    }
}
