/*
 * Copyright (c) 2010, 2013 Thomas F. Morris
 *               2018, 2019 OpenRefine contributors
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * Neither the name of Google nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
                    exceptions);
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
                    exceptions);
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
                Sheet worksheet) throws IOException {
            String range = worksheet.getProperties().getTitle();
            ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();

            rowsOfCells = result.getValues();

            return rowsOfCells;
        }

    }
}
