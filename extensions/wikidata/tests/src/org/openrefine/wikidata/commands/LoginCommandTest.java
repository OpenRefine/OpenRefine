package org.openrefine.wikidata.commands;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.util.TestUtils;

public class LoginCommandTest extends CommandTest {
    
    @BeforeMethod
    public void SetUp() {
        command = new LoginCommand();
    }
    
    @Test
    public void testNoCredentials() throws ServletException, IOException {
    	when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
    	
        command.doPost(request, response);

        assertEquals("{\"logged_in\":false,\"username\":null}", writer.toString());
    }
    
    @Test
    public void testCsrfProtection() throws ServletException, IOException {
    	command.doPost(request, response);
    	TestUtils.assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }
    
    @Test
    public void testGetNotCsrfProtected() throws ServletException, IOException {
    	command.doGet(request, response);
    	TestUtils.assertEqualAsJson("{\"logged_in\":false,\"username\":null}", writer.toString());
    }
}
