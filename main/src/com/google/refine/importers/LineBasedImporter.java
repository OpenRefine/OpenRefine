package com.google.refine.importers;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.util.JSONUtilities;

public class LineBasedImporter extends TabularImportingParserBase {
    static final Logger logger = LoggerFactory.getLogger(LineBasedImporter.class);
    
    public LineBasedImporter() {
        super(false);
    }
    
    @Override
    public JSONObject createParserUIInitializationData(
            ImportingJob job, List<JSONObject> fileRecords, String format) {
        JSONObject options = super.createParserUIInitializationData(job, fileRecords, format);
        
        JSONUtilities.safePut(options, "linesPerRow", 1);
        JSONUtilities.safePut(options, "headerLines", 0);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        
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
        final int linesPerRow = JSONUtilities.getInt(options, "linesPerRow", 1);
        
        final List<Object> columnNames;
        if (options.has("columnNames")) {
            columnNames = new ArrayList<Object>();
            String[] strings = JSONUtilities.getStringArray(options, "columnNames");
            for (String s : strings) {
                columnNames.add(s);
            }
            JSONUtilities.safePut(options, "headerLines", 1);
        } else {
            columnNames = null;
            JSONUtilities.safePut(options, "headerLines", 0);
        }
        
        final LineNumberReader lnReader = new LineNumberReader(reader);
        
        try {
            int skip = JSONUtilities.getInt(options, "ignoreLines", -1);
            while (skip > 0) {
                lnReader.readLine();
                skip--;
            }
        } catch (IOException e) {
            logger.error("Error reading line-based file", e);
        }
        JSONUtilities.safePut(options, "ignoreLines", -1);
        
        TableDataReader dataReader = new TableDataReader() {
            boolean usedColumnNames = false;
            
            @Override
            public List<Object> getNextRowOfCells() throws IOException {
                if (columnNames != null && !usedColumnNames) {
                    usedColumnNames = true;
                    return columnNames;
                } else {
                    List<Object> cells = null;
                    for (int i = 0; i < linesPerRow; i++) {
                        String line = lnReader.readLine();
                        if (i == 0) {
                            if (line == null) {
                                return null;
                            } else {
                                cells = new ArrayList<Object>(linesPerRow);
                                cells.add(line);
                            }
                        } else if (line != null) {
                            cells.add(line);
                        } else {
                            break;
                        }
                    }
                    return cells;
                }
            }
        };
        
        TabularImportingParserBase.readTable(project, metadata, job, dataReader, fileSource, limit, options, exceptions);
        
        super.parseOneFile(project, metadata, job, fileSource, reader, limit, options, exceptions);
    }
}
