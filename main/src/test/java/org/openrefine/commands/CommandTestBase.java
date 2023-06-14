
package org.openrefine.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.RefineTest;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;

public class CommandTestBase extends RefineTest {

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected Command command = null;
    protected StringWriter writer = null;

    @BeforeMethod
    public void setUpRequestResponse() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to check that CSRF protection was triggered
     */
    protected void assertCSRFCheckFailed() {
        verify(response).setStatus(401);
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}");
    }
}
