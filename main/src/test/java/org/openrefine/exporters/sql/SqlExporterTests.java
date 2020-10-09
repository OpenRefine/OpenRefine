/*

Copyright 2018, Tony Opara.
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

package org.openrefine.exporters.sql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotEquals;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class SqlExporterTests extends RefineTest {
    
    private static final String TEST_PROJECT_NAME = "SQL_EXPORTER_TEST_PROJECT";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    StringWriter writer;
    ProjectMetadata projectMetadata;
    GridState grid;
    Engine engine;
    Properties options;
    SqlCreateBuilder sqlCreateBuilder;
    SqlInsertBuilder sqlInsertBuilder;

    //System Under Test
    SqlExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new SqlExporter();
        writer = new StringWriter();
        projectMetadata = new ProjectMetadata();
        projectMetadata.setName(TEST_PROJECT_NAME);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        writer = null;
        grid = null;
        projectMetadata = null;
        engine = null;
        options = null;
        sqlCreateBuilder = null;
        sqlInsertBuilder = null;
    }
    
    @Test
    public void testExportSqlWithNonZeroScaleNumericValue() {
    	grid = createGrid(new String[] {"columnO", "column1"},
    			new Serializable[][] {
    		{generateRandomNumericValues(), generateRandomNumericValues()},
    		{generateRandomNumericValues(), generateRandomNumericValues()}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
    	
        String tableName = "sql_table_test";
        String optionsString = createOptionsFromProject(tableName, SqlData.SQL_TYPE_NUMERIC,null).toString();
        when(options.getProperty("options")).thenReturn(optionsString);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
        logger.info("result = \n" + result);
        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        //logger.info("checkResult1 =" + checkResult);
        checkResult = result.contains("INSERT INTO " + tableName);
       // logger.info("checkResult2 =" + checkResult);
        Assert.assertEquals(checkResult,  true);
   
    }

    @Test
    public void testExportSimpleSql() {
    	grid = createGrid(new String[] {"columnO", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        String tableName = "sql_table_test";
        String optionsString = createOptionsFromProject(tableName, null,null).toString();
        when(options.getProperty("options")).thenReturn(optionsString);

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();

        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        Assert.assertTrue(result.contains("INSERT INTO " + tableName));
   
    }
    
    @Test
    public void testExportSqlNoSchema() {
    	grid = createGrid(new String[] {"columnO", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null,null);
        optionsJson.put("includeStructure", false);
        when(options.getProperty("options")).thenReturn(optionsJson.toString());
       // logger.info("Options = " + optionsJson.toString());

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
        //logger.info("result = \n" + result);
        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        Assert.assertEquals(checkResult,  false);
       
        checkResult = result.contains("INSERT INTO " + tableName);
        Assert.assertEquals(checkResult,  true);
   
    }
    
    @Test
    public void testExportSqlNoContent(){
    	grid = createGrid(new String[] {"columnO", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
    	
        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeContent", false);
        when(options.getProperty("options")).thenReturn(optionsJson.toString());
        //logger.info("Options = " + optionsJson.toString());

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
       // logger.info("result = \n" + result);
        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        Assert.assertEquals(checkResult,  true);
       
        checkResult = result.contains("INSERT INTO " + tableName);
        Assert.assertEquals(checkResult,  false);
   
    }
    
    @Test
    public void testExportSqlIncludeSchemaWithDropStmt(){
    	grid = createGrid(new String[] {"columnO", "column1"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1"},
    		{"row1cell0", "row1cell1"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        
        when(options.getProperty("options")).thenReturn(optionsJson.toString());
        //logger.info("Options = " + optionsJson.toString());

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
        //logger.info("result = " + result);
        
        Assert.assertNotNull(result);
//        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
//        assertNotEquals(writer.toString(), SqlExporter.NO_COL_SELECTED_ERROR);
        
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        Assert.assertEquals(checkResult,  true);
        //logger.info("checkResult1 = " + checkResult);
       
        checkResult = result.contains("INSERT INTO " + tableName );
        Assert.assertEquals(checkResult,  true);
        //logger.info("checkResult2 = " + checkResult);
        
        checkResult = result.contains("DROP TABLE IF EXISTS " + tableName + ";");
        Assert.assertEquals(checkResult,  true);
        //logger.info("checkResult3 = " + checkResult);
   
    }

    @Test
    public void testGetCreateSql() {
    	grid = createGrid(new String[] {"columnO", "column1", "column2"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1", "row0cell2"},
    		{"row1cell0", "row1cell1", "row1cell2"},
    		{"row2cell0", "row2cell1", "row2cell2"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
    	
        String tableName = "sql_table_test";
        String type = "CHAR";
        String size = "2";
        JsonNode optionsJson = createOptionsFromProject(tableName, type, size);
       // logger.info("Options:: = " + optionsJson.toString());
        List<String> columns = grid.getColumnModel().getColumns().stream().map(col -> col.getName()).collect(Collectors.toList());
       
        sqlCreateBuilder  = new SqlCreateBuilder(tableName, columns, optionsJson);
        String createSql = sqlCreateBuilder.getCreateSQL();
        //logger.info("createSql = \n" + createSql);
        Assert.assertNotNull(createSql);
        Assert.assertTrue(createSql.contains(type + "(" + size + ")"));
    }
    
    @Test
    public void testExportSqlWithNullFields(){
    	grid = createGrid(new String[] {"columnO", "column1", "column2"},
    			new Serializable[][] {
    		{"", "", ""},
    		{"", "", ""},
    		{"", "", "row2cell2"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
    	
        int inNull = 8;

        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);
        
        
        when(options.getProperty("options")).thenReturn(optionsJson.toString());
        //logger.info("Options = " + optionsJson.toString());

        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
        Assert.assertNotNull(result);
        //logger.info("\nresult = " + result);
       // logger.info("\nNull Count:" + countWordInString(result, "null"));
        
        int countNull = countWordInString(result, "null");
        Assert.assertEquals(countNull, inNull);

    }
    
    @Test
    public void testExportSqlWithNotNullColumns(){
    	grid = createGrid(new String[] {"columnO", "column1", "column2", "column3"},
    			new Serializable[][] {
    		{"row0cell0", "row0cell1", "row0cell2", "row0cell3"},
    		{"row1cell0", "row1cell1", "row1cell2", "row1cell3"},
    		{"row2cell0", "row2cell1", "row2cell2", "row2cell3"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);
        
        String tableName = "sql_table_test";
        ObjectNode optionsJson = createOptionsFromProject(tableName, null, null, null, false);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);
        
       when(options.getProperty("options")).thenReturn(optionsJson.toString());
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
        logger.info("\nresult:={} ", result);
        Assert.assertNotNull(result);
     
        int countNull = countWordInString(result, "NOT NULL");
        logger.info("\nNot Null Count: {}" , countNull);
        Assert.assertEquals(countNull, 4);

    }
    
    @Test
    public void testExportSqlWithSingleQuote() {
    	grid = createGrid(new String[] {"column0", "column1", "column2", "column3"},
    			new Serializable[][] {
    		{"It's row0cell0", "It's row0cell1", "It's row0cell2", "It's row0cell3"}
    	});
    	engine = new Engine(grid, EngineConfig.ALL_ROWS);

        String tableName = "sql_table_test";
        ObjectNode optionsJson = createOptionsFromProject(tableName, null, null, null, false);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);
        
       when(options.getProperty("options")).thenReturn(optionsJson.toString());
        try {
            SUT.export(grid, projectMetadata, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }
        
        String result = writer.toString();
        logger.info("\nresult:={} ", result);

        Assert.assertTrue(result.contains("INSERT INTO sql_table_test (column0,column1,column2,column3) VALUES \n" + 
        		"( 'It''s row0cell0','It''s row0cell1','It''s row0cell2','It''s row0cell3' )"));

    }
  
    //helper methods
    
   public int countWordInString(String input, String word){
       if(input == null || input.isEmpty()) {
           return 0;
       }
       int i = 0;
       Pattern p = Pattern.compile(word);
       Matcher m = p.matcher( input );
       while (m.find()) {
           i++;
       }
       
       return i;
       
   }
    
    protected ObjectNode createNumericColOptionsFromProject(String tableName, String type, String size) {
        
        ObjectNode json = ParsingUtilities.mapper.createObjectNode();
        ArrayNode columns = json.putArray("columns");
        json.put("tableName", tableName);
        
        List<ColumnMetadata> cols = grid.getColumnModel().getColumns();
      
        cols.forEach(c -> {
            //logger.info("Column Name = " + c.getName());
            ObjectNode columnModel = ParsingUtilities.mapper.createObjectNode();
            columnModel.put("name", c.getName());
            if(type != null) {
                columnModel.put("type", type);
            } else {
                columnModel.put("type", "VARCHAR");
            }
            if(size != null) {
                columnModel.put("size", size); 
            } else {
                columnModel.put("size", "100");
            }
            
            if(type != null) {
                columnModel.put("type", type);
            }
            if(size != null) {
               // logger.info(" Size = " + size);
                columnModel.put("size", size);
            }
            
            columns.add(columnModel);
       
        });
        
       return json;
    }
  
   protected JsonNode createOptionsFromProject(String tableName, String type, String size) {
       ObjectNode json = ParsingUtilities.mapper.createObjectNode();
       json.put("tableName", tableName);
       ArrayNode columns = json.putArray("columns");
       
       List<ColumnMetadata> cols = grid.getColumnModel().getColumns();
     
       cols.forEach(c -> {
           //logger.info("Column Name = " + c.getName());
           ObjectNode columnModel = ParsingUtilities.mapper.createObjectNode();
           columnModel.put("name", c.getName());
           if(type != null) {
               columnModel.put("type", type);
           }else {
               columnModel.put("type", "VARCHAR");
           }
           if(size != null) {
               columnModel.put("size", size); 
           }else {
               columnModel.put("size", "100");
           }
           
           if(type != null) {
               columnModel.put("type", type);
           }
           if(size != null) {
              // logger.info(" Size = " + size);
               columnModel.put("size", size);
           }
           
           columns.add(columnModel);
      
       });
       
       return json;
   }
   
    protected ObjectNode createOptionsFromProject(String tableName, String type, String size, String defaultValue,
            boolean allowNull) {
       
       ObjectNode json = ParsingUtilities.mapper.createObjectNode();
       ArrayNode columns = json.putArray("columns");
       json.put("tableName", tableName);
       
       List<ColumnMetadata> cols = grid.getColumnModel().getColumns();
     
       cols.forEach(c -> {
           //logger.info("Column Name = " + c.getName());
           ObjectNode columnModel = ParsingUtilities.mapper.createObjectNode();
           columnModel.put("name", c.getName());
           if(type != null) {
               columnModel.put("type", type);
           }else {
               columnModel.put("type", "VARCHAR");
           }
           if(size != null) {
               columnModel.put("size", size); 
           }else {
               columnModel.put("size", "100");
           }
           
           if(type != null) {
               columnModel.put("type", type);
           }
           if(size != null) {
              // logger.info(" Size = " + size);
               columnModel.put("size", size);
           }
          
           columnModel.put("defaultValue", defaultValue);
           columnModel.put("allowNull", allowNull);
           
           columns.add(columnModel);
      
       });
       
      return json;
   }
    
    double generateRandomNumericValues(){
        int precision = 100; //scale = 2
        double randomnum = Math.floor(Math.random() * (10 * precision - 1 * precision) + 1 * precision) / (1*precision);
        return randomnum;
    }
    
   
  
}
