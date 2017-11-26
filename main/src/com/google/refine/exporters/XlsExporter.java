/*

Copyright 2010,2011 Google Inc.
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
import java.util.List;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

public class XlsExporter implements StreamExporter {
    final private boolean xml;
    
    public XlsExporter(boolean xml) {
        this.xml = xml;
    }
    
    @Override
    public String getContentType() {
        return xml ? "application/xlsx" : "application/xls";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine,
            OutputStream outputStream) throws IOException {

        final Workbook wb = xml ? new XSSFWorkbook() : new HSSFWorkbook();
        
        TabularSerializer serializer = new TabularSerializer() {
            Sheet s;
            int rowCount = 0;
            CellStyle dateStyle;
            
            @Override
            public void startFile(JSONObject options) {
                s = wb.createSheet();
                String sheetName = WorkbookUtil.createSafeSheetName(
                        ProjectManager.singleton.getProjectMetadata(project.id).getName());
                wb.setSheetName(0, sheetName);

                dateStyle = wb.createCellStyle();
                dateStyle.setDataFormat(
                        wb.getCreationHelper().createDataFormat().getFormat("YYYY-MM-DD")); // TODO what format here?
            }

            @Override
            public void endFile() {
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                Row r = s.createRow(rowCount++);
                
                for (int i = 0; i < cells.size(); i++) {
                    Cell c = r.createCell(i);
                    if (i == 255 && cells.size() > 256) {
                        c.setCellValue("ERROR: TOO MANY COLUMNS");
                        break;
                    } else {
                        CellData cellData = cells.get(i);
                        
                        if (cellData != null && cellData.text != null && cellData.value != null) {
                            Object v = cellData.value;
                            if (v instanceof Number) {
                                c.setCellValue(((Number) v).doubleValue());
                            } else if (v instanceof Boolean) {
                                c.setCellValue(((Boolean) v).booleanValue());
                            } else if (v instanceof Date) {
                                c.setCellValue((Date) v);
                                c.setCellStyle(dateStyle);
                            } else if (v instanceof Calendar) {
                                c.setCellValue((Calendar) v);
                                c.setCellStyle(dateStyle);
                            } else {
                                String s = cellData.text;
                                if (s.length() > 32767) {
                                    // The maximum length of cell contents (text) is 32,767 characters
                                    s = s.substring(0, 32767);
                                }
                                c.setCellValue(s);
                            }
                            
                            if (cellData.link != null) {
                                HSSFHyperlink hl = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
                                hl.setLabel(cellData.text);
                                hl.setAddress(cellData.link);
                            }
                        }

                    }
                }
            }
        };
        
        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);
        
        wb.write(outputStream);
        outputStream.flush();
    }

}
