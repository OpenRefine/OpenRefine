/*
 * Copyright (c) 2018, Tony Opara
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

package com.google.refine.exporters.sql;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.exporters.TabularSerializer;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class SqlExporter implements WriterExporter {

    private static final Logger logger = LoggerFactory.getLogger("SqlExporter");
    public static final String NO_COL_SELECTED_ERROR = "****NO COLUMNS SELECTED****";
    public static final String NO_OPTIONS_PRESENT_ERROR = "****NO OPTIONS PRESENT****";
    // JSON Property names
    public static final String JSON_INCLUDE_STRUCTURE = "includeStructure";
    public static final String JSON_INCLUDE_CONTENT = "includeContent";
    public static final String JSON_TABLE_NAME = "tableName";

    private List<String> columnNames = new ArrayList<String>();
    private List<ArrayList<SqlData>> sqlDataList = new ArrayList<ArrayList<SqlData>>();
    private JsonNode sqlOptions;

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, final Writer writer)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("export sql with params: {}", params);
        }

        TabularSerializer serializer = new TabularSerializer() {

            @Override
            public void startFile(JsonNode options) {
                sqlOptions = options;
                // logger.info("setting options::{}", sqlOptions);
            }

            @Override
            public void endFile() {
                try {
                    if (columnNames.isEmpty()) {
                        logger.error("No Columns Selected!!");
                        throw new SqlExporterException(NO_COL_SELECTED_ERROR);

                    }
                    if (sqlOptions == null) {
                        logger.error("No Options Selected!!");
                        throw new SqlExporterException(NO_OPTIONS_PRESENT_ERROR);

                    }
                    String tableName = ProjectManager.singleton.getProjectMetadata(project.id).getName();

                    String tableNameManual = JSONUtilities.getString(sqlOptions, JSON_TABLE_NAME, null);

                    if (tableNameManual != null) {
                        tableName = tableNameManual;
                    }

                    SqlCreateBuilder createBuilder = new SqlCreateBuilder(tableName, columnNames, sqlOptions);
                    SqlInsertBuilder insertBuilder = new SqlInsertBuilder(tableName, columnNames, sqlDataList,
                            sqlOptions);

                    final boolean includeStructure = sqlOptions == null ? true
                            : JSONUtilities.getBoolean(sqlOptions, JSON_INCLUDE_STRUCTURE, true);

                    final boolean includeContent = sqlOptions == null ? true
                            : JSONUtilities.getBoolean(sqlOptions, JSON_INCLUDE_CONTENT, true);

                    if (includeStructure) {
                        String sqlCreateStr = createBuilder.getCreateSQL();
                        writer.write(sqlCreateStr);
                    }

                    if (includeContent) {
                        String sqlInsertStr = insertBuilder.getInsertSQL();
                        writer.write(sqlInsertStr);
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("sqlOptions::{}", sqlOptions);
                    }

                    columnNames = new ArrayList<String>();
                    sqlDataList = new ArrayList<ArrayList<SqlData>>();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {

                if (isHeader) {
                    for (CellData cellData : cells) {
                        columnNames.add(cellData.text);
                    }

                } else {
                    ArrayList<SqlData> values = new ArrayList<>();
                    for (CellData cellData : cells) {

                        if (cellData != null) {
                            if (cellData.text == null || cellData.text.isEmpty()) {
                                values.add(new SqlData(cellData.columnName, "", ""));
                            } else {
                                values.add(new SqlData(cellData.columnName, cellData.value, cellData.text));
                            }

                        }

                    }
                    sqlDataList.add(values);
                }

            }
        };

        CustomizableTabularExporterUtilities.exportRows(project, engine, params, serializer);
    }

}
