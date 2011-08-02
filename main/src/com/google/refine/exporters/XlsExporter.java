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

package com.google.refine.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class XlsExporter implements StreamExporter {

    @Override
    public String getContentType() {
        return "application/xls";
    }

    @Override
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {

        Workbook wb = new HSSFWorkbook();
        Sheet s = wb.createSheet();
        wb.setSheetName(0, ProjectManager.singleton.getProjectMetadata(project.id).getName());
        
        int rowCount = 0;
        
        {
            org.apache.poi.ss.usermodel.Row r = s.createRow(rowCount++);
            
            int cellCount = 0;
            for (Column column : project.columnModel.columns) {
                if (cellCount++ > 255) {
                    // TODO: Warn user about truncated data
                } else {
                    org.apache.poi.ss.usermodel.Cell c = r.createCell(cellCount);
                    c.setCellValue(column.getName());
                }
            }
        }
        
        {
            RowVisitor visitor = new RowVisitor() {
                Sheet sheet;
                int rowCount;
                
                public RowVisitor init(Sheet sheet, int rowCount) {
                    this.sheet = sheet;
                    this.rowCount = rowCount;
                    return this;
                }
                
                @Override
                public void start(Project project) {
                    // nothing to do
                }

                @Override
                public void end(Project project) {
                    // nothing to do
                }
                
                @Override
                public boolean visit(Project project, int rowIndex, Row row) {
                    org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowCount++);
                    
                    int cellCount = 0;
                    for (Column column : project.columnModel.columns) {
                        if (cellCount++ > 255) {
                            // TODO: Warn user about truncated data
                        } else {
                            org.apache.poi.ss.usermodel.Cell c = r.createCell(cellCount);

                            int cellIndex = column.getCellIndex();
                            if (cellIndex < row.cells.size()) {
                                Cell cell = row.cells.get(cellIndex);
                                if (cell != null) {
                                    if (cell.recon != null && cell.recon.match != null) {
                                        c.setCellValue(cell.recon.match.name);

                                        HSSFHyperlink hl = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
                                        hl.setLabel(cell.recon.match.name);
                                        hl.setAddress("http://www.freebase.com/view" + cell.recon.match.id);

                                        c.setHyperlink(hl);
                                    } else if (cell.value != null) {
                                        Object v = cell.value;

                                        if (v instanceof Number) {
                                            c.setCellValue(((Number) v).doubleValue());
                                        } else if (v instanceof Boolean) {
                                            c.setCellValue(((Boolean) v).booleanValue());
                                        } else if (v instanceof Date) {
                                            c.setCellValue((Date) v);
                                        } else if (v instanceof Calendar) {
                                            c.setCellValue((Calendar) v);
                                        } else if (v instanceof String) {
                                            String s = (String) v;
                                            if (s.length() > 32767) {
                                                // The maximum length of cell contents (text) is 32,767 characters
                                                s = s.substring(0, 32767);
                                            }
                                            c.setCellValue(s);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return false;
                }
            }.init(s, rowCount);
            
            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, visitor);
        }
        
        wb.write(outputStream);
        outputStream.flush();
    }

}
