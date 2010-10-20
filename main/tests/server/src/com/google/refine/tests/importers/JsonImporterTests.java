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

package com.google.refine.tests.importers;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.JsonImporter;
import com.google.refine.importers.parsers.JSONParser;
import com.google.refine.importers.parsers.TreeParserToken;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;

public class JsonImporterTests extends RefineTest {
	@BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }


    //dependencies
    Project project = null;
    Properties options = null;
    ByteArrayInputStream inputStream = null;

    //System Under Test
    JsonImporter SUT = null;


    @BeforeMethod
    public void SetUp(){
        SUT = new JsonImporter();
        project = new Project();
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() throws IOException{
        SUT = null;
        project = null;
        options = null;
        if (inputStream != null) inputStream.close();
        inputStream = null;
    }

    @Test
    public void canParseSample(){
        RunTest(getSample());

        log(project);
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "Author 1, The");
    }

    @Test
    public void canParseSampleWithDuplicateNestedElements(){
        RunTest(getSampleWithDuplicateNestedElements());

        log(project);
        assertProjectCreated(project, 4, 12);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 5);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "Author 1, The");
        Assert.assertEquals(project.rows.get(1).getCell(2).value, "Author 1, Another");
    }

    @Test
    public void testCanParseLineBreak(){

        RunTest(getSampleWithLineBreak());

        log(project);
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(3);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 5);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "With line\n break");
    }

    @Test
    public void testElementsWithVaryingStructure(){
        RunTest(getSampleWithVaryingStructure());

        log(project);
        assertProjectCreated(project, 5, 6);

        Assert.assertEquals( project.columnModel.getColumnByCellIndex(5).getName(), "__anonymous__ - genre");

        Row row0 = project.rows.get(0);
        Assert.assertNotNull(row0);
        Assert.assertEquals(row0.cells.size(),5);

        Row row5  = project.rows.get(5);
        Assert.assertNotNull(row5);
        Assert.assertEquals(row5.cells.size(),6);
    }

    @Test
    public void testElementWithNestedTree(){
        RunTest(getSampleWithTreeStructure());
        log(project);
        assertProjectCreated(project, 5, 6);

        Assert.assertEquals(project.columnModel.columnGroups.size(),1);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).keyColumnIndex, 3);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).startColumnIndex, 3);
        Assert.assertNull(project.columnModel.columnGroups.get(0).parentGroup);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).subgroups.size(),0);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).columnSpan,2);
    }
    
    @Test
    public void testElementWithMqlReadOutput(){
        String mqlOutput = "{\"code\":\"/api/status/ok\",\"result\":[{\"armed_force\":{\"id\":\"/en/wehrmacht\"},\"id\":\"/en/afrika_korps\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/en/sacred_band_of_thebes\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/british_16_air_assault_brigade\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/pathfinder_platoon\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0ch7qgz\"},\"id\":\"/en/sacred_band\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/polish_navy\"},\"id\":\"/en/3rd_ship_flotilla\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxn9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxq9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqh\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqw\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxl3\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxlp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0ck96kz\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cm3j23\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cw8hb4\",\"type\":\"/military/military_unit\"}],\"status\":\"200 OK\",\"transaction_id\":\"cache;cache01.p01.sjc1:8101;2010-10-04T15:04:33Z;0007\"}";
        RunTest(mqlOutput);
        log(project);
        assertProjectCreated(project,3,16);
    }
    
    
    /**
     * org.codehaus.Jackson.JsonParser has an inconsistency when returning getLocalName
     * of an Entity_Start token which occurs after a Field_Name token
     */
    @Test
    public void EnsureJSONParserHandlesgetLocalNameCorrectly() throws Exception{
        String sampleJson = "{\"field\":\"value\"}";
        String sampleJson2 = "{\"field\":{}}";
        String sampleJson3 = "{\"field\":[{},{}]}";
        
        JSONParser parser = new JSONParser(new ByteArrayInputStream( sampleJson.getBytes( "UTF-8" ) ));
        TreeParserToken token = TreeParserToken.Ignorable;
        int i = 0;
        try{
            while(token != null){
                token = parser.next();
                if(token == null)
                    break;
                i++;
                if(i == 3){
                    Assert.assertEquals(TreeParserToken.Value, token);
                    Assert.assertEquals("field", parser.getLocalName());
                }
            }
        }catch(Exception e){
            //silent
        }
        
        
        parser = new JSONParser(new ByteArrayInputStream( sampleJson2.getBytes( "UTF-8" ) ) );
        token = TreeParserToken.Ignorable;
        i = 0;
        try{
            while(token != null){
                token = parser.next();
                if(token == null)
                    break;
                i++;
                if(i == 3){
                    Assert.assertEquals(TreeParserToken.StartEntity, token);
                    Assert.assertEquals(parser.getLocalName(), "field");
                }
            }
        }catch(Exception e){
            //silent
        }
        
        parser = new JSONParser(new ByteArrayInputStream( sampleJson3.getBytes( "UTF-8" ) ) );
        token = TreeParserToken.Ignorable;
        i = 0;
        try{
            while(token != null){
                token = parser.next();
                if(token == null)
                    break;
                i++;
                if(i == 3){
                    Assert.assertEquals(token, TreeParserToken.StartEntity);
                    Assert.assertEquals(parser.getLocalName(), "field");
                }
                if(i == 4){
                    Assert.assertEquals(token, TreeParserToken.StartEntity);
                    Assert.assertEquals(parser.getLocalName(), "__anonymous__");
                }
                if(i == 6){
                    Assert.assertEquals(token, TreeParserToken.StartEntity);
                    Assert.assertEquals(parser.getLocalName(), "__anonymous__");
                }
            }
        }catch(Exception e){
            //silent
        }
    }

    //------------helper methods---------------

    public static String getTypicalElement(int id){
        return "{ \"id\" : " + id + "," +
        "\"author\" : \"Author " + id + ", The\"," +
        "\"title\" : \"Book title " + id + "\"," +
        "\"publish_date\" : \"2010-05-26\"" +
        "}";
    }

    public static String getElementWithDuplicateSubElement(int id){
        return "{ \"id\" : " + id + "," +
                 "\"authors\":[" +
                               "{\"name\" : \"Author " + id + ", The\"}," +
                               "{\"name\" : \"Author " + id + ", Another\"}" +
                             "]," +
                 "\"title\" : \"Book title " + id + "\"," +
                 "\"publish_date\" : \"2010-05-26\"" +
               "}";
    }

    public static String getSample(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 1; i < 7; i++){
            sb.append(getTypicalElement(i));
            if(i < 6)
            	sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getSampleWithDuplicateNestedElements(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 1; i < 7; i++){
            sb.append(getElementWithDuplicateSubElement(i));
            if(i < 6)
            	sb.append(",");
        }
        sb.append("]");
        return sb.toString();

    }

    public static String getSampleWithLineBreak(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 1; i < 4; i++){
            sb.append(getTypicalElement(i));
            sb.append(",");
        }
        sb.append("{\"id\" : 4," +
                "\"author\" : \"With line\\n break\"," + //FIXME this line break is doubled - is this correct??
                "\"title\" : \"Book title 4\"," +
                "\"publish_date\" : \"2010-05-26\"" +
                "},");
        sb.append(getTypicalElement(5));
        sb.append(",");
        sb.append(getTypicalElement(6));
        sb.append("]");
        return sb.toString();
    }

    public static String getSampleWithVaryingStructure(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 1; i < 6; i++){
            sb.append(getTypicalElement(i));
            sb.append(",");
        }
        sb.append("{\"id\" : 6," +
                "\"author\" : \"Author 6, The\"," +
                "\"title\" : \"Book title 6\"," +
                "\"genre\" : \"New element not seen in other records\"," +
                "\"publish_date\" : \"2010-05-26\"" +
                "}");
        sb.append("]");
        return sb.toString();
    }

    public static String getSampleWithTreeStructure(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 1; i < 7; i++){
            sb.append("{\"id\" : " + i + "," +
                    "\"author\" : {\"author-name\" : \"Author " + i + ", The\"," +
                    "\"author-dob\" : \"1950-0" + i + "-15\"}," +
                    "\"title\" : \"Book title " + i + "\"," +
                    "\"publish_date\" : \"2010-05-26\"" +
                    "}");
            if(i < 6)
            	sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private void RunTest(String testString){
        try {
            inputStream = new ByteArrayInputStream( testString.getBytes( "UTF-8" ) );
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }

        try {
            SUT.read(inputStream, project, new ProjectMetadata(), options);
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
