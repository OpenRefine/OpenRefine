
package org.openrefine.extension.gdata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.Command;
import org.openrefine.extension.gdata.UploadCommand;
import org.openrefine.util.ParsingUtilities;

public class UploadCommandTest {

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected Command command = null;
    protected StringWriter writer = null;

    @BeforeMethod
    public void setUpRequestResponse() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        command = new UploadCommand();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCsrfProtection() throws ServletException, IOException {
        command.doPost(request, response);
        Assert.assertEquals(
                ParsingUtilities.mapper.readValue("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}",
                        ObjectNode.class),
                ParsingUtilities.mapper.readValue(writer.toString(), ObjectNode.class));

    }
}
