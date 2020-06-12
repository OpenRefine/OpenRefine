package org.openrefine.wikidata.commands;

import com.google.refine.commands.Command;
import org.mockito.ArgumentCaptor;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.AssertUserFailedException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.refine.util.TestUtils.assertEqualAsJson;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.openrefine.wikidata.commands.LoginCommand.*;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.testng.Assert.*;

@PrepareForTest(ConnectionManager.class)
public class LoginCommandTest extends CommandTest {

    private static final String username = "my_username";
    private static final String password = "my_password";

    private static final String consumerToken = "my_consumer_token";
    private static final String consumerSecret = "my_consumer_secret";
    private static final String accessToken = "my_access_token";
    private static final String accessSecret = "my_access_secret";

    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

    private ArgumentCaptor<Cookie> captor;

    // used for mocking singleton
    Constructor<ConnectionManager> constructor;

    @BeforeClass
    public void initConstructor() throws NoSuchMethodException {
        constructor = ConnectionManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        command = new LoginCommand();

        // mock the ConnectionManager singleton
        ConnectionManager manager = constructor.newInstance();
        mockStatic(ConnectionManager.class);
        given(ConnectionManager.getInstance()).willReturn(manager);

        when(request.getCookies()).thenReturn(new Cookie[]{});
        captor = ArgumentCaptor.forClass(Cookie.class);
        doNothing().when(response).addCookie(captor.capture());
    }

    @Test
    public void testNoCredentials() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        command.doPost(request, response);
        // the first param is the actual one for testng.assertEquals
        assertEquals(writer.toString(), "{\"logged_in\":false,\"username\":null}");
    }

    @Test
    public void testCsrfProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }

    @Test
    public void testGetNotCsrfProtected() throws ServletException, IOException {
        command.doGet(request, response);
        assertEqualAsJson("{\"logged_in\":false,\"username\":null}", writer.toString());
    }

    @Test
    public void testUsernamePasswordLogin() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        command.doPost(request, response);

        verify(connection).login(username, password);
        assertTrue(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues());
        assertCookieEquals(cookies.get(USERNAME), "", 0);
        assertCookieEquals(cookies.get(PASSWORD), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginRememberCredentials() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        command.doPost(request, response);

        verify(connection).login(username, password);
        assertTrue(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues());
        assertCookieEquals(cookies.get(USERNAME), username, ONE_YEAR);
        assertCookieEquals(cookies.get(PASSWORD), password, ONE_YEAR);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginWithCookies() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        Cookie usernameCookie = new Cookie(USERNAME, username);
        Cookie passwordCookie = new Cookie(PASSWORD, password);
        when(request.getCookies()).thenReturn(new Cookie[]{usernameCookie, passwordCookie});

        command.doPost(request, response);

        verify(connection).login(username, password);
        assertTrue(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues());
        assertCookieEquals(cookies.get(USERNAME), username, ONE_YEAR);
        assertCookieEquals(cookies.get(PASSWORD), password, ONE_YEAR);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(ACCESS_SECRET), "", 0);
    }

    @Test
    public void testOwnerOnlyConsumerLogin() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);

        command.doPost(request, response);

        assertTrue(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues());
        assertCookieEquals(cookies.get(USERNAME), "", 0);
        assertCookieEquals(cookies.get(PASSWORD), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(ACCESS_SECRET), "", 0);
    }

    @Test
    public void testOwnerOnlyConsumerLoginRememberCredentials() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);

        command.doPost(request, response);

        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues());
        assertCookieEquals(cookies.get(USERNAME), "", 0);
        assertCookieEquals(cookies.get(PASSWORD), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), consumerToken, ONE_YEAR);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), consumerSecret, ONE_YEAR);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), accessToken, ONE_YEAR);
        assertCookieEquals(cookies.get(ACCESS_SECRET), accessSecret, ONE_YEAR);
    }

    @Test
    public void testOwnerOnlyConsumerLoginWithCookies() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        Cookie consumerTokenCookie = new Cookie(CONSUMER_TOKEN, consumerToken);
        Cookie consumerSecretCookie = new Cookie(CONSUMER_SECRET, consumerSecret);
        Cookie accessTokenCookie = new Cookie(ACCESS_TOKEN, accessToken);
        Cookie accessSecretCookie = new Cookie(ACCESS_SECRET, accessSecret);
        when(request.getCookies()).thenReturn(new Cookie[]{consumerTokenCookie, consumerSecretCookie, accessTokenCookie, accessSecretCookie});
        command.doPost(request, response);

        assertTrue(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues());
        assertCookieEquals(cookies.get(USERNAME), "", 0);
        assertCookieEquals(cookies.get(PASSWORD), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), consumerToken, ONE_YEAR);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), consumerSecret, ONE_YEAR);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), accessToken, ONE_YEAR);
        assertCookieEquals(cookies.get(ACCESS_SECRET), accessSecret, ONE_YEAR);
    }

    @Test
    public void testLogout() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        assertTrue(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\"}", writer.toString());

        // logout
        when(request.getParameter("logout")).thenReturn("true");
        StringWriter logoutWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(logoutWriter));

        command.doPost(request, response);

        assertFalse(ConnectionManager.getInstance().isLoggedIn());
        assertEqualAsJson("{\"logged_in\":false,\"username\":null}", logoutWriter.toString());

        // When logging in, 6 cookies are set.
        Map<String, Cookie> cookies = getCookieMap(captor.getAllValues().subList(6, captor.getAllValues().size()));
        assertCookieEquals(cookies.get(USERNAME), "", 0);
        assertCookieEquals(cookies.get(PASSWORD), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginFailed() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        doThrow(new LoginFailedException("login failed")).when(connection).login(username, password);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        // we don't check the username/password here
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        verify(connection).login(username, password);
        assertFalse(ConnectionManager.getInstance().isLoggedIn());
    }

    @Test
    public void testOwnerOnlyConsumerLoginFailed() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        doThrow(new AssertUserFailedException("assert user login failed")).when(connection).checkCredentials();

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);

        command.doPost(request, response);

        verify(connection).checkCredentials();
        assertFalse(connection.isLoggedIn());
    }

    @Test
    public void testLogoutFailed() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        assertTrue(ConnectionManager.getInstance().isLoggedIn());

        // logout
        when(request.getParameter("logout")).thenReturn("true");
        doThrow(new MediaWikiApiErrorException("", "")).when(connection).logout();
        command.doPost(request, response);

        // still logged in
        assertTrue(ConnectionManager.getInstance().isLoggedIn());
    }

    private static void assertCookieEquals(Cookie cookie, String expectedValue, int expectedMaxAge) {
        assertEquals(cookie.getValue(), expectedValue);
        assertEquals(cookie.getMaxAge(), expectedMaxAge);
        assertEquals(cookie.getPath(), "/");
    }

    private static Map<String, Cookie> getCookieMap(List<Cookie> cookies) {
        Map<String, Cookie> map = new HashMap<>();
        cookies.forEach(cookie -> map.put(cookie.getName(), cookie));
        return map;
    }
}
