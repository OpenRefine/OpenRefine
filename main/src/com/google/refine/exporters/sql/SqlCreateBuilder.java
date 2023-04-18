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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.JSONUtilities;

public class SqlCreateBuilder {

    private final static Logger logger = LoggerFactory.getLogger("SqlCreateBuilder");

    private String table;
    private List<String> columns;
    private JsonNode options;

    public SqlCreateBuilder(String table, List<String> columns, JsonNode sqlOptions) {
        this.table = table;
        this.columns = columns;
        this.options = sqlOptions;

    }

    public String getCreateSQL() {
        if (logger.isDebugEnabled()) {
            logger.debug("Create SQL with columns: {}", columns);
        }
        StringBuffer createSB = new StringBuffer();

        List<JsonNode> columnOptionArray = options == null ? Collections.emptyList() : JSONUtilities.getArray(options, "columns");
        boolean trimColNames = options == null ? false : JSONUtilities.getBoolean(options, "trimColumnNames", false);

        int count = columnOptionArray.size();

        for (int i = 0; i < count; i++) {
            JsonNode columnOptions = columnOptionArray.get(i);
            if (columnOptions != null) {
                String name = JSONUtilities.getString(columnOptions, "name", null);
                String type = JSONUtilities.getString(columnOptions, "type", SqlData.SQL_TYPE_VARCHAR);
                String size = JSONUtilities.getString(columnOptions, "size", "");
                boolean allowNull = JSONUtilities.getBoolean(columnOptions, "allowNull", true);
                String defaultValue = JSONUtilities.getString(columnOptions, "defaultValue", null);
                logger.debug("allowNull::{}", allowNull);

                String allowNullStr = "NULL";
                if (!allowNull) {
                    allowNullStr = "NOT NULL";
                }

                if (name != null) {
                    if (trimColNames) {
                        String trimmedCol = name.replaceAll("[^a-zA-Z0-9_]", "_");
                        createSB.append(trimmedCol + " ");
                    } else {
                        createSB.append(name + " ");
                    }

                    if (type.equals(SqlData.SQL_TYPE_VARCHAR)) {
                        if (size.isEmpty()) {
                            size = "255";
                        }
                        createSB.append(type + "(" + size + ")");

                    } else if (type.equals(SqlData.SQL_TYPE_CHAR)) {
                        if (size.isEmpty()) {
                            size = "10";
                        }
                        createSB.append(type + "(" + size + ")");

                    } else if (type.equals(SqlData.SQL_TYPE_INT) || type.equals(SqlData.SQL_TYPE_INTEGER)) {
                        if (size.isEmpty()) {
                            createSB.append(type);
                        } else {
                            createSB.append(type + "(" + size + ")");
                        }

                    } else if (type.equals(SqlData.SQL_TYPE_NUMERIC)) {
                        if (size.isEmpty()) {
                            createSB.append(type);
                        } else {
                            createSB.append(type + "(" + size + ")");
                        }
                    } else {
                        createSB.append(type);
                    }

                    createSB.append(" " + allowNullStr);
                    if (defaultValue != null && !defaultValue.isEmpty()) {
                        if (type.equals(SqlData.SQL_TYPE_VARCHAR) || type.equals(SqlData.SQL_TYPE_CHAR)
                                || type.equals(SqlData.SQL_TYPE_TEXT)) {
                            createSB.append(" DEFAULT " + "'" + defaultValue + "'");
                        } else {
                            try {
                                Integer.parseInt(defaultValue);
                            } catch (NumberFormatException nfe) {
                                throw new SqlExporterException(defaultValue + " is not compatible with column type :" + type);
                            }
                            createSB.append(" DEFAULT " + defaultValue);
                        }

                    }

                    if (i < count - 1) {
                        createSB.append(",");
                    }
                    createSB.append("\n");
                }
            }
        }

        StringBuffer sql = new StringBuffer();

        boolean includeDrop = JSONUtilities.getBoolean(options, "includeDropStatement", false);
        boolean addIfExist = JSONUtilities.getBoolean(options, "includeIfExistWithDropStatement", true);
        if (includeDrop) {
            if (addIfExist) {
                sql.append("DROP TABLE IF EXISTS " + table + ";\n");
            } else {
                sql.append("DROP TABLE " + table + ";\n");
            }

        }

        sql.append("CREATE TABLE ").append(table);
        sql.append(" (").append("\n");
        sql.append(createSB.toString());
        sql.append(")").append(";" + "\n");

        String createSQL = sql.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("Create SQL Generated Successfully...{}", createSQL);
        }
        return createSQL;
    }

}
