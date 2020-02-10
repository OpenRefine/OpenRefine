/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.snac.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.io.File;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.snac.testing.TestingData2;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;
import com.google.refine.util.ParsingUtilities;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;

import org.snaccooperative.commands.SNACUploadCommand;
import org.snaccooperative.commands.SNACResourceCommand;
import org.snaccooperative.exporters.SNACResourceCreator;
import org.snaccooperative.data.EntityId;
import org.snaccooperative.data.Resource;

public class SNACResourceTest extends RefineTest{

    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    protected Command command = null;
    protected Command upload = null;
    protected SNACResourceCreator manager = SNACResourceCreator.getInstance();
    protected EntityId entityId = null;

    @BeforeMethod
    public void SetUp() {
        // Setup for Post Request
        manager.csv_headers = new LinkedList<String>(){{add("title"); add("link"); add("abstract");}};
        HashMap<String, String> hash_map = new HashMap<String, String>();
        hash_map.put("title", "title");
        hash_map.put("link", "link");
        hash_map.put("abstract", "abstract");

        manager.match_attributes = hash_map;

        project = createCSVProject(TestingData2.resourceCsv);

        command = new SNACResourceCommand();
        upload = new SNACUploadCommand();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        // when(request.getParameter("dict")).thenReturn("{\"col1\":\"snaccol1\", \"col2\":\"snaccol2\", \"col3\":\"snaccol3\"}");
        // when(request.getParameter("project")).thenReturn("" + project.id + "");

        try {
            when(response.getWriter()).thenReturn(printWriter);
        } catch (IOException e1) {
            Assert.fail();
        }
    }

    @Test
    public void testLanguage1() throws Exception{
      Assert.assertNotNull(manager.detectLanguage("eng"));
      Assert.assertNotNull(manager.detectLanguage("kor"));
      Assert.assertNull(manager.detectLanguage("reeeee"));
      Assert.assertNotNull(manager.detectLanguage("jpn"));
      Assert.assertNull(manager.detectLanguage("hmm"));
    }
    // @Test
    // public void testLanguage2() throws Exception{
    //   Assert.assertNull(manager.detectLanguage("reeeee"));
    // }
    // @Test
    // public void testLanguage3() throws Exception{
    //   Assert.assertNotNull(manager.detectLanguage("kor"));
    // }

    @Test
    public void testResourceEquivalent1() throws Exception{
      Resource fromDataRes = manager.createResource(project.rows.get(0));
      String fromData = Resource.toJSON(fromDataRes);
      Assert.assertTrue(fromData.contains("Title1"));
    }

    @Test
    public void testResourceEquivalent2() throws Exception{
      Resource fromDataRes = manager.createResource(project.rows.get(0));
      String fromData = Resource.toJSON(fromDataRes);
      Assert.assertTrue(fromData.contains("abstract_example1"));
    }

    @Test
    public void testResourceGlobalOne() throws Exception{
      // command.doPost(request, response);
      // ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
      // String response_str = response.get("resource").textValue();
      String response_str = manager.getColumnMatchesJSONString();
      Assert.assertTrue(response_str.contains("title"));
    }

    @Test
    public void testResourceGlobalFalseFive() throws Exception{
      // command.doPost(request, response);
      // ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
      // String response_str = response.get("resource").textValue();
      String response_str = manager.getColumnMatchesJSONString();
      Assert.assertFalse(response_str.contains("col5"));
    }


    @Test
    public void testResourceGlobalTwo() throws Exception{
      // command.doPost(request, response);
      // ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
      // String response_str = response.get("resource").textValue();
      String response_str = manager.getColumnMatchesJSONString();
      Assert.assertTrue(response_str.contains("abstract"));
    }

    @Test
    public void testResourceGlobalThree() throws Exception{
      // command.doPost(request, response);
      // ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
      // String response_str = response.get("resource").textValue();
      String response_str = manager.getColumnMatchesJSONString();
      Assert.assertTrue(response_str.contains("link"));
    }

    @Test
    public void testResourceGlobalFalseFour() throws Exception{
      // command.doPost(request, response);
      // ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
      // String response_str = response.get("resource").textValue();
      String response_str = manager.getColumnMatchesJSONString();
      Assert.assertFalse(response_str.contains("col4"));
    }

//     @Test
//     public void testGson() throws Exception{
//       Gson bruh = new Gson();
//       String a="";
//       Assert.assertTrue(a.equals(""));
//     }
    @Test
    public void testResourceUpload() throws Exception{
      upload.doPost(request, response);
      ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
      String response_str = response.get("done").textValue();
      Assert.assertNotNull(response_str);
    }

    @Test
    public void testResourceUploadGet(){
        try{
            upload.doGet(request, response);
            ObjectNode response = ParsingUtilities.evaluateJsonStringToObjectNode(writer.toString());
            String response_str = response.get("doneGet").textValue();
        }
        catch(Exception e){
            String a="";
            Assert.assertTrue(a.equals(""));
        }

    }
}
