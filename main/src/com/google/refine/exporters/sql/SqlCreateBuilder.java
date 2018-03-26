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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.util.JSONUtilities;

public class SqlCreateBuilder {

    private final static Logger logger = LoggerFactory.getLogger("SqlCreateBuilder");

    private String table;

    @SuppressWarnings("unused")
    private List<String> columns;

    private JSONObject options;

    public SqlCreateBuilder(String table, List<String> columns, JSONObject options) {
        this.table = table;
        this.columns = columns;
        this.options = options;
    }

    public String getCreateSQL() {
        StringBuffer createSB = new StringBuffer();

        JSONArray columnOptionArray = options == null ? null : JSONUtilities.getArray(options, "columns");
        
        final boolean trimColNames = options == null ? true
                : JSONUtilities.getBoolean(options, "trimColumnNames", false);

        int count = columnOptionArray.length();

        for (int i = 0; i < count; i++) {
            JSONObject columnOptions = JSONUtilities.getObjectElement(columnOptionArray, i);
            if (columnOptions != null) {
                String name = JSONUtilities.getString(columnOptions, "name", null);
                String type = JSONUtilities.getString(columnOptions, "type", "VARCHAR");
                String size = JSONUtilities.getString(columnOptions, "size", "");
               
                if (name != null) {
                    if(trimColNames) {
                        createSB.append(name.trim() + " ");
                    }else{
                        createSB.append(name + " ");
                    }
                   
                    if (type.equals("VARCHAR")) {
                        if (size.isEmpty()) {
                            size = "255";
                        }
                        createSB.append(type + "(" + size + ")");

                    } else if (type.equals("CHAR")) {
                        if (size.isEmpty()) {
                            size = "10";
                        }
                        createSB.append(type + "(" + size + ")");

                    } else if (type.equals("INT") || type.equals("INTEGER")) {
                        if (size.isEmpty()) {
                            createSB.append(type);
                        } else {
                            createSB.append(type + "(" + size + ")");
                        }

                    } else if (type.equals("NUMERIC")) {
                        if (size.isEmpty()) {
                            createSB.append(type);
                        } else {
                            createSB.append(type + "(" + size + ")");
                        }
                    } else {
                        createSB.append(type);
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
        if (includeDrop) {
            sql.append("DROP TABLE " + table + ";\n");
        }

        sql.append("CREATE TABLE ").append(table);
        sql.append(" (").append("\n");
        sql.append(createSB.toString());
        sql.append(")").append(";" + "\n");
        
        String createSQL = sql.toString();
        if(logger.isDebugEnabled()){
            logger.debug("Create SQL Generated Successfully...{}", createSQL);
        }
        return createSQL;
    }

}
