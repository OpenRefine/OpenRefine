
package org.openrefine.extension.gdata;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectMetadata;
import org.openrefine.importers.TabularParserHelper;
import org.openrefine.importers.TabularParserHelper.TableDataReader;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.util.JSONUtilities;

public class GDataImporter {

    static final Logger logger = LoggerFactory.getLogger("GDataImporter");

    static public GridState parse(
            DatamodelRunner runner,
            String token,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options) throws Exception {

        String docType = JSONUtilities.getString(options, "docType", null);
        if ("spreadsheet".equals(docType)) {
            Sheets service = GoogleAPIExtension.getSheetsService(token);
            return parse(
                    runner,
                    service,
                    metadata,
                    job,
                    limit,
                    options);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported docType \"%s\"", docType));
        }
    }

    static public GridState parse(
            DatamodelRunner runner,
            Sheets service,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options) throws Exception {

        String docUrlString = JSONUtilities.getString(options, "docUrl", null);
        String worksheetUrlString = JSONUtilities.getString(options, "sheetUrl", null);

        // the index of the worksheet
        int worksheetIndex = JSONUtilities.getInt(options, "worksheetIndex", 0);

        if (docUrlString != null && worksheetUrlString != null) {
            return parseOneWorkSheet(
                    runner,
                    service,
                    metadata,
                    job,
                    new URL(docUrlString),
                    worksheetIndex,
                    limit,
                    options);
        } else {
            throw new IllegalArgumentException("docUrl and sheetUrl are required");
        }
    }

    static public GridState parseOneWorkSheet(
            DatamodelRunner runner,
            Sheets service,
            ProjectMetadata metadata,
            final ImportingJob job,
            URL docURL,
            int worksheetIndex,
            int limit,
            ObjectNode options) throws Exception {

        String spreadsheetId = GoogleAPIExtension.extractSpreadSheetId(docURL.toString());

        Spreadsheet response = service.spreadsheets().get(spreadsheetId)
                .setIncludeGridData(true)
                .execute();
        Sheet worksheetEntry = response.getSheets().get(worksheetIndex);

        String spreadsheetName = docURL.toExternalForm();

        String fileSource = spreadsheetName + " # " +
                worksheetEntry.getProperties().getTitle();

        setProgress(job, fileSource, 0);
        TabularParserHelper tabularParsingHelper = new TabularParserHelper(runner);
        GridState grid = tabularParsingHelper.parseOneFile(
                metadata,
                job,
                fileSource,
                new WorksheetBatchRowReader(job, fileSource, service, spreadsheetId, worksheetEntry),
                limit,
                options);
        setProgress(job, fileSource, 100);
        return grid;
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
                Sheet worksheet) throws IOException {
            String range = worksheet.getProperties().getTitle();
            ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();

            rowsOfCells = result.getValues();

            return rowsOfCells;
        }

    }
}
