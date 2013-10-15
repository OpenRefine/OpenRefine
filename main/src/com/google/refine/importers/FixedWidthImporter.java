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
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class FixedWidthImporter extends TabularImportingParserBase {
    public FixedWidthImporter() {
        super(false);
    }
    
    @Override
    public JSONObject createParserUIInitializationData(
            ImportingJob job, List<JSONObject> fileRecords, String format) {
        JSONObject options = super.createParserUIInitializationData(job, fileRecords, format);
        JSONArray columnWidths = new JSONArray();
        if (fileRecords.size() > 0) {
            JSONObject firstFileRecord = fileRecords.get(0);
            String encoding = ImportingUtilities.getEncoding(firstFileRecord);
            String location = JSONUtilities.getString(firstFileRecord, "location", null);
            if (location != null) {
                File file = new File(job.getRawDataDir(), location);
                int[] columnWidthsA = guessColumnWidths(file, encoding);
                if (columnWidthsA != null) {
                    for (int w : columnWidthsA) {
                        JSONUtilities.append(columnWidths, w);
                    }
                }
            }

            JSONUtilities.safePut(options, "headerLines", 0);
            JSONUtilities.safePut(options, "columnWidths", columnWidths);
            JSONUtilities.safePut(options, "guessCellValueTypes", false);
        }
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
        JSONObject options,
        List<Exception> exceptions
    ) {
        final int[] columnWidths = JSONUtilities.getIntArray(options, "columnWidths");
        
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
                
                if (retrievedColumnNames.size() > 0) {
                    JSONUtilities.safePut(options, "headerLines", 1);
                } else {
                    retrievedColumnNames = null;
                }
            }
        }
        
        final List<Object> columnNames = retrievedColumnNames;
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
                        return getCells(line, columnWidths);
                    }
                }
            }
        };
        
        TabularImportingParserBase.readTable(project, metadata, job, dataReader, fileSource, limit, options, exceptions);
    }
    
    /**
     * Splits the line into columns
     * @param line Line to be split
     * @param widths array of integers with field sizes
     * @return
     */
    static private ArrayList<Object> getCells(String line, int[] widths) {
        ArrayList<Object> cells = new ArrayList<Object>();
        
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
