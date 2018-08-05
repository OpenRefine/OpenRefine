package org.openrefine.wikidata.commands;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoginCommandTest extends CommandTest {
    
    @BeforeMethod
    public void SetUp()
            throws JSONException {
        command = new LoginCommand();
    }
    
    @Test
    public void testNoCredentials() throws ServletException, IOException {
        command.doPost(request, response);

        assertEquals("{\"logged_in\":false,\"username\":null}", writer.toString());
    }
}
