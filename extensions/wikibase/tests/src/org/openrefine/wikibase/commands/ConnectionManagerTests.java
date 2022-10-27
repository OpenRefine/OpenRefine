
package org.openrefine.wikibase.commands;

public class ConnectionManagerTests {
    /**
     * 
     * TODO the following tests were written for LoginCommand but actually test ConnectionManager. They should be
     * rewritten to directly test this class.
     * 
     * 
     * @Test public void testLogoutFailedBecauseCredentialsExpired() throws Exception { // if our credentials expire and
     *       we try to log out, // we should consider that the logout succeeded. // Workaround for
     *       https://github.com/Wikidata/Wikidata-Toolkit/issues/511 BasicApiConnection connection =
     *       mock(BasicApiConnection.class);
     *       whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
     *       when(connection.getCurrentUser()).thenReturn(username);
     * 
     *       when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
     *       when(request.getParameter(API_ENDPOINT)).thenReturn(apiEndpoint);
     *       when(request.getParameter(USERNAME)).thenReturn(username);
     *       when(request.getParameter(PASSWORD)).thenReturn(password);
     * 
     *       // login first command.doPost(request, response);
     * 
     *       assertTrue(ConnectionManager.getInstance().isLoggedIn(apiEndpoint));
     * 
     *       // logout when(request.getParameter("logout")).thenReturn("true"); doThrow(new
     *       MediaWikiApiErrorException("assertuserfailed", "No longer logged in")).when(connection).logout();
     *       command.doPost(request, response);
     * 
     *       // not logged in anymore assertFalse(ConnectionManager.getInstance().isLoggedIn(apiEndpoint)); }
     * 
     * @Test public void testMultipleConnections() throws Exception { BasicApiConnection connection =
     *       mock(BasicApiConnection.class);
     *       whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);
     *       when(connection.getCurrentUser()).thenReturn(username);
     * 
     *       String wikibase1 = "https://www.wikibase1.org/w/api.php"; String wikibase2 =
     *       "https://www.wikibase2.org/w/api.php";
     * 
     *       when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
     *       when(request.getParameter(USERNAME)).thenReturn(username);
     *       when(request.getParameter(PASSWORD)).thenReturn(password);
     * 
     *       // login to one endpoint first when(request.getParameter(API_ENDPOINT)).thenReturn(wikibase1);
     *       command.doPost(request, response);
     * 
     *       // not logged in to another endpoint assertFalse(ConnectionManager.getInstance().isLoggedIn(wikibase2));
     * 
     *       // login to another endpoint when(request.getParameter(API_ENDPOINT)).thenReturn(wikibase2);
     *       command.doPost(request, response);
     * 
     *       // logged in to both endpoints assertTrue(ConnectionManager.getInstance().isLoggedIn(wikibase1));
     *       assertTrue(ConnectionManager.getInstance().isLoggedIn(wikibase2));
     * 
     *       // logout from the first endpoint when(request.getParameter("logout")).thenReturn("true");
     *       when(request.getParameter(API_ENDPOINT)).thenReturn(wikibase1); command.doPost(request, response);
     * 
     *       // logged out from the first endpoint assertFalse(ConnectionManager.getInstance().isLoggedIn(wikibase1));
     * 
     *       // still logged in to another endpoint assertTrue(ConnectionManager.getInstance().isLoggedIn(wikibase2)); }
     */
}
