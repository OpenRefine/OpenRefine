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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.util.JSONUtilities;

public class SqlInsertBuilder {
    
    private  static final Logger logger = LoggerFactory.getLogger("SQLInsertBuilder");

    private String table;

    private List<String> columns;

    private List<ArrayList<SqlData>> sqlDataList;

    private JSONObject options;
    
    /**
     * 
     * @param table
     * @param columns
     * @param rows
     * @param options
     */
    public SqlInsertBuilder(String table, List<String> columns, List<ArrayList<SqlData>> rows, JSONObject options) {
        this.table = table;
        this.columns = columns;
        this.sqlDataList = rows;
        this.options = options;
    }

   /**
    * Get Insert Sql
    * @return
    */
    public String getInsertSQL(){
        if(logger.isDebugEnabled()) {
            logger.debug("Insert SQL with columns: {}", columns);
        }
        
        JSONArray colOptionArray = options == null ? null : JSONUtilities.getArray(options, "columns");
        Map<String, JSONObject> colOptionsMap = new HashMap<String, JSONObject>();
        if(colOptionArray != null) {
            colOptionArray.forEach(c -> {
                JSONObject json = (JSONObject)c;  
                colOptionsMap.put("" + json.get("name"), json);
            });
        }
      
        boolean nullValueToEmptyStr = options == null ? false : JSONUtilities.getBoolean(options, "convertNulltoEmptyString", true);
               
        StringBuffer values = new StringBuffer();
       
        int idx = 0;
        for(ArrayList<SqlData> sqlRow : sqlDataList) {
            StringBuilder rowValue = new StringBuilder();

            for(SqlData val : sqlRow) {
             
                JSONObject jsonOb = colOptionsMap.get(val.getColumnName());
                String type = (String)jsonOb.get("type");
                String defaultValue = (String)jsonOb.get("defaultValue");
                boolean allowNullChkBox = (boolean)jsonOb.get("allowNull");
                
                
                if(type == null) {
                    type = SqlData.SQL_TYPE_VARCHAR;
                }
                //Character Types
                if(type.equals(SqlData.SQL_TYPE_VARCHAR) || type.equals(SqlData.SQL_TYPE_CHAR) || type.equals(SqlData.SQL_TYPE_TEXT)) {
                    
                    if(!allowNullChkBox) {
                        throw new RuntimeException("bad input");
                    }
                    if((val.getText() == null || val.getText().isEmpty()) && nullValueToEmptyStr ) {
                       // logger.info("Appending empty String:::{}" , val.getText());
                        if(defaultValue != null && !defaultValue.isEmpty()) {
                            rowValue.append("'" + defaultValue + "'");
                        }else {
                            rowValue.append("null");
                        }
                        
                    }else {
                        rowValue.append("'" + val.getText() + "'"); 
                    }
                 
                }else {//Numeric Types
                    
                    if((val.getText() == null || val.getText().isEmpty()) && nullValueToEmptyStr ) {
                      //  logger.info("Appending empty String others:::{}" , val.getText());
                        if(defaultValue != null && !defaultValue.isEmpty()) {
                            rowValue.append("'" + defaultValue + "'");
                        }else {
                            rowValue.append("null");
                        }
                    }else {
                        rowValue.append(val.getText());
                    }
                    
                }
                
                rowValue.append(",");
               
            }
            
            
            idx++;
            String rowValString = rowValue.toString();
            rowValString = rowValString.substring(0, rowValString.length() - 1);
            
            values.append("( ");
            values.append(rowValString);
            values.append(" )");
            if(idx < sqlDataList.size()) {
                values.append(","); 
            }
            values.append("\n");
           
        }

        boolean trimColNames = options == null ? false : JSONUtilities.getBoolean(options, "trimColumnNames", false);
        String colNamesWithSep = columns.stream().map(col -> col.replaceAll("\\s", "")).collect(Collectors.joining(","));;
        if(!trimColNames) {
           colNamesWithSep = columns.stream().collect(Collectors.joining(","));  
        }
        
        String valuesString = values.toString();
        valuesString = valuesString.substring(0, valuesString.length() - 1);
        
        
        StringBuffer sql = new StringBuffer();

        sql.append("INSERT INTO ").append(table);
        sql.append(" (");
        sql.append(colNamesWithSep);
        sql.append(") VALUES ").append("\n");
        sql.append(valuesString);
        
        String sqlString = sql.toString();
        if(logger.isDebugEnabled()) {
            logger.debug("Insert Statement Generated Successfully...{}", sqlString);
        }
        return sqlString;
    }

}
