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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.JSONUtilities;

public class SqlInsertBuilder {

    private static final Logger logger = LoggerFactory.getLogger("SQLInsertBuilder");

    private String table;

    private List<String> columns;

    private List<ArrayList<SqlData>> sqlDataList;

    private JsonNode options;

    /**
     * 
     * @param table
     * @param columns
     * @param rows
     * @param sqlOptions
     */
    public SqlInsertBuilder(String table, List<String> columns, List<ArrayList<SqlData>> rows, JsonNode sqlOptions) {
        this.table = table;
        this.columns = columns;
        this.sqlDataList = rows;
        this.options = sqlOptions;
        // logger.info("Column Size:{}", columns.size());

    }

    /**
     * Get Insert Sql
     * 
     * @return
     */
    public String getInsertSQL() {
        if (logger.isDebugEnabled()) {
            logger.debug("Insert SQL with columns: {}", columns);
        }

        List<JsonNode> colOptionArray = options == null ? null : JSONUtilities.getArray(options, "columns");
        Map<String, JsonNode> colOptionsMap = new HashMap<>();
        if (colOptionArray != null) {
            colOptionArray.forEach(json -> {
                colOptionsMap.put(JSONUtilities.getString(json, "name", null), json);
            });
        }

        boolean nullValueNull = options == null ? true : JSONUtilities.getBoolean(options, "convertNulltoEmptyString", true);

        StringBuffer values = new StringBuffer();

        int idx = 0;
        for (ArrayList<SqlData> sqlRow : sqlDataList) {
            StringBuilder rowValue = new StringBuilder();

            // int fieldCount = 0;
            for (SqlData val : sqlRow) {

                JsonNode jsonOb = colOptionsMap.get(val.getColumnName());
                String type = JSONUtilities.getString(jsonOb, "type", null);

                String defaultValue = JSONUtilities.getString(jsonOb, "defaultValue", null);

                boolean allowNullChkBox = JSONUtilities.getBoolean(jsonOb, "defaultValue", true);
                ;
                if (type == null) {
                    type = SqlData.SQL_TYPE_VARCHAR;
                }
                // Character Types
                if (type.equals(SqlData.SQL_TYPE_VARCHAR) || type.equals(SqlData.SQL_TYPE_CHAR) || type.equals(SqlData.SQL_TYPE_TEXT)) {

                    if ((val.getText() == null || val.getText().isEmpty())) {

                        handleNullField(allowNullChkBox, defaultValue, nullValueNull, val.getColumnName(), rowValue, true);

                    } else {
                        rowValue.append("'" + val.getText().replace("'", "''") + "'");

                    }

                } else if (type.equals(SqlData.SQL_TYPE_INT) || type.equals(SqlData.SQL_TYPE_INTEGER)
                        || type.equals(SqlData.SQL_TYPE_NUMERIC)) {// Numeric Types : INT, NUMERIC

                    if ((val.getText() == null || val.getText().isEmpty())) {

                        handleNullField(allowNullChkBox, defaultValue, nullValueNull, val.getColumnName(), rowValue, false);

                    } else {// value not null

                        if (type.equals(SqlData.SQL_TYPE_NUMERIC)) {// test if number is numeric (decimal(p,s) number is
                                                                    // valid)

                            if (!NumberUtils.isCreatable(val.getText())) {
                                throw new SqlExporterException(
                                        val.getText() + " is not compatible with column type :" + type);
                            }
                        } else {

                            try { // number should be an integer
                                Integer.parseInt(val.getText());
                            } catch (NumberFormatException nfe) {
                                throw new SqlExporterException(
                                        val.getText() + " is not compatible with column type :" + type);
                            }

                        }

                        rowValue.append(val.getText());

                    }

                } else if (type.equals(SqlData.SQL_TYPE_DATE) || type.equals(SqlData.SQL_TYPE_TIMESTAMP)) {
                    if ((val.getText() == null || val.getText().isEmpty())) {
                        handleNullField(allowNullChkBox, defaultValue, nullValueNull, val.getColumnName(), rowValue, true);
                    } else {
                        rowValue.append("'" + val.getText() + "'");
                    }
                }

                rowValue.append(",");

            }

            idx++;
            String rowValString = rowValue.toString();
//            logger.info("rowValString::" + rowValString);
            rowValString = rowValString.substring(0, rowValString.length() - 1);

            values.append("( ");
            values.append(rowValString);
            values.append(" )");
            if (idx < sqlDataList.size()) {
                values.append(",");
            }
            values.append("\n");

        }

        boolean trimColNames = options == null ? false : JSONUtilities.getBoolean(options, "trimColumnNames", false);
        String colNamesWithSep = columns.stream().map(col -> col.replaceAll("[^a-zA-Z0-9_]", "_")).collect(Collectors.joining(","));
        if (!trimColNames) {
            colNamesWithSep = columns.stream().collect(Collectors.joining(","));
        }

        String valuesString = values.toString();
        valuesString = valuesString.substring(0, Integer.max(0, valuesString.length() - 1));

        StringBuffer sql = new StringBuffer();

        sql.append("INSERT INTO ").append(table);
        sql.append(" (");
        sql.append(colNamesWithSep);
        sql.append(") VALUES ").append("\n");
        sql.append(valuesString);

        String sqlString = sql.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("Insert Statement Generated Successfully...{}", sqlString);
        }
        return sqlString;
    }

    /**
     * 
     * @param allowNullChkBox
     * @param defaultValue
     * @param nullValueNull
     * @param col
     * @param rowValue
     * @param quote
     */
    public void handleNullField(
            boolean allowNullChkBox,
            String defaultValue,
            boolean nullValueNull,
            String col,
            StringBuilder rowValue,
            boolean quote) {

        if (allowNullChkBox) {// cell nullable
            if (defaultValue != null && !defaultValue.isEmpty()) {
                if (quote) {
                    rowValue.append("'" + defaultValue + "'");
                } else {
                    rowValue.append(defaultValue);
                }

            } else {
                if (nullValueNull) {
                    rowValue.append("null");

                } else {
                    throw new SqlExporterException("Null value not allowed for Field :" + col);
                }

            }

        } else {
            if (defaultValue != null && !defaultValue.isEmpty()) {
                if (quote) {
                    rowValue.append("'" + defaultValue + "'");
                } else {
                    rowValue.append(defaultValue);
                }

            } else {
                throw new SqlExporterException("Null value not allowed for Field :" + col);
            }

        }

    }

}
