package org.openrefine.wikidata.commands;

import org.mockito.BDDMockito;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;

@PrepareForTest(ConnectionManager.class)
public class AuthorizeCommandTest extends CommandTest {

    @BeforeMethod
    public void setup() {
        command = new AuthorizeCommand();
    }

    @Test
    public void testSupport() throws InterruptedException, ExecutionException, IOException, ServletException {
        ConnectionManager manager = mock(ConnectionManager.class);
        when(manager.supportOAuth()).thenReturn(true);
        when(manager.getAuthorizationUrl()).thenReturn("http://test.org");
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(manager);
        command.doGet(request, response);
        verify(response).sendRedirect("http://test.org");
    }

    @Test
    public void testNotSupport() throws ServletException, IOException {
        ConnectionManager manager = mock(ConnectionManager.class);
        when(manager.supportOAuth()).thenReturn(false);
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(manager);
        command.doGet(request, response);
        assertEquals("<h1>You must configure Wikidata OAuth client id/secret in refine.ini in order to use OAuth to login<h1>", writer.toString());
    }

}
