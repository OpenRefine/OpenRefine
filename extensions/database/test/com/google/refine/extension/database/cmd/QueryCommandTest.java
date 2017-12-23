package com.google.refine.extension.database.cmd;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;


public class QueryCommandTest {
    
    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @BeforeTest
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDoPost() {

        when(request.getParameter("databaseType")).thenReturn(PgSQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn("127.0.0.1");
        when(request.getParameter("databasePort")).thenReturn("5432");
        when(request.getParameter("databaseUser")).thenReturn("postgres");
        when(request.getParameter("databasePassword")).thenReturn("");
        when(request.getParameter("initialDatabase")).thenReturn("openrefine");
        when(request.getParameter("queryString")).thenReturn("SELECT count(*) FROM call_data");
        

        StringWriter sw = new StringWriter();

        PrintWriter pw = new PrintWriter(sw);

        try {
            when(response.getWriter()).thenReturn(pw);
            ExecuteQueryCommand connectCommand = new ExecuteQueryCommand();
           
            connectCommand.doPost(request, response);
            
            String result = sw.getBuffer().toString().trim();
            JSONObject json = new JSONObject(result);
       
            String code = json.getString("code");
            Assert.assertEquals(code, "ok");

        
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
