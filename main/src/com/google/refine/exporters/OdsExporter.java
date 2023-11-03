/*

Copyright 2011, Thomas F. Morris
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class OdsExporter implements StreamExporter {

    @Override
    public String getContentType() {
        return "application/vnd.oasis.opendocument.spreadsheet";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine,
            OutputStream outputStream) throws IOException {

        final OdfSpreadsheetDocument odfDoc;
        try {
            odfDoc = OdfSpreadsheetDocument.newSpreadsheetDocument();
        } catch (Exception e) {
            throw new IOException("Failed to create spreadsheet", e);
        }

        TabularSerializer serializer = new TabularSerializer() {

            OdfTable table;
            // int rowCount = 0;
            int rowBeforeHeader = 0;

            @Override
            public void startFile(JsonNode options) {
                table = OdfTable.newTable(odfDoc);
                String tableName = ProjectManager.singleton.getProjectMetadata(project.id).getName();

                // the ODF document might already contain some other tables
                try {
                    table.setTableName(tableName);
                } catch (IllegalArgumentException e) {
                    // there is already a table with that name
                    table = odfDoc.getTableByName(tableName);
                }
                // delete any other table which has another name
                odfDoc.getTableList().stream()
                        .filter(table -> !table.getTableName().equals(tableName))
                        .forEach(OdfTable::remove);
                rowBeforeHeader = table.getRowCount();
            }

            @Override
            public void endFile() {
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                OdfTableRow r = table.appendRow();
                // rowCount++;

                for (int i = 0; i < cells.size(); i++) {
                    OdfTableCell c = r.getCellByIndex(i); // implicitly creates cell
                    CellData cellData = cells.get(i);

                    if (cellData != null && cellData.text != null && cellData.value != null) {
                        Object v = cellData.value;
                        if (v instanceof Number) {
                            c.setDoubleValue(((Number) v).doubleValue());
                        } else if (v instanceof Boolean) {
                            c.setBooleanValue(((Boolean) v).booleanValue());
                        } else if (v instanceof OffsetDateTime) {
                            OffsetDateTime odt = (OffsetDateTime) v;
                            c.setDateValue(ParsingUtilities.offsetDateTimeToCalendar(odt));
                        } else {
                            c.setStringValue(cellData.text);
                        }

                        if (cellData.link != null) {
                            // TODO: How do we do output hyperlinks?
                        }
                    }
                }
                if (rowBeforeHeader != 0) { // avoid the api change the default values again
                    int nowRows = table.getRowCount();
                    table.removeRowsByIndex(0, rowBeforeHeader);
                    rowBeforeHeader -= nowRows - table.getRowCount();
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);

        try {
            odfDoc.save(outputStream);
        } catch (Exception e) {
            throw new IOException("Error saving spreadsheet", e);
        }
        outputStream.flush();
    }

}
