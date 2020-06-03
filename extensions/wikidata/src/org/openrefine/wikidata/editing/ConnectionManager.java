/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.editing;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.apis.MediaWikiApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

/**
 * Manages a connection to Wikidata, with login credentials stored in the
 * preferences.
 *
 * The user can choose to use OAuth or password to login.
 *
 * Ideally, we should store only the cookies and not the password. But
 * Wikidata-Toolkit does not allow for that as cookies are kept private.
 *
 * This class is also hard-coded for Wikidata: generalization to other Wikibase
 * instances should be feasible though.
 *
 * @author Antonin Delpeuch
 * @author Lu Liu
 */

public class ConnectionManager {

    final static Logger logger = LoggerFactory.getLogger("connection_manager");

    public static final String PREFERENCE_STORE_KEY = "wikidata_credentials";
    public static final String WIKIDATA_CLIENT_ID_ENV_KEY = "ext.wikidata.clientid";
    public static final String WIKIDATA_CLIENT_SECRET_ENV_KEY = "ext.wikidata.clientsecret";
    public static final int CONNECT_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;

    private PreferenceStore prefStore;
    private ApiConnection connection;

    private String CLIENT_ID = System.getProperty(WIKIDATA_CLIENT_ID_ENV_KEY, "");
    private String CLIENT_SECRET = System.getProperty(WIKIDATA_CLIENT_SECRET_ENV_KEY, "");
    private OAuth10aService mediaWikiService;
    private OAuth1RequestToken requestToken;

    private boolean rememberCredentials = false;

    private static final ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    /**
     * Creates a connection manager, which attempts to restore any
     * previous connection (from the preferences).
     */
    private ConnectionManager() {
        if (CLIENT_ID.equals("") || CLIENT_SECRET.equals("")) {
            mediaWikiService = null;
        } else {
            ServiceBuilder serviceBuilder = new ServiceBuilder(CLIENT_ID);
            mediaWikiService = serviceBuilder
                    .apiSecret(CLIENT_SECRET)
                    .build(MediaWikiApi.instance());
        }

        prefStore = ProjectManager.singleton.getPreferenceStore();
        connection = null;
        restoreSavedConnection();
    }

    /**
     * Logs in to the Wikibase instance, using login/password
     *
     * @param username
     *      the username to log in with
     * @param password
     *      the password to log in with
     */
    public void login(String username, String password) {
        connection = BasicApiConnection.getWikidataApiConnection();
        setupConnection(connection);
        try {
            ((BasicApiConnection) connection).login(username, password);
            if (rememberCredentials) {
                ArrayNode array = ParsingUtilities.mapper.createArrayNode();
                ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
                obj.put("username", username);
                obj.put("password", password);
                array.add(obj);
                prefStore.put(PREFERENCE_STORE_KEY, array);
            }
        } catch (LoginFailedException e) {
            connection = null;
        }
    }

    /**
     * Login with the OAuth verifier.
     *
     * The verifier is used to trade OAuth access token/secret.
     */
    public void login(String verifier) {
        try {
            OAuth1AccessToken accessToken = mediaWikiService.getAccessToken(requestToken, verifier);
            connection = new OAuthApiConnection(ApiConnection.URL_WIKIDATA_API,
                    mediaWikiService.getApiKey(), mediaWikiService.getApiSecret(),
                    accessToken.getToken(), accessToken.getTokenSecret());
            setupConnection(connection);
            // check if the OAuth credentials are valid by fetching the username
            String currentUser = connection.getCurrentUser();
            if (currentUser == null || currentUser.equals("")) {
                connection = null;
            } else if (rememberCredentials) {
                ArrayNode array = ParsingUtilities.mapper.createArrayNode();
                ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
                obj.put("access_token", accessToken.getToken());
                obj.put("access_secret", accessToken.getTokenSecret());
                array.add(obj);
                prefStore.put(PREFERENCE_STORE_KEY, array);
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            connection = null;
        }
    }

    /**
     * Restore any previously saved connection, from the preferences.
     */
    private void restoreSavedConnection() {
        ObjectNode savedCredentials = getStoredCredentials();
        if (savedCredentials != null) {
            if (savedCredentials.has("username") && savedCredentials.has("password")) {
                connection = BasicApiConnection.getWikidataApiConnection();
                setupConnection(connection);
                try {
                    String username = savedCredentials.get("username").asText();
                    String password = savedCredentials.get("password").asText();
                    ((BasicApiConnection) connection).login(username, password);
                    logger.info("Successfully restored connection from saved Wikidata username/password");
                } catch (LoginFailedException e) {
                    connection = null;
                }
            } else if (savedCredentials.has("access_token") && savedCredentials.has("access_secret")) {
                String accessToken = savedCredentials.get("access_token").asText();
                String accessSecret = savedCredentials.get("access_secret").asText();
                connection = new OAuthApiConnection(ApiConnection.URL_WIKIDATA_API,
                        CLIENT_ID, CLIENT_SECRET,
                        accessToken, accessSecret);
                setupConnection(connection);
                String currentUser = connection.getCurrentUser();
                if (currentUser == null || currentUser.equals("")) {
                    connection = null;
                } else {
                    logger.info("Successfully restored connection from saved Wikidata OAuth access token/secret");
                }
            }
        }
    }

    private ObjectNode getStoredCredentials() {
        ArrayNode array = (ArrayNode) prefStore.get(PREFERENCE_STORE_KEY);
        if (array != null && array.size() > 0 && array.get(0) instanceof ObjectNode) {
            return (ObjectNode) array.get(0);
        }
        return null;
    }

    public void logout() {
        prefStore.put(PREFERENCE_STORE_KEY, ParsingUtilities.mapper.createArrayNode());
        if (connection != null) {
            try {
                connection.logout();
                connection = null;
            } catch (IOException | MediaWikiApiErrorException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public ApiConnection getConnection() {
        return connection;
    }

    public boolean isLoggedIn() {
        return connection != null;
    }

    public String getUsername() {
        if (connection != null) {
            return connection.getCurrentUser();
        } else {
            return null;
        }
    }

    /**
     * Set whether to store these credentials in the preferences (unencrypted!)
     */
    public void setRememberCredentials(boolean rememberCredentials) {
        this.rememberCredentials = rememberCredentials;
    }


    protected void setupConnection(ApiConnection connection) {
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
    }

    public String getAuthorizationUrl() throws InterruptedException, ExecutionException, IOException {
        assert mediaWikiService != null;
        requestToken = mediaWikiService.getRequestToken();
        return mediaWikiService.getAuthorizationUrl(requestToken);
    }

    public boolean supportOAuth() {
        return mediaWikiService != null;
    }

}
