
package org.openrefine.wikibase.commands;

import static com.google.refine.util.TestUtils.assertEqualAsJson;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.openrefine.wikibase.commands.LoginCommand.ACCESS_SECRET;
import static org.openrefine.wikibase.commands.LoginCommand.ACCESS_TOKEN;
import static org.openrefine.wikibase.commands.LoginCommand.API_ENDPOINT;
import static org.openrefine.wikibase.commands.LoginCommand.CONSUMER_SECRET;
import static org.openrefine.wikibase.commands.LoginCommand.CONSUMER_TOKEN;
import static org.openrefine.wikibase.commands.LoginCommand.PASSWORD;
import static org.openrefine.wikibase.commands.LoginCommand.USERNAME;
import static org.openrefine.wikibase.commands.LoginCommand.WIKIBASE_COOKIE_PREFIX;
import static org.openrefine.wikibase.commands.LoginCommand.getCookieValue;
import static org.openrefine.wikibase.commands.LoginCommand.removeCRLF;
import static org.openrefine.wikibase.commands.LoginCommand.sanitizeCookieKey;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;

import com.google.refine.commands.Command;

public class LoginCommandTest extends CommandTest {

    private static final String apiEndpoint = "https://www.wikidata.org/w/api.php";
    private static final String apiEndpointPrefix = sanitizeCookieKey(apiEndpoint) + "-";

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

    ConnectionManager connectionManager;

    @BeforeMethod
    public void setUp() throws Exception {
        command = new LoginCommand();
        connectionManager = mock(ConnectionManager.class);
        ((LoginCommand) command).setConnectionManager(connectionManager);

        when(request.getCookies()).thenReturn(new Cookie[] {});
        cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        doNothing().when(response).addCookie(cookieCaptor.capture());
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
        assertEqualAsJson("{\"logged_in\":true,\"username\":\"" + username + "\",\"mediawiki_api_endpoint\":\"" + apiEndpoint + "\"}",
                writer.toString());
    }

    @Test
    public void testUsernamePasswordLogin() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        when(connectionManager.login(apiEndpoint, username, password)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager, times(1)).login(apiEndpoint, username, password);

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
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        when(connectionManager.login(apiEndpoint, username, password)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        BasicApiConnection connection = mock(BasicApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCookies()).thenReturn(makeResponseCookies());
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager, times(1)).login(apiEndpoint, username, password);

        assertLogin();

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        cookieMap.forEach(
                (key, value) -> assertCookieEquals(cookies.get(apiEndpointPrefix + WIKIBASE_COOKIE_PREFIX + key), value, ONE_YEAR));
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), username, ONE_YEAR);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginWithCookies() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getCookies()).thenReturn(makeRequestCookies());

        when(connectionManager.login(eq(apiEndpoint), eq(username), Mockito.<List<Cookie>> any())).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        BasicApiConnection connection = mock(BasicApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCookies()).thenReturn(makeResponseCookies());
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager, times(1)).login(eq(apiEndpoint), eq(username), Mockito.<List<Cookie>> any());

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
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);

        when(connectionManager.login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager, times(1)).login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret);

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
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);
        when(request.getCookies()).thenReturn(makeRequestCookies());

        when(connectionManager.login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager, times(1)).login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret);
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
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        Cookie consumerTokenCookie = new Cookie(apiEndpointPrefix + CONSUMER_TOKEN, consumerToken);
        Cookie consumerSecretCookie = new Cookie(apiEndpointPrefix + CONSUMER_SECRET, consumerSecret);
        Cookie accessTokenCookie = new Cookie(apiEndpointPrefix + ACCESS_TOKEN, accessToken);
        Cookie accessSecretCookie = new Cookie(apiEndpointPrefix + ACCESS_SECRET, accessSecret);
        when(request.getCookies())
                .thenReturn(new Cookie[] { consumerTokenCookie, consumerSecretCookie, accessTokenCookie, accessSecretCookie });

        when(connectionManager.login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager, times(1)).login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret);

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
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("remember-credentials")).thenReturn("on");
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn("malformed consumer token \r\n %?");
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);
        when(request.getCookies()).thenReturn(makeRequestCookies());

        when(connectionManager.login(apiEndpoint, "malformed consumer token \r\n %?", consumerSecret, accessToken, accessSecret))
                .thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        Map<String, Cookie> cookies = getCookieMap(cookieCaptor.getAllValues());
        assertNotEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN).getValue(), "malformed consumer token \r\n %?");
        assertEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN).getValue(), "malformed+consumer+token+%0D%0A+%25%3F");
    }

    @Test
    public void testCookieDecoding() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        Cookie consumerTokenCookie = new Cookie(apiEndpointPrefix + CONSUMER_TOKEN, "malformed+consumer+token+%0D%0A+%25%3F");
        Cookie consumerSecretCookie = new Cookie(apiEndpointPrefix + CONSUMER_SECRET, consumerSecret);
        Cookie accessTokenCookie = new Cookie(apiEndpointPrefix + ACCESS_TOKEN, accessToken);
        Cookie accessSecretCookie = new Cookie(apiEndpointPrefix + ACCESS_SECRET, accessSecret);
        when(request.getCookies())
                .thenReturn(new Cookie[] { consumerTokenCookie, consumerSecretCookie, accessTokenCookie, accessSecretCookie });

        when(connectionManager.login(apiEndpoint, "malformed consumer token \r\n %?", consumerSecret, accessToken, accessSecret))
                .thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(connection);
        when(connection.getCurrentUser()).thenReturn(username);

        command.doPost(request, response);

        verify(connectionManager).login(apiEndpoint, "malformed consumer token \r\n %?", consumerSecret, accessToken, accessSecret);
    }

    @Test
    public void testLogout() throws Exception {

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        when(connectionManager.login(apiEndpoint, username, password)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);

        // login first
        command.doPost(request, response);

        int loginCookiesSize = cookieCaptor.getAllValues().size();

        verify(connectionManager, times(1)).login(apiEndpoint, username, password);

        assertLogin();

        // logout
        when(request.getParameter("logout")).thenReturn("true");
        when(request.getCookies()).thenReturn(makeRequestCookies()); // will be cleared
        StringWriter logoutWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(logoutWriter));

        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(false);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(null);

        command.doPost(request, response);

        verify(connectionManager).logout(apiEndpoint);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(false);
        assertEqualAsJson("{\"logged_in\":false,\"username\":null, \"mediawiki_api_endpoint\":\"" + apiEndpoint + "\"}",
                logoutWriter.toString());

        Map<String, Cookie> cookies = getCookieMap(
                cookieCaptor.getAllValues().subList(loginCookiesSize, cookieCaptor.getAllValues().size()));
        cookieMap.forEach((key, value) -> assertCookieEquals(cookies.get(apiEndpointPrefix + WIKIBASE_COOKIE_PREFIX + key), "", 0));
        assertCookieEquals(cookies.get(apiEndpointPrefix + USERNAME), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + CONSUMER_SECRET), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_TOKEN), "", 0);
        assertCookieEquals(cookies.get(apiEndpointPrefix + ACCESS_SECRET), "", 0);
    }

    @Test
    public void testUsernamePasswordLoginFailed() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        // we don't check the username/password here
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        when(connectionManager.login(apiEndpoint, username, password)).thenReturn(false);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(false);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(null);

        // login first
        command.doPost(request, response);

        verify(connectionManager).login(apiEndpoint, username, password);
    }

    @Test
    public void testUsernamePasswordWithCookiesLoginFailed() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        // we don't check the username/password here
        when(request.getCookies()).thenReturn(makeRequestCookies());

        when(connectionManager.login(eq(apiEndpoint), eq(username), Mockito.<List<Cookie>> any())).thenReturn(false);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(false);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(null);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(null);

        // login first
        command.doPost(request, response);

        verify(connectionManager).login(eq(apiEndpoint), eq(username), Mockito.<List<Cookie>> any());
        assertFalse(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
    }

    @Test
    public void testOwnerOnlyConsumerLoginFailed() throws Exception {

        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(CONSUMER_TOKEN)).thenReturn(consumerToken);
        when(request.getParameter(CONSUMER_SECRET)).thenReturn(consumerSecret);
        when(request.getParameter(ACCESS_TOKEN)).thenReturn(accessToken);
        when(request.getParameter(ACCESS_SECRET)).thenReturn(accessSecret);

        when(connectionManager.login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret)).thenReturn(false);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(false);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(null);
        when(connectionManager.getConnection(apiEndpoint)).thenReturn(null);

        command.doPost(request, response);

        verify(connectionManager).login(apiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret);
    }

    @Test
    public void testLogoutFailed() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
        when(request.getParameter(USERNAME)).thenReturn(username);
        when(request.getParameter(PASSWORD)).thenReturn(password);

        when(connectionManager.login(apiEndpoint, username, password)).thenReturn(true);
        when(connectionManager.isLoggedIn(apiEndpoint)).thenReturn(true);
        when(connectionManager.getUsername(apiEndpoint)).thenReturn(username);

        // login first
        command.doPost(request, response);

        verify(connectionManager).login(apiEndpoint, username, password);

        // logout
        when(request.getParameter("logout")).thenReturn("true");

        command.doPost(request, response);

        // still logged in
        verify(connectionManager).logout(apiEndpoint);
        assertLogin();
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

    @Test
    public void testSanitizeCookieKey() {
        assertEquals(sanitizeCookieKey("https://www.wikidata.org/"), "https---www.wikidata.org-");
    }
}
