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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.poi.common.usermodel.Hyperlink;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.Recon.Judgment;

public class ExcelImporter implements StreamImporter {
    protected boolean _xmlBased;

    @Override
    public void read(InputStream inputStream, Project project, ProjectMetadata metadata, Properties options) throws ImportException {
        int ignoreLines = ImporterUtilities.getIntegerOption("ignore", options, -1);
        int headerLines = ImporterUtilities.getIntegerOption("header-lines", options, 1);
        int limit = ImporterUtilities.getIntegerOption("limit", options, -1);
        int skip = ImporterUtilities.getIntegerOption("skip", options, 0);

        Workbook wb = null;
        try {
            wb = _xmlBased ?
                new XSSFWorkbook(inputStream) :
                new HSSFWorkbook(new POIFSFileSystem(inputStream));
        } catch (IOException e) {
            throw new ImportException(
                "Attempted to parse as an Excel file but failed. " +
                "Try to use Excel to re-save the file as a different Excel version or as TSV and upload again.",
                e
            );
        } catch (ArrayIndexOutOfBoundsException e){
            throw new ImportException(
                   "Attempted to parse file as an Excel file but failed. " +
                   "This is probably caused by a corrupt excel file, or due to the file having previously been created or saved by a non-Microsoft application. " +
                   "Please try opening the file in Microsoft Excel and resaving it, then try re-uploading the file. " +
                   "See https://issues.apache.org/bugzilla/show_bug.cgi?id=48261 for further details",
                   e);
        }
        
        Sheet sheet = wb.getSheetAt(0);
        
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        
        List<String>         columnNames = new ArrayList<String>();
        Set<String>          columnNameSet = new HashSet<String>();
        Map<String, Integer> columnRootNameToIndex = new HashMap<String, Integer>();
        
        int                  rowsWithData = 0;
        Map<String, Recon>   reconMap = new HashMap<String, Recon>();
        
        for (int r = firstRow; r <= lastRow; r++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            } else if (ignoreLines > 0) {
                ignoreLines--;
                continue;
            }
            
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell < 0 || firstCell > lastCell) {
                continue;
            }
            
            /*
             *  Still processing header lines
             */
            if (headerLines > 0) {
                headerLines--;
                
                for (int c = firstCell; c <= lastCell; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    if (cell != null) {
                        Serializable value = extractCell(cell);
                        String text = value != null ? value.toString() : null;
                        if (text != null && text.length() > 0) {
                            while (columnNames.size() < c + 1) {
                                columnNames.add(null);
                            }
                            
                            String existingName = columnNames.get(c);
                            String name = (existingName == null) ? text : (existingName + " " + text);
                            
                            columnNames.set(c, name);
                        }
                    }
                }
                
                if (headerLines == 0) {
                    for (int i = 0; i < columnNames.size(); i++) {
                        String rootName = columnNames.get(i);
                        if (rootName == null) {
                            continue;
                        }
                        setUnduplicatedColumnName(rootName, columnNames, i, columnNameSet, columnRootNameToIndex);
                    }
                }
                
            /*
             *  Processing data rows
             */
            } else {
                Row newRow = new Row(columnNames.size());
                boolean hasData = false;
                
                for (int c = firstCell; c <= lastCell; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    if (cell == null) {
                        continue;
                    }
                    
                    Cell ourCell = extractCell(cell, reconMap);
                    if (ourCell != null) {
                        while (columnNames.size() < c + 1) {
                            columnNames.add(null);
                        }
                        if (columnNames.get(c) == null) {
                            setUnduplicatedColumnName("Column", columnNames, c, columnNameSet, columnRootNameToIndex);
                        }
                        
                        newRow.setCell(c, ourCell);
                        hasData = true;
                    }
                }
                
                if (hasData) {
                    rowsWithData++;
                    
                    if (skip <= 0 || rowsWithData > skip) {
                        project.rows.add(newRow);
                        project.columnModel.setMaxCellIndex(newRow.cells.size());
                        
                        if (limit > 0 && project.rows.size() >= limit) {
                            break;
                        }
                    }
                }
            }
        }
        
        /*
         *  Create columns
         */
        for (int c = 0; c < columnNames.size(); c++) {
            String name = columnNames.get(c);
            if (name != null) {
                Column column = new Column(c, name);
                project.columnModel.columns.add(column);
            }
        }
    }
    
    protected void setUnduplicatedColumnName(
        String rootName, List<String> columnNames, int index, Set<String> columnNameSet, Map<String, Integer> columnRootNameToIndex) {
        if (columnNameSet.contains(rootName)) {
            int startIndex = columnRootNameToIndex.containsKey(rootName) ? columnRootNameToIndex.get(rootName) : 2;
            while (true) {
                String name = rootName + " " + startIndex;
                if (columnNameSet.contains(name)) {
                    startIndex++;
                } else {
                    columnNames.set(index, name);
                    columnNameSet.add(name);
                    break;
                }
            }
            
            columnRootNameToIndex.put(rootName, startIndex + 1);
        } else {
            columnNames.set(index, rootName);
            columnNameSet.add(rootName);
        }
    }
    
    protected Serializable extractCell(org.apache.poi.ss.usermodel.Cell cell) {
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
            } else {
                value = d;
            }
        } else {
            String text = cell.getStringCellValue().trim();
            if (text.length() > 0) {
                value = text;
            }
        }
        
        return value;
    }
    
    protected Cell extractCell(org.apache.poi.ss.usermodel.Cell cell, Map<String, Recon> reconMap) {
        Serializable value = extractCell(cell);
        
        if (value != null) {
            Recon recon = null;
            
            Hyperlink hyperlink = cell.getHyperlink();
            if (hyperlink != null) {
                String url = hyperlink.getAddress();
                
                if (url.startsWith("http://") ||
                    url.startsWith("https://")) {
                    
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
    
    @Override
    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
            if ("application/msexcel".equals(contentType) ||
                "application/x-msexcel".equals(contentType) ||
                "application/x-ms-excel".equals(contentType) ||
                "application/vnd.ms-excel".equals(contentType) ||
                "application/x-excel".equals(contentType) ||
                "application/xls".equals(contentType)) {
                this._xmlBased = false;
                return true;
            } else if("application/x-xls".equals(contentType)) {
                this._xmlBased = true;
                return true;
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".xls")) {
                this._xmlBased = false;
                return true;
            } else if (fileName.endsWith(".xlsx")) {
                this._xmlBased = true;
                return true;
            }
        }
        return false;
    }
}
