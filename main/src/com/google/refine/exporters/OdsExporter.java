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
import java.util.Calendar;

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;

import com.google.refine.model.Project;

public class OdsExporter extends AbstractSpreadsheetExporter<OdsExporter.OdsState> {

    /**
     * State of one export. It is not stored in the exporter itself because the exporter is shared by all requests.
     */
    static final class OdsState {

        final OdfSpreadsheetDocument odfDoc;

        OdfTable table;
        OdfTableRow row;
        OdfTableCell cell;
        int rowBeforeHeader = 0;

        OdsState(OdfSpreadsheetDocument odfDoc) {
            this.odfDoc = odfDoc;
        }
    }

    @Override
    public String getContentType() {
        return "application/vnd.oasis.opendocument.spreadsheet";
    }

    @Override
    protected OdsState createState(Project project) throws IOException {
        try {
            return new OdsState(OdfSpreadsheetDocument.newSpreadsheetDocument());
        } catch (Exception e) {
            throw new IOException("Failed to create spreadsheet", e);
        }
    }

    @Override
    protected void startSheet(OdsState state, String tableName) {
        state.table = OdfTable.newTable(state.odfDoc);

        // the ODF document might already contain some other tables
        try {
            state.table.setTableName(tableName);
        } catch (IllegalArgumentException e) {
            // there is already a table with that name
            state.table = state.odfDoc.getTableByName(tableName);
        }
        // delete any other table which has another name
        state.odfDoc.getTableList().stream()
                .filter(table -> !table.getTableName().equals(tableName))
                .forEach(OdfTable::remove);
        state.rowBeforeHeader = state.table.getRowCount();
    }

    @Override
    protected void startRow(OdsState state) {
        state.row = state.table.appendRow();
    }

    @Override
    protected void startCell(OdsState state, int column, int columnCount) {
        state.cell = state.row.getCellByIndex(column); // implicitly creates cell
    }

    @Override
    protected void writeNumber(OdsState state, double value) {
        state.cell.setDoubleValue(value);
    }

    @Override
    protected void writeBoolean(OdsState state, boolean value) {
        state.cell.setBooleanValue(value);
    }

    @Override
    protected void writeDate(OdsState state, Calendar value) {
        state.cell.setDateValue(value);
    }

    @Override
    protected void writeText(OdsState state, int column, String text) {
        state.cell.setStringValue(text);
    }

    // TODO: How do we do output hyperlinks? Until then writeLink keeps the default behaviour of ignoring them.

    @Override
    protected void endRow(OdsState state) {
        if (state.rowBeforeHeader != 0) { // avoid the api change the default values again
            int nowRows = state.table.getRowCount();
            state.table.removeRowsByIndex(0, state.rowBeforeHeader);
            state.rowBeforeHeader -= nowRows - state.table.getRowCount();
        }
    }

    @Override
    protected void writeDocument(OdsState state, OutputStream outputStream) throws IOException {
        try {
            state.odfDoc.save(outputStream);
        } catch (Exception e) {
            throw new IOException("Error saving spreadsheet", e);
        }
    }

}