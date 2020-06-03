package org.openrefine.wikidata.commands;

import com.google.refine.util.TestUtils;
import org.mockito.BDDMockito;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@PrepareForTest(ConnectionManager.class)
public class SupportOAuthCommandTest extends CommandTest {

    // used for mocking singleton
    Constructor<ConnectionManager> constructor;

    @BeforeClass
    public void initConstructor() throws NoSuchMethodException {
        constructor = ConnectionManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    @BeforeMethod
    public void setup() {
        command = new SupportOAuthCommand();
    }

    @Test
    public void testSupport() throws ServletException, IOException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        System.setProperty(ConnectionManager.WIKIDATA_CLIENT_ID_ENV_KEY, "client_id");
        System.setProperty(ConnectionManager.WIKIDATA_CLIENT_SECRET_ENV_KEY, "client_secret");
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(constructor.newInstance());
        command.doGet(request, response);
        TestUtils.assertEqualAsJson("{\"support_oauth\":true}", writer.toString());
    }

    @Test
    public void testNotSupport1() throws ServletException, IOException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        System.clearProperty(ConnectionManager.WIKIDATA_CLIENT_ID_ENV_KEY);
        System.clearProperty(ConnectionManager.WIKIDATA_CLIENT_SECRET_ENV_KEY);
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(constructor.newInstance());
        command.doGet(request, response);
        TestUtils.assertEqualAsJson("{\"support_oauth\":false}", writer.toString());
    }

    @Test
    public void testNotSupport2() throws ServletException, IOException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        System.setProperty(ConnectionManager.WIKIDATA_CLIENT_ID_ENV_KEY, "client_id");
        System.clearProperty(ConnectionManager.WIKIDATA_CLIENT_SECRET_ENV_KEY);
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(constructor.newInstance());
        command.doGet(request, response);
        TestUtils.assertEqualAsJson("{\"support_oauth\":false}", writer.toString());
    }

    @Test
    public void testNotSupport3() throws ServletException, IOException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        System.clearProperty(ConnectionManager.WIKIDATA_CLIENT_ID_ENV_KEY);
        System.setProperty(ConnectionManager.WIKIDATA_CLIENT_SECRET_ENV_KEY, "client_secret");
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(constructor.newInstance());
        command.doGet(request, response);
        TestUtils.assertEqualAsJson("{\"support_oauth\":false}", writer.toString());
    }

}
