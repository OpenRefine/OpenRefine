package org.openrefine.wikidata.commands;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;
import org.mockito.ArgumentCaptor;
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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.refine.util.TestUtils.assertEqualAsJson;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.openrefine.wikidata.commands.LoginCommand.*;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.testng.Assert.*;

@PrepareForTest(ConnectionManager.class)
public class LoginCommandTest extends CommandTest {

    private static final String apiEndpoint = "https://www.wikidata.org/w/api.php";
    private static final String apiEndpointPrefix = apiEndpoint + "-";

    private static final String username = "my_username";
    private static final String password = "my_password";

    private static final String consumerToken = "my_consumer_token";
    private static final String consumerSecret = "my_consumer_secret";
    private static final String accessToken = "my_access_token";
    private static final String accessSecret = "my_access_secret";

    private static final Map<String, String> cookieMap = new HashMap<>();

    static {
        cookieMap.put("GeoIP", "TW:TXG:Taichung:24.15:120.68:v4");
        cookieMap.put("WMF-Last-Access", "15-Jun-2020");
        cookieMap.put("WMF-Last-Access-Global", "15-Jun-2020");
        cookieMap.put("centralauth_Session", "centralauth_Session123");
        cookieMap.put("centralauth_Token", "centralauth_Token123");
        cookieMap.put("centralauth_User", username);
        cookieMap.put("wikidatawikiSession", "wikidatawikiSession123");
        cookieMap.put("wikidatawikiUserID", "123456");
        cookieMap.put("wikidatawikiUserName", username);
    }

    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

    private ArgumentCaptor<Cookie> cookieCaptor;

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
        cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        doNothing().when(response).addCookie(cookieCaptor.capture());
    }

    @Test
    public void testClearCredentialsInPreferences() throws Exception {
        PreferenceStore prefStore = new PreferenceStore();
        ProjectManager.singleton = mock(ProjectManager.class);
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);
        prefStore.put(ConnectionManager.PREFERENCE_STORE_KEY, ParsingUtilities.mapper.createArrayNode());
        assertNotNull(prefStore.get(ConnectionManager.PREFERENCE_STORE_KEY));
        constructor.newInstance();
        assertNull(prefStore.get(ConnectionManager.PREFERENCE_STORE_KEY));
    }

    @Test
    public void testNoApiEndpointPost() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        command.doPost(request, response);
        assertEqualAsJson("{\"code\":\"error\",\"message\":\"missing parameter 'wb-api-endpoint'\"}", writer.toString());
    }

    @Test
    public void testNoApiEndpointGet() throws ServletException, IOException {
        command.doGet(request, response);
        assertEqualAsJson("{\"code\":\"error\",\"message\":\"missing parameter 'wb-api-endpoint'\"}", writer.toString());
    }

    @Test
    public void testNoCredentials() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        command.doPost(request, response);
        assertEqualAsJson("{\"logged_in\":false,\"username\":null,\"mediawiki_api_endpoint\":\"" + apiEndpoint + "\"}", writer.toString());
    }

    @Test
    public void testCsrfProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }

    @Test
    public void testGetNotCsrfProtected() throws ServletException, IOException {
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        command.doGet(request, response);
        assertEqualAsJson("{\"logged_in\":false,\"username\":null,\"mediawiki_api_endpoint\":\"" + apiEndpoint + "\"}", writer.toString());
    }

    private void assertLogin() {
        assertTrue(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\",\"mediawiki_api_endpoint\":\"" + apiEndpoint + "\"}",
                writer.toString());
    }

    @Test
    public void testUsernamePasswordLogin() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);
        when(connection.getCookies()).thenReturn(makeResponseCookies());

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        command.doPost(request, response);

        verify(connection).login(username, password);

        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        assertEquals(cookies.size(), 5);
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginRememberCredentials() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);
        when(connection.getCookies()).thenReturn(makeResponseCookies());

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        command.doPost(request, response);

        verify(connection).login(username, password);
        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        cookieMap.forEach((key, value) -> assertCookieEquals(cookies.get(apiEndpointPrefix + WIKIBASE_COOKIE_PREFIX + key), value, ONE_YEAR));
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), username, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginWithCookies() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        given(ConnectionManager.convertToBasicApiConnection(anyMap())).willReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);
        when(connection.getCookies()).thenReturn(makeResponseCookies());

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getCookies()).thenReturn(makeRequestCookies());

        command.doPost(request, response);

        verify(connection).checkCredentials();
        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        assertEquals(cookies.size(), 4);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testOwnerOnlyConsumerLogin() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);

        command.doPost(request, response);

        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        assertEquals(cookies.size(), 5);
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testOwnerOnlyConsumerLoginRememberCredentials() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);
        when(request.getCookies()).thenReturn(makeRequestCookies());

        command.doPost(request, response);

        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        // If logging in with owner-only consumer,
        // cookies for the username/password login should be cleared.
        cookieMap.forEach((key, value) -> assertCookieEquals(cookies.get(apiEndpointPrefix + WIKIBASE_COOKIE_PREFIX + key), "", 0));
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), consumerToken, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), consumerSecret, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), accessToken, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), accessSecret, ONE_YEAR);
    }

    @Test
    public void testOwnerOnlyConsumerLoginWithCookies() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        Cookie consumerTokenCookie = new Cookie(apiEndpointPrefix + CONSUMER_TOKEN, consumerToken);
        Cookie consumerSecretCookie = new Cookie(apiEndpointPrefix + CONSUMER_SECRET, consumerSecret);
        Cookie accessTokenCookie = new Cookie(apiEndpointPrefix + ACCESS_TOKEN, accessToken);
        Cookie accessSecretCookie = new Cookie(apiEndpointPrefix + ACCESS_SECRET, accessSecret);
        when(request.getCookies()).thenReturn(new Cookie[]{consumerTokenCookie, consumerSecretCookie, accessTokenCookie, accessSecretCookie});
        command.doPost(request, response);

        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        assertEquals(cookies.size(), 5);
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), consumerToken, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), consumerSecret, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), accessToken, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), accessSecret, ONE_YEAR);
    }

    @Test
    public void testCookieEncoding() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn("malformed consumer token \r\n %?");
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);
        when(request.getCookies()).thenReturn(makeRequestCookies());

        command.doPost(request, response);

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        assertNotEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN).getValue(), "malformed consumer token \r\n %?");
        assertEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN).getValue(), "malformed+consumer+token+%0D%0A+%25%3F");
    }

    @Test
    public void testCookieDecoding() throws Exception {
        ConnectionManager manager = mock(ConnectionManager.class);
        given(ConnectionManager.getInstance()).willReturn(manager);

        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        Cookie consumerTokenCookie = new Cookie(apiEndpointPrefix + CONSUMER_TOKEN, "malformed+consumer+token+%0D%0A+%25%3F");
        Cookie consumerSecretCookie = new Cookie(apiEndpointPrefix + CONSUMER_SECRET, consumerSecret);
        Cookie accessTokenCookie = new Cookie(apiEndpointPrefix + ACCESS_TOKEN, accessToken);
        Cookie accessSecretCookie = new Cookie(apiEndpointPrefix + ACCESS_SECRET, accessSecret);
        when(request.getCookies()).thenReturn(new Cookie[]{consumerTokenCookie, consumerSecretCookie, accessTokenCookie, accessSecretCookie});

        command.doPost(request, response);

        verify(manager).login(apiEndpoint, "malformed consumer token \r\n %?", consumerSecret, accessToken, accessSecret);
    }

    @Test
    public void testLogout() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);
        when(connection.getCookies()).thenReturn(makeResponseCookies());

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        int loginCookiesSize = cookieCaptor.getAllValues().size();

        assertLogin();

        // logout
        when(request.getParameter("logout")).thenReturn("true");
        when(request.getCookies()).thenReturn(makeRequestCookies()); // will be cleared
        StringWriter logoutWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(logoutWriter));

        command.doPost(request, response);

        assertFalse(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
        assertEqualAsJson("{\"logged_in\":false,\"username\":null, \"mediawiki_api_endpoint\":\"" + apiEndpoint + "\"}", logoutWriter.toString());

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues().subList(loginCookiesSize, cookieCaptor.getAllValues().size()));
        cookieMap.forEach((key, value) -> assertCookieEquals(cookies.get(apiEndpointPrefix + WIKIBASE_COOKIE_PREFIX + key), "", 0));
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginFailed() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        doThrow(new LoginFailedException("login failed")).when(connection).login(username, password);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        // we don't check the username/password here
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        verify(connection).login(username, password);
        assertFalse(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
    }

    @Test
    public void testUsernamePasswordWithCookiesLoginFailed() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        given(ConnectionManager.convertToBasicApiConnection(anyMap())).willReturn(connection);
        doThrow(new AssertUserFailedException("assert user login failed")).when(connection).checkCredentials();

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        // we don't check the username/password here
        when(request.getCookies()).thenReturn(makeRequestCookies());

        // login first
        command.doPost(request, response);

        verify(connection).checkCredentials();
        assertFalse(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
    }

    @Test
    public void testOwnerOnlyConsumerLoginFailed() throws Exception {
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        doThrow(new AssertUserFailedException("assert user login failed")).when(connection).checkCredentials();

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
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
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        assertTrue(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));

        // logout
        when(request.getParameter("logout")).thenReturn("true");
        doThrow(new MediaWikiApiErrorException("", "")).when(connection).logout();
        command.doPost(request, response);

        // still logged in
        assertTrue(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
    }

    @Test
    public void testLogoutFailedBecauseCredentialsExpired() throws Exception {
        // if our credentials expire and we try to log out,
        // we should consider that the logout succeeded.
        // Workaround for https://github.com/Wikidata/Wikidata-Toolkit/issues/511
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login first
        command.doPost(request, response);

        assertTrue(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));

        // logout
        when(request.getParameter("logout")).thenReturn("true");
        doThrow(new MediaWikiApiErrorException("assertuserfailed", "No longer logged in")).when(connection).logout();
        command.doPost(request, response);

        // not logged in anymore
        assertFalse(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
    }

    @Test
    public void testMultipleConnections() throws Exception {
        BasicApiConnection connection = mock(BasicApiConnection.class);
        whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        String wikibase1 = "https://www.wikibase1.org/w/api.php";
        String wikibase2 = "https://www.wikibase2.org/w/api.php";

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        // login to one endpoint first
        when(request.getParameter(API_ENDPOINT)).thenReturn(wikibase1);
        command.doPost(request, response);

        // not logged in to another endpoint
        assertFalse(ConnectionManager.getInstance().isLoggedIn(wikibase2));

        // login to another endpoint
        when(request.getParameter(API_ENDPOINT)).thenReturn(wikibase2);
        command.doPost(request, response);

        // logged in to both endpoints
        assertTrue(ConnectionManager.getInstance().isLoggedIn(wikibase1));
        assertTrue(ConnectionManager.getInstance().isLoggedIn(wikibase2));

        // logout from the first endpoint
        when(request.getParameter("logout")).thenReturn("true");
        when(request.getParameter(API_ENDPOINT)).thenReturn(wikibase1);
        command.doPost(request, response);

        // logged out from the first endpoint
        assertFalse(ConnectionManager.getInstance().isLoggedIn(wikibase1));

        // still logged in to another endpoint
        assertTrue(ConnectionManager.getInstance().isLoggedIn(wikibase2));
    }

    private static Cookie[] makeRequestCookies() {
        List<Cookie> cookies = new ArrayList<>();
        cookieMap.forEach((key, value) -> cookies.add(new Cookie(apiEndpointPrefix + WIKIBASE_COOKIE_PREFIX + key, value)));
        cookies.add(new Cookie(apiEndpointPrefix + USERNAME, username));
        return cookies.toArray(new Cookie[0]);
    }

    private static List<HttpCookie> makeResponseCookies() {
        List<HttpCookie> cookies = new ArrayList<>();
        cookieMap.forEach((key, value) -> cookies.add(new HttpCookie(key, value)));
        return cookies;
    }

    private static void assertCookieEquals(Cookie cookie, String expectedValue, int expectedMaxAge) {
        try {
            assertEquals(getCookieValue(cookie), expectedValue);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assertEquals(cookie.getMaxAge(), expectedMaxAge);
        assertEquals(cookie.getPath(), "/");
    }

    private static Map<String, Cookie> getCookieMap(List<Cookie> cookies) {
        Map<String, Cookie> map = new HashMap<>();
        cookies.forEach(cookie -> map.put(cookie.getName(), cookie));
        return map;
    }

    @Test
    public void testRemoveCRLF() {
        assertEquals(removeCRLF("a\rb\nc\r\n\r\nd"), "abcd");
        assertEquals(removeCRLF(null), "");
    }
}
