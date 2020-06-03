package org.openrefine.wikidata.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.scribejava.apis.MediaWikiApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.TestUtils;
import org.mockito.BDDMockito;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.openrefine.wikidata.testing.WikidataRefineTest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test the OAuth workflow.
 */
@PrepareForTest(ConnectionManager.class)
public class OAuthWorkflowTest extends WikidataRefineTest {

    // used for mocking singleton
    Constructor<ConnectionManager> constructor;

    @BeforeClass
    public void initConstructor() throws NoSuchMethodException {
        constructor = ConnectionManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    @Test
    public void test() throws Exception {
        // env
        System.setProperty(ConnectionManager.WIKIDATA_CLIENT_ID_ENV_KEY, "client_id123");
        System.setProperty(ConnectionManager.WIKIDATA_CLIENT_SECRET_ENV_KEY, "client_secret123");

        // preference
        PreferenceStore prefStore = new PreferenceStore();
        ProjectManager.singleton = mock(ProjectManager.class);
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);

        // use this service in ConnectionManager
        OAuth10aService mediaWikiService = mock(OAuth10aService.class);
        ServiceBuilder serviceBuilder = mock(ServiceBuilder.class);
        when(serviceBuilder.apiSecret(anyString())).thenReturn(serviceBuilder);
        when(serviceBuilder.build(any(MediaWikiApi.class))).thenReturn(mediaWikiService);
        PowerMockito.whenNew(ServiceBuilder.class).withAnyArguments().thenReturn(serviceBuilder);

        // mock the ConnectionManager singleton
        ConnectionManager manager = constructor.newInstance();
        PowerMockito.mockStatic(ConnectionManager.class);
        BDDMockito.given(ConnectionManager.getInstance()).willReturn(manager);

        OAuth1RequestToken requestToken = new OAuth1RequestToken("request_token123", "request_secret123");
        when(mediaWikiService.getRequestToken()).thenReturn(requestToken);
        when(mediaWikiService.getAuthorizationUrl(requestToken)).thenReturn("http://bar.com/authorize");
        HttpServletRequest authorizeRequest = mock(HttpServletRequest.class);
        HttpServletResponse authorizeResponse = mock(HttpServletResponse.class);
        when(authorizeRequest.getParameter("remember-credentials")).thenReturn("on");

        HttpServletRequest authorizedRequest = mock(HttpServletRequest.class);
        HttpServletResponse authorizedResponse = mock(HttpServletResponse.class);
        when(authorizedRequest.getParameter("oauth_verifier")).thenReturn("oauth_verifier123");
        StringWriter authorizedWriter = new StringWriter();
        when(authorizedResponse.getWriter()).thenReturn(new PrintWriter(authorizedWriter));
        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connection.getCurrentUser()).thenReturn("foo");
        PowerMockito.whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);
        when(mediaWikiService.getAccessToken(requestToken, "oauth_verifier123"))
                .thenReturn(new OAuth1AccessToken("access_token123", "access_secret123"));

        HttpServletRequest loginInfoRequest = mock(HttpServletRequest.class);
        HttpServletResponse loginInfoResponse = mock(HttpServletResponse.class);
        when(loginInfoRequest.getParameter("wb-username")).thenReturn(null);
        when(loginInfoRequest.getParameter("wb-password")).thenReturn(null);
        when(loginInfoRequest.getParameter("remember-credentials")).thenReturn(null);
        when(loginInfoRequest.getParameter("logout")).thenReturn(null);
        StringWriter loginInfoWriter = new StringWriter();
        when(loginInfoResponse.getWriter()).thenReturn(new PrintWriter(loginInfoWriter));

        HttpServletRequest logoutRequest = mock(HttpServletRequest.class);
        HttpServletResponse logoutResponse = mock(HttpServletResponse.class);
        when(logoutRequest.getParameter("wb-username")).thenReturn(null);
        when(logoutRequest.getParameter("wb-password")).thenReturn(null);
        when(logoutRequest.getParameter("remember-credentials")).thenReturn(null);
        when(logoutRequest.getParameter("logout")).thenReturn("true");
        StringWriter logoutWriter = new StringWriter();
        when(logoutResponse.getWriter()).thenReturn(new PrintWriter(logoutWriter));

        // 1. The user sends a request to AuthorizeCommand.
        new AuthorizeCommand().doGet(authorizeRequest, authorizeResponse);

        // 2. AuthorizeCommand redirects the user to the authorization page.
        verify(authorizeResponse).sendRedirect("http://bar.com/authorize");

        // 3. The user is redirected back to AuthorizedCommand, with the oauth verifier.
        new AuthorizedCommand().doGet(authorizedRequest, authorizedResponse);

        // 4. The OAuth page is closed.
        assertEquals("<script>window.close()</script>", authorizedWriter.toString());

        // Done.
        assertTrue(manager.isLoggedIn());
        assertEquals("foo", manager.getUsername());
        ArrayNode array = (ArrayNode) prefStore.get(ConnectionManager.PREFERENCE_STORE_KEY);
        assertEquals(1, array.size());
        ObjectNode savedCredentials = (ObjectNode) array.get(0);
        assertEquals("access_token123", savedCredentials.get("access_token").asText());
        assertEquals("access_secret123", savedCredentials.get("access_secret").asText());

        // send "/command/wikidata/login" to get the login info
        new LoginCommand().doGet(loginInfoRequest, loginInfoResponse);
        TestUtils.assertEqualAsJson("{\"logged_in\":true,\"username\":\"foo\"}", loginInfoWriter.toString());

        // send "/command/wikidata/login?logout=true" to logout
        new LoginCommand().doGet(logoutRequest, logoutResponse);
        TestUtils.assertEqualAsJson("{\"logged_in\":false,\"username\":null}", logoutWriter.toString());

        // the credentials in the preference should be cleared
        array = (ArrayNode) prefStore.get(ConnectionManager.PREFERENCE_STORE_KEY);
        assertEquals(0, array.size());

    }
}
