package org.openrefine.wikidata.commands;

import org.mockito.BDDMockito;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;

@PrepareForTest(ConnectionManager.class)
public class AuthorizedCommandTest extends CommandTest {

    @BeforeMethod
    public void setup() {
        command = new AuthorizedCommand();
    }

    @Test
    public void test() throws ServletException, IOException {
        ConnectionManager manager = mock(ConnectionManager.class);
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(manager);
        when(request.getParameter("oauth_verifier")).thenReturn("oauth_verifier");
        command.doGet(request, response);
        verify(manager).login("oauth_verifier");
        assertEquals("<script>window.close()</script>", writer.toString());
    }
}
