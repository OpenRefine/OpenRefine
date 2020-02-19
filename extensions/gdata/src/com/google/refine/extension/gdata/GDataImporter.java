package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importers.TabularImportingParserBase.TableDataReader;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class GDataImporter {
    static final Logger logger = LoggerFactory.getLogger("GDataImporter");
    
    static public void parse(
        String token,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        int limit,
        ObjectNode options,
        List<Exception> exceptions) throws IOException {
    
        String docType = JSONUtilities.getString(options, "docType", null);
        if ("spreadsheet".equals(docType)) {
            Sheets service = GoogleAPIExtension.getSheetsService(token);
            parse(
                service,
                project,
                metadata,
                job,
                limit,
                options,
                exceptions
            );
        }
    }
    
    static public void parse(
        Sheets service,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        int limit,
        ObjectNode options,
        List<Exception> exceptions) {
        
        String docUrlString = JSONUtilities.getString(options, "docUrl", null);
        String worksheetUrlString = JSONUtilities.getString(options, "sheetUrl", null);
        
        // the index of the worksheet
        int worksheetIndex = JSONUtilities.getInt(options, "worksheetIndex", 0);
        
        if (docUrlString != null && worksheetUrlString != null) {
            try {
                parseOneWorkSheet(
                    service,
                    project,
                    metadata,
                    job,
                    new URL(docUrlString),
                    worksheetIndex,
                    limit,
                    options,
                    exceptions);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                exceptions.add(e);
            }
        }
    }
    
    static public void parseOneWorkSheet(
        Sheets service,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        URL docURL,
        int worksheetIndex,
        int limit,
        ObjectNode options,
        List<Exception> exceptions) {
        
        try {
            String spreadsheetId = GoogleAPIExtension.extractSpreadSheetId(docURL.toString());
            
            Spreadsheet response = service.spreadsheets().get(spreadsheetId)
                    .setIncludeGridData(true)
                    .execute();
            Sheet worksheetEntry = response.getSheets().get(worksheetIndex);
            
            String spreadsheetName = docURL.toExternalForm();
            
            String fileSource = spreadsheetName + " # " +
                worksheetEntry.getProperties().getTitle();
            
            setProgress(job, fileSource, 0);
            TabularImportingParserBase.readTable(
                project,
                metadata,
                job,
                new WorksheetBatchRowReader(job, fileSource, service, spreadsheetId, worksheetEntry),
                fileSource,
                limit,
                options,
                exceptions
            );
            setProgress(job, fileSource, 100);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            exceptions.add(e);
        }
    }
    
    static private void setProgress(ImportingJob job, String fileSource, int percent) {
        job.setProgress(percent, "Reading " + fileSource);
    }
    
    static private class WorksheetBatchRowReader implements TableDataReader {
        final ImportingJob job;
        final String fileSource;
        
        final Sheets service;
        final String spreadsheetId;
        final Sheet worksheet;
        
        private int indexRow = 0;
        private List<List<Object>> rowsOfCells = null;
        
        public WorksheetBatchRowReader(ImportingJob job, String fileSource,
                Sheets service, String spreadsheetId, Sheet worksheet) {
            this.job = job;
            this.fileSource = fileSource;
            this.service = service;
            this.spreadsheetId = spreadsheetId;
            this.worksheet = worksheet;
        }
        
        @Override
        public List<Object> getNextRowOfCells() throws IOException {
            if (rowsOfCells == null) {
                rowsOfCells = getRowsOfCells(
                    service,
                    spreadsheetId,
                    worksheet);
            }
            
            if (rowsOfCells == null) {
                return null;
            }
            
            if (rowsOfCells.size() > 0) {
                setProgress(job, fileSource, 100 * indexRow / rowsOfCells.size());
            } else {
                setProgress(job, fileSource, 100);
            }
            
            if (indexRow < rowsOfCells.size()) {
                return rowsOfCells.get(indexRow++);
            } else {
                return null;
            }
        }
        
        List<List<Object>> getRowsOfCells(
            Sheets service,
            String spreadsheetId,
            Sheet worksheet
        ) throws IOException {
            String range = worksheet.getProperties().getTitle();
            ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();
            
            rowsOfCells = result.getValues();
            
            return rowsOfCells;
        }
        
    }
}
    
