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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.snac.testing.TestingData2;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;

public class CommandTest extends RefineTest{

    protected Project project = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    protected Command command = null;


    @Test
    public void testRecentlyPublished() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\":\"recently_published\"}", "UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      //System.out.println(result);
      Assert.assertTrue(result.contains("success"));
    }

    @Test
    public void testTermSearch() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"vocabulary\",\"query_string\": \"person\",\"type\": \"entity_type\",\"entity_type\": null}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("700"));
    }

    @Test
    public void testBrowsing() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"browse\",\"term\": \"Washington\",\"position\": \"middle\"}", "UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("name_entry"));
    }

    /*
    * Test API calls for read_resource
    */

    @Test
    public void testReadConstellation() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"read_resource\",\"resourceid\": 7149468}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("resource"));
    }

    @Test
    public void TestNonexistantReadResource() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"read_resource\",\"resourceid\": \"100000000\"}", "UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(!result.contains("dataType\": \"Resource\""));
    }

    /*
    * Test API calls for resource_search
    */

    @Test
    public void TestResourceSearch() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"resource_search\",\"term\": \"Papers\"}", "UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("total\": ")); // we got a result
      Assert.assertTrue(!result.contains("total\": 0,")); // There were resources found
    }

    /*
    * Test API calls for read
    */

    @Test
    public void testSearchConcepts() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"read\",\"constellationid\": \"16715425\"}","UTF-8"));
      //post.setEntity(new StringEntity("{\"command\": \"browse\",\"term\": \"Washington\",\"position\": \"middle\"}", "UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("500"));
    }

    /*
    * Test API calls for constellation_history
    */

    @Test
    public void testConstellationHistory() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"constellation_history\",\"constellationid\": 76813079}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("Constellation"));
    }

    /*
    * Test API calls for shared_resources
    */

   @Test
    public void testSharedResources() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"shared_resources\",\"icid1\": 29260863 ,\"icid2\": 50307952}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("7960925"));
    }

    /*
    * Test API calls for read_vocabulary
    */

    @Test
    public void testReadVocabulary() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"read_vocabulary\",\"term_id\": 700}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("person"));
    }

    /*
    * Test API calls for get_holdings
    */

    @Test
    public void testGetHoldings() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"get_holdings\",\"constellationid\": 76778184}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("7677119"));
    }

    /*
    * Test API calls for elastic
    */

    @Test
    public void testElastic() throws Exception{
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://api.snaccooperative.org");
      post.setEntity(new StringEntity("{\"command\": \"elastic\",\"query\": { \"simple_query_string\": { \"query\": \"poets\", \"default_operator\": \"and\"}}, \"size\": 2, \"from\":0}","UTF-8"));
      HttpResponse response = client.execute(post);
      String result = EntityUtils.toString(response.getEntity());
      Assert.assertTrue(result.contains("\"result\": \"success\""));
      Assert.assertTrue(!result.contains("total\": 0,"));
    }

    @Test
    public void testRead1() throws Exception{
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://api.snaccooperative.org");
        post.setEntity(new StringEntity("{\"command\": \"search\",\"term\": \"Mozart\",\"count\": 10,\"start\": 0,\"entity_type\": \"person\"}","UTF-8"));
        HttpResponse response = client.execute(post);
        String result = EntityUtils.toString(response.getEntity());
        Assert.assertTrue(result.contains("23271282"));
    }

    @Test
    public void testRead2() throws Exception{
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://api.snaccooperative.org");
        post.setEntity(new StringEntity("{\"command\": \"search\",\"term\": \"Mendelssohn\",\"count\": 10,\"start\": 0,\"entity_type\": \"person\"}","UTF-8"));
        HttpResponse response = client.execute(post);
        String result = EntityUtils.toString(response.getEntity());
        Assert.assertTrue(result.contains("47916702"));
    }

    @Test
    public void testRead3() throws Exception{
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://api.snaccooperative.org");
        post.setEntity(new StringEntity("{\"command\": \"search\",\"term\": \"Dvorak\",\"count\": 10,\"start\": 0,\"entity_type\": \"person\"}","UTF-8"));
        HttpResponse response = client.execute(post);
        String result = EntityUtils.toString(response.getEntity());
        Assert.assertTrue(result.contains("40081682"));
    }
/*
    @BeforeMethod(alwaysRun = true)
    public void setUpProject() {
        project = createCSVProject(TestingData2.inceptionWithNewCsv);
        TestingData2.reconcileInceptionCells(project);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getParameter("project")).thenReturn(String.valueOf(project.id));

        try {
            when(response.getWriter()).thenReturn(printWriter);
        } catch (IOException e1) {
            Assert.fail();
        }
    }*/

    @Test
    public void testProjectColumns() throws Exception{
      project = createCSVProject(TestingData2.inceptionWithNewCsv);
      //project = createProjectWithColumns("test_columns", TestingData2.column_values);
      Assert.assertEquals(project.columnModel.getColumnNames().size(), 3);
    }

}
