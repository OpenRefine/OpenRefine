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
package org.openrefine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FixedWidthImporter extends LineBasedImporter {

    public FixedWidthImporter(DatamodelRunner datamodelRunner) {
        super(datamodelRunner);
    }
    
    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        if (fileRecords.size() > 0) {
            ImportingFileRecord firstFileRecord = fileRecords.get(0);
            try {
            	File file = firstFileRecord.getFile(job.getRawDataDir());
                int[] columnWidthsA = guessColumnWidths(file, firstFileRecord.getEncoding());
                if (columnWidthsA != null) {
                    for (int w : columnWidthsA) {
                        JSONUtilities.append(columnWidths, w);
                    }
                }
            } catch(IllegalArgumentException e) {
            	// the file is not stored in the import directory, skipping
            }

            JSONUtilities.safePut(options, "headerLines", 0);
            JSONUtilities.safePut(options, "columnWidths", columnWidths);
            JSONUtilities.safePut(options, "guessCellValueTypes", false);
        }
        return options;
    }
    

    @Override
    public RowMapper getRowMapper(ObjectNode options) {
        final int[] columnWidths = JSONUtilities.getIntArray(options, "columnWidths");

        return parseCells(columnWidths);
    }
    
    static private RowMapper parseCells(int[] widths) {
		return new RowMapper() {
			private static final long serialVersionUID = 1L;

            @Override
            public Row call(long rowId, Row row) {
                String line = (String)row.getCellValue(0);
                List<Cell> cellValues = getCells(line, widths)
                        .stream()
                        .map(v -> new Cell(v, null))
                        .collect(Collectors.toList());
                return new Row(cellValues);
            }
			
		};
    }
    
    /**
     * Splits the line into columns
     * @param line Line to be split
     * @param widths array of integers with field sizes
     * @return
     */
    static private List<Serializable> getCells(String line, int[] widths) {
        ArrayList<Serializable> cells = new ArrayList<>();
        
        int columnStartCursor = 0;
        int columnEndCursor = 0;
        for (int width : widths) {
            if (columnStartCursor >= line.length()) {
                cells.add(null); //FIXME is adding a null cell (to represent no data) OK?
                continue;
            }
            
            columnEndCursor = columnStartCursor + width;
            
            if (columnEndCursor > line.length()) {
                columnEndCursor = line.length();
            }
            if (columnEndCursor <= columnStartCursor) {
                cells.add(null); //FIXME is adding a null cell (to represent no data, or a zero width column) OK? 
                continue;
            }
            
            cells.add(line.substring(columnStartCursor, columnEndCursor));
            
            columnStartCursor = columnEndCursor;
        }
        
        // Residual text
        if (columnStartCursor < line.length()) {
            cells.add(line.substring(columnStartCursor));
        }
        return cells;
    }
    
    static public int[] guessColumnWidths(File file, String encoding) {
        try {
            InputStream is = new FileInputStream(file);
            Reader reader = (encoding != null) ? new InputStreamReader(is, encoding) : new InputStreamReader(is);
            LineNumberReader lineNumberReader = new LineNumberReader(reader);

            try {
                int[] counts = null;
                int totalBytes = 0;
                int lineCount = 0;
                String s;
                while (totalBytes < 64 * 1024 &&
                       lineCount < 100 &&
                       (s = lineNumberReader.readLine()) != null) {
                    
                    totalBytes += s.length() + 1; // count the new line character
                    if (s.length() == 0) {
                        continue;
                    }
                    lineCount++;
                    
                    if (counts == null) {
                        counts = new int[s.length()];
                        for (int c = 0; c < counts.length; c++) {
                            counts[c] = 0;
                        }
                    }
                    
                    for (int c = 0; c < counts.length && c < s.length(); c++) {
                        char ch = s.charAt(c);
                        if (ch == ' ') {
                            counts[c]++;
                        }
                    }
                }
                
                if (counts != null && lineCount > 2) {
                    List<Integer> widths = new ArrayList<Integer>();
                    
                    int startIndex = 0;
                    for (int c = 0; c < counts.length; c++) {
                        int count = counts[c];
                        if (count == lineCount) {
                            widths.add(c - startIndex + 1);
                            startIndex = c + 1;
                        }
                    }
                    
                    for (int i = widths.size() - 2; i >= 0; i--) {
                        if (widths.get(i) == 1) {
                            widths.set(i + 1, widths.get(i + 1) + 1);
                            widths.remove(i);
                        }
                    }
                    
                    int[] widthA = new int[widths.size()];
                    for (int i = 0; i < widthA.length; i++) {
                        widthA[i] = widths.get(i);
                    }
                    return widthA;
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
