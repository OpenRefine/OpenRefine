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
import java.io.Writer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CsvExporter implements WriterExporter{

    final static Logger logger = LoggerFactory.getLogger("CsvExporter");
    char separator;

    public CsvExporter() {
        separator = ','; //Comma separated-value is default
    }

    public CsvExporter(char separator) {
        this.separator = separator;
    }

    @Override
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        boolean printColumnHeader = true;

        if (options != null && options.getProperty("printColumnHeader") != null) {
            printColumnHeader = Boolean.parseBoolean(options.getProperty("printColumnHeader"));
        }

        RowVisitor visitor = new RowVisitor() {
            CSVWriter csvWriter;
            boolean printColumnHeader = true;
            boolean isFirstRow = true; //the first row should also add the column headers

            public RowVisitor init(CSVWriter writer, boolean printColumnHeader) {
                this.csvWriter = writer;
                this.printColumnHeader = printColumnHeader;
                return this;
            }

            public boolean visit(Project project, int rowIndex, Row row) {
                int size = project.columnModel.columns.size();

                String[] cols = new String[size];
                String[] vals = new String[size];

                int i = 0;
                for (Column col : project.columnModel.columns) {
                    int cellIndex = col.getCellIndex();
                    cols[i] = col.getName();

                    Object value = row.getCellValue(cellIndex);
                    if (value != null) {
                        vals[i] = value instanceof String ? (String) value : value.toString();
                    }
                    i++;
                }

                if (printColumnHeader && isFirstRow) {
                    csvWriter.writeNext(cols,false);
                    isFirstRow = false; //switch off flag
                }
                csvWriter.writeNext(vals,false);

                return false;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                try {
                    csvWriter.close();
                } catch (IOException e) {
                    logger.error("CsvExporter could not close writer : " + e.getMessage());
                }
            }

        }.init(new CSVWriter(writer, separator), printColumnHeader);

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
    }

    @Override
    public String getContentType() {
        return "application/x-unknown";
    }

}
