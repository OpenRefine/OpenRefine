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

package com.google.refine.tests;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;

public class RefineTest {

    protected Logger logger;

    @BeforeSuite
    public void init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
    }
            
    /**
     * Check that a project was created with the appropriate number of columns and rows.
     * 
     * @param project project to check
     * @param numCols expected column count
     * @param numRows expected row count
     */
    public static void assertProjectCreated(Project project, int numCols, int numRows) {
        Assert.assertNotNull(project);
        Assert.assertNotNull(project.columnModel);
        Assert.assertNotNull(project.columnModel.columns);
        Assert.assertEquals(project.columnModel.columns.size(), numCols);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), numRows);
    }

    /**
     * Check that a project was created with the appropriate number of columns, rows, and records.
     * 
     * @param project project to check
     * @param numCols expected column count
     * @param numRows expected row count
     * @param numRows expected record count
     */
    public static void assertProjectCreated(Project project, int numCols, int numRows, int numRecords) {
        assertProjectCreated(project,numCols,numRows);
        Assert.assertNotNull(project.recordModel);
        Assert.assertEquals(project.recordModel.getRecordCount(),numRecords);
    }

    public void log(Project project) {
        // some quick and dirty debugging
        StringBuilder sb = new StringBuilder();
        for(Column c : project.columnModel.columns){
            sb.append(c.getName());
            sb.append("; ");
        }
        logger.info(sb.toString());
        for(Row r : project.rows){
            sb = new StringBuilder();
            for(int i = 0; i < r.cells.size(); i++){
                Cell c = r.getCell(i);
                if(c != null){
                   sb.append(c.value);
                   sb.append("; ");
                }else{
                    sb.append("null; ");
                }
            }
            logger.info(sb.toString());
        }
    }
    
    //----helpers----
    
    static public void whenGetBooleanOption(String name, JSONObject options, Boolean def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getBoolean(options, name, def)).thenReturn(def);
    }
    
    static public void whenGetIntegerOption(String name, JSONObject options, int def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getInt(options, name, def)).thenReturn(def);
    }
    
    static public void whenGetStringOption(String name, JSONObject options, String def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getString(options, name, def)).thenReturn(def);
    }
    
    static public void whenGetObjectOption(String name, JSONObject options, JSONObject def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getObject(options, name)).thenReturn(def);
    }
    
    static public void whenGetArrayOption(String name, JSONObject options, JSONArray def){
        when(options.has(name)).thenReturn(true);
        when(JSONUtilities.getArray(options, name)).thenReturn(def);
    }
    
    static public void verifyGetOption(String name, JSONObject options){
        verify(options, times(1)).has(name);
        try {
            verify(options, times(1)).get(name);
        } catch (JSONException e) {
            Assert.fail("JSONException",e);
        }
    }
    
    // Works for both int, String, and JSON arrays
    static public void verifyGetArrayOption(String name, JSONObject options){
        verify(options, times(1)).has(name);
        try {
            verify(options, times(1)).getJSONArray(name);
        } catch (JSONException e) {
            Assert.fail("JSONException",e);
        }
    }
}
