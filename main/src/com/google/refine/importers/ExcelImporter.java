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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class ExcelImporter extends TabularImportingParserBase {
    static final Logger logger = LoggerFactory.getLogger(ExcelImporter.class);
    
    public ExcelImporter() {
        super(true);
    }
    
    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        ArrayNode sheetRecords = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.safePut(options, "sheetRecords", sheetRecords);
        try {
            for (int index = 0;index < fileRecords.size();index++) {
                ObjectNode fileRecord = fileRecords.get(index);
                File file = ImportingUtilities.getFile(job, fileRecord);

                Workbook wb = null;
                try {
                    wb = FileMagic.valueOf(file) == FileMagic.OOXML ? new XSSFWorkbook(file) :
                                 new HSSFWorkbook(new POIFSFileSystem(file));

                    int sheetCount = wb.getNumberOfSheets();
                    for (int i = 0; i < sheetCount; i++) {
                        Sheet sheet = wb.getSheetAt(i);
                        int rows = sheet.getLastRowNum() - sheet.getFirstRowNum() + 1;

                        ObjectNode sheetRecord = ParsingUtilities.mapper.createObjectNode();
                        JSONUtilities.safePut(sheetRecord, "name",  file.getName() + "#" + sheet.getSheetName());
                        JSONUtilities.safePut(sheetRecord, "fileNameAndSheetIndex", file.getName() + "#" + i);
                        JSONUtilities.safePut(sheetRecord, "rows", rows);
                        if (rows > 1) {
                            JSONUtilities.safePut(sheetRecord, "selected", true);
                        } else {
                            JSONUtilities.safePut(sheetRecord, "selected", false);
                        }
                        JSONUtilities.append(sheetRecords, sheetRecord);
                    }
                } finally {
                    if (wb != null) {
                        wb.close();
                    }
                }
            }                
        } catch (IOException e) {
            logger.error("Error generating parser UI initialization data for Excel file", e);
        } catch (IllegalArgumentException e) {
            logger.error("Error generating parser UI initialization data for Excel file (only Excel 97 & later supported)", e);
        } catch (POIXMLException|InvalidFormatException e) {
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
        ObjectNode options,
        List<Exception> exceptions
    ) {
        Workbook wb = null;
        if (!inputStream.markSupported()) {
          inputStream = new BufferedInputStream(inputStream);
        }
        
        try {
            wb = FileMagic.valueOf(inputStream) == FileMagic.OOXML ?
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
        
        ArrayNode sheets = (ArrayNode) options.get("sheets");
        
        for(int i=0;i<sheets.size();i++)  {
            String[] fileNameAndSheetIndex = new String[2];
            ObjectNode sheetObj = (ObjectNode) sheets.get(i);
            // value is fileName#sheetIndex
            fileNameAndSheetIndex = sheetObj.get("fileNameAndSheetIndex").asText().split("#");
            
            if (!fileNameAndSheetIndex[0].equals(fileSource))
                continue;
            
            final Sheet sheet = wb.getSheetAt(Integer.parseInt(fileNameAndSheetIndex[1]));
            final int lastRow = sheet.getLastRowNum();
            
            TableDataReader dataReader = new TableDataReader() {
                int nextRow = 0;
                
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
                                cell = extractCell(sourceCell);
                            }
                            cells.add(cell);
                        }
                    }
                    return cells;
                }
            };
            
            // TODO: Do we need to preserve the original filename? Take first piece before #?
//           JSONUtilities.safePut(options, "fileSource", fileSource + "#" + sheet.getSheetName());
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
    
    static protected Cell extractCell(org.apache.poi.ss.usermodel.Cell cell) {
        CellType cellType = cell.getCellType();
        if (cellType.equals(CellType.FORMULA)) {
            cellType = cell.getCachedFormulaResultType();
        }
        if (cellType.equals(CellType.ERROR) ||
            cellType.equals(CellType.BLANK)) {
            return null;
        }
        
        Serializable value = null;
        if (cellType.equals(CellType.BOOLEAN)) {
            value = cell.getBooleanCellValue();
        } else if (cellType.equals(CellType.NUMERIC)) {
            double d = cell.getNumericCellValue();
            
            if (DateUtil.isCellDateFormatted(cell)) {
                value = ParsingUtilities.toDate(DateUtil.getJavaDate(d));
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

        return new Cell(value, null);
    }
}
