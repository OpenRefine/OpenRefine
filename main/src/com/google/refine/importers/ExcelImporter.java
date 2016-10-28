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

package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLException;
import org.apache.poi.common.usermodel.Hyperlink;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconCandidate;
import com.google.refine.util.JSONUtilities;

public class ExcelImporter extends TabularImportingParserBase {
    static final Logger logger = LoggerFactory.getLogger(ExcelImporter.class);
    
    public ExcelImporter() {
        super(true);
    }
    
    @Override
    public JSONObject createParserUIInitializationData(
            ImportingJob job, List<JSONObject> fileRecords, String format) {
        JSONObject options = super.createParserUIInitializationData(job, fileRecords, format);

        JSONArray sheetRecords = new JSONArray();
        JSONUtilities.safePut(options, "sheetRecords", sheetRecords);
        try {
            if (fileRecords.size() > 0) {
                JSONObject firstFileRecord = fileRecords.get(0);
                File file = ImportingUtilities.getFile(job, firstFileRecord);
                InputStream is = new FileInputStream(file);

                if (!is.markSupported()) {
                  is = new PushbackInputStream(is, 8);
                }

                try {
                    Workbook wb = POIXMLDocument.hasOOXMLHeader(is) ?
                            new XSSFWorkbook(is) :
                                new HSSFWorkbook(new POIFSFileSystem(is));

                            int sheetCount = wb.getNumberOfSheets();
                            boolean hasData = false;
                            for (int i = 0; i < sheetCount; i++) {
                                Sheet sheet = wb.getSheetAt(i);
                                int rows = sheet.getLastRowNum() - sheet.getFirstRowNum() + 1;

                                JSONObject sheetRecord = new JSONObject();
                                JSONUtilities.safePut(sheetRecord, "name", sheet.getSheetName());
                                JSONUtilities.safePut(sheetRecord, "rows", rows);
                                if (hasData) {
                                    JSONUtilities.safePut(sheetRecord, "selected", false);
                                } else if (rows > 1) {
                                    JSONUtilities.safePut(sheetRecord, "selected", true);
                                    hasData = true;
                                }
                                JSONUtilities.append(sheetRecords, sheetRecord);
                            }
                } finally {
                    is.close();
                }
            }                
        } catch (IOException e) {
            logger.error("Error generating parser UI initialization data for Excel file", e);
        } catch (IllegalArgumentException e) {
            logger.error("Error generating parser UI initialization data for Excel file (only Excel 97 & later supported)", e);
        } catch (POIXMLException e) {
            logger.error("Error generating parser UI initialization data for Excel file - invalid XML", e);
        }
        
        return options;
    }
    
    @Override
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        InputStream inputStream,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) {
        Workbook wb = null;
        if (!inputStream.markSupported()) {
          inputStream = new PushbackInputStream(inputStream, 8);
        }
        
        try {
            wb = POIXMLDocument.hasOOXMLHeader(inputStream) ?
                new XSSFWorkbook(inputStream) :
                new HSSFWorkbook(new POIFSFileSystem(inputStream));
        } catch (IOException e) {
            exceptions.add(new ImportException(
                "Attempted to parse as an Excel file but failed. " +
                "Try to use Excel to re-save the file as a different Excel version or as TSV and upload again.",
                e
            ));
            return;
        } catch (ArrayIndexOutOfBoundsException e){
            exceptions.add(new ImportException(
               "Attempted to parse file as an Excel file but failed. " +
               "This is probably caused by a corrupt excel file, or due to the file having previously been created or saved by a non-Microsoft application. " +
               "Please try opening the file in Microsoft Excel and resaving it, then try re-uploading the file. " +
               "See https://issues.apache.org/bugzilla/show_bug.cgi?id=48261 for further details",
               e
           ));
            return;
        } catch (IllegalArgumentException e) {
            exceptions.add(new ImportException(
                    "Attempted to parse as an Excel file but failed. " +
                    "Only Excel 97 and later formats are supported.",
                    e
                ));
                return;
        } catch (POIXMLException e) {
            exceptions.add(new ImportException(
                    "Attempted to parse as an Excel file but failed. " +
                    "Invalid XML.",
                    e
                ));
                return;
        }
        
        int[] sheets = JSONUtilities.getIntArray(options, "sheets");
        for (int sheetIndex : sheets) {
            final Sheet sheet = wb.getSheetAt(sheetIndex);
            final int lastRow = sheet.getLastRowNum();
            
            TableDataReader dataReader = new TableDataReader() {
                int nextRow = 0;
                Map<String, Recon> reconMap = new HashMap<String, Recon>();
                
                @Override
                public List<Object> getNextRowOfCells() throws IOException {
                    if (nextRow > lastRow) {
                        return null;
                    }
                    
                    List<Object> cells = new ArrayList<Object>();
                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(nextRow++);
                    if (row != null) {
                        short lastCell = row.getLastCellNum();
                        for (short cellIndex = 0; cellIndex < lastCell; cellIndex++) {
                            Cell cell = null;
                            
                            org.apache.poi.ss.usermodel.Cell sourceCell = row.getCell(cellIndex);
                            if (sourceCell != null) {
                                cell = extractCell(sourceCell, reconMap);
                            }
                            cells.add(cell);
                        }
                    }
                    return cells;
                }
            };
            
            TabularImportingParserBase.readTable(
                project,
                metadata,
                job,
                dataReader,
                fileSource + "#" + sheet.getSheetName(),
                limit,
                options,
                exceptions
            );
        }
    }
    
    static protected Serializable extractCell(org.apache.poi.ss.usermodel.Cell cell) {
        int cellType = cell.getCellType();
        if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR ||
            cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK) {
            return null;
        }
        
        Serializable value = null;
        if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN) {
            value = cell.getBooleanCellValue();
        } else if (cellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC) {
            double d = cell.getNumericCellValue();
            
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                value = HSSFDateUtil.getJavaDate(d);
                // TODO: If we had a time datatype, we could use something like the following
                // to distinguish times from dates (although Excel doesn't really make the distinction)
                // Another alternative would be to look for values < 0.60
//                String format = cell.getCellStyle().getDataFormatString();
//                if (!format.contains("d") && !format.contains("m") && !format.contains("y") ) {
//                    // It's just a time
//                }
            } else {
                value = d;
            }
        } else {
            String text = cell.getStringCellValue();
            if (text.length() > 0) {
                value = text;
            }
        }
        
        return value;
    }
    
    static protected Cell extractCell(org.apache.poi.ss.usermodel.Cell cell, Map<String, Recon> reconMap) {
        Serializable value = extractCell(cell);
        
        if (value != null) {
            Recon recon = null;
            
            Hyperlink hyperlink = cell.getHyperlink();
            if (hyperlink != null) {
                String url = hyperlink.getAddress();
                
                if (url != null && (url.startsWith("http://") ||
                    url.startsWith("https://"))) {
                    
                    final String sig = "freebase.com/view";
                    
                    int i = url.indexOf(sig);
                    if (i > 0) {
                        String id = url.substring(i + sig.length());
                        
                        int q = id.indexOf('?');
                        if (q > 0) {
                            id = id.substring(0, q);
                        }
                        int h = id.indexOf('#');
                        if (h > 0) {
                            id = id.substring(0, h);
                        }
                        
                        if (reconMap.containsKey(id)) {
                            recon = reconMap.get(id);
                            recon.judgmentBatchSize++;
                        } else {
                            recon = new Recon(0, null, null);
                            recon.service = "import";
                            recon.match = new ReconCandidate(id, value.toString(), new String[0], 100);
                            recon.matchRank = 0;
                            recon.judgment = Judgment.Matched;
                            recon.judgmentAction = "auto";
                            recon.judgmentBatchSize = 1;
                            recon.addCandidate(recon.match);
                            
                            reconMap.put(id, recon);
                        }
                        
                    }
                }
            }
            
            return new Cell(value, recon);
        } else {
            return null;
        }
    }
}
