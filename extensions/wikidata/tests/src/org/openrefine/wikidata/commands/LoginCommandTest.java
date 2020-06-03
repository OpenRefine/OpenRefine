package org.openrefine.wikidata.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.TestUtils;
import org.mockito.BDDMockito;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Constructor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@PrepareForTest({ConnectionManager.class, BasicApiConnection.class})
public class LoginCommandTest extends CommandTest {

    // used for mocking singleton
    Constructor<ConnectionManager> constructor;

    @BeforeClass
    public void initConstructor() throws NoSuchMethodException {
        constructor = ConnectionManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

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


    @Test
    public void testPasswordLogin() throws Exception {
        when(request.getParameter("wb-username")).thenReturn("username123");
        when(request.getParameter("wb-password")).thenReturn("password123");
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        PreferenceStore prefStore = new PreferenceStore();
        ProjectManager.singleton = mock(ProjectManager.class);
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);

        BasicApiConnection connection = mock(BasicApiConnection.class);
        when(connection.getCurrentUser()).thenReturn("username123");
        PowerMockito.whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);

        // mock the ConnectionManager singleton
        ConnectionManager manager = constructor.newInstance();
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(manager);

        command.doPost(request, response);

        ArrayNode array = (ArrayNode) prefStore.get(ConnectionManager.PREFERENCE_STORE_KEY);
        assertEquals(1, array.size());
        ObjectNode savedCredentials = (ObjectNode) array.get(0);
        assertEquals("username123", savedCredentials.get("username").asText());
        assertEquals("password123", savedCredentials.get("password").asText());
        TestUtils.assertEqualAsJson("{\"logged_in\":true,\"username\":\"username123\"}", writer.toString());
    }
}
