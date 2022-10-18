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

package org.openrefine.wikibase.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a connection to the current Wikibase instance.
 * <p>
 * The connection can be either {@link BasicApiConnection} or {@link OAuthApiConnection}.
 * <p>
 * 
 * @author Antonin Delpeuch
 * @author Lu Liu
 */

public class ConnectionManager {

    final static Logger logger = LoggerFactory.getLogger("connection_manager");

    /**
     * We used this key to read/write credentials from/to preferences in the past, which is insecure. Now this key is
     * kept only to delete those credentials in the preferences.
     */
    public static final String PREFERENCE_STORE_KEY = "wikidata_credentials";

    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 60000;

    private Map<String, ApiConnection> endpointToConnection = new HashMap<>();

    private static final ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        PreferenceStore prefStore = ProjectManager.singleton.getPreferenceStore();
        // remove credentials stored in the preferences
        prefStore.put(PREFERENCE_STORE_KEY, null);
    }

    /**
     * Logs in to the Wikibase instance, using username/password.
     * <p>
     * If failed to login, the connection will be set to null.
     *
     * @param mediaWikiApiEndpoint
     *            the api endpoint of the target Wikibase instance
     * @param username
     *            the username to log in with
     * @param password
     *            the password to log in with
     * @return true if logged in successfully, false otherwise
     */
    public boolean login(String mediaWikiApiEndpoint, String username, String password) {
        BasicApiConnection connection = new BasicApiConnection(mediaWikiApiEndpoint);
        setupConnection(connection);
        try {
            connection.login(username, password);
            endpointToConnection.put(mediaWikiApiEndpoint, connection);
            return true;
        } catch (LoginFailedException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Logs in to the Wikibase instance, using owner-only consumer.
     * <p>
     * If failed to login, the connection will be set to null.
     *
     * @param mediaWikiApiEndpoint
     *            the api endpoint of the target Wikibase instance
     * @param consumerToken
     *            consumer token of an owner-only consumer
     * @param consumerSecret
     *            consumer secret of an owner-only consumer
     * @param accessToken
     *            access token of an owner-only consumer
     * @param accessSecret
     *            access secret of an owner-only consumer
     * @return true if logged in successfully, false otherwise
     */
    public boolean login(String mediaWikiApiEndpoint, String consumerToken, String consumerSecret,
            String accessToken, String accessSecret) {
        OAuthApiConnection connection = new OAuthApiConnection(mediaWikiApiEndpoint,
                consumerToken, consumerSecret,
                accessToken, accessSecret);
        setupConnection(connection);
        try {
            // check if the credentials are valid
            connection.checkCredentials();
            endpointToConnection.put(mediaWikiApiEndpoint, connection);
            return true;
        } catch (IOException | MediaWikiApiErrorException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Logs in to the Wikibase instance, using cookies.
     * <p>
     * If failed to login, the connection will be set to null.
     *
     * @param mediaWikiApiEndpoint
     *            the api endpoint of the target Wikibase instance
     * @param username
     *            the username
     * @param cookies
     *            the cookies used to login
     * @return true if logged in successfully, false otherwise
     */
    public boolean login(String mediaWikiApiEndpoint, String username, List<Cookie> cookies) {
        cookies.forEach(cookie -> cookie.setPath("/"));
        Map<String, Object> map = new HashMap<>();
        map.put("baseUrl", mediaWikiApiEndpoint);
        map.put("cookies", cookies);
        map.put("username", username);
        map.put("loggedIn", true);
        map.put("tokens", Collections.emptyMap());
        map.put("connectTimeout", CONNECT_TIMEOUT);
        map.put("readTimeout", READ_TIMEOUT);
        try {
            BasicApiConnection connection = convertToBasicApiConnection(map);
            connection.checkCredentials();
            endpointToConnection.put(mediaWikiApiEndpoint, connection);
            return true;
        } catch (IOException | MediaWikiApiErrorException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * For testability.
     */
    BasicApiConnection convertToBasicApiConnection(Map<String, Object> map) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(map);
        return mapper.readValue(json, BasicApiConnection.class);
    }

    public void logout(String mediaWikiApiEndpoint) {
        ApiConnection connection = endpointToConnection.get(mediaWikiApiEndpoint);
        if (connection != null) {
            try {
                connection.logout();
                endpointToConnection.remove(mediaWikiApiEndpoint);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (MediaWikiApiErrorException e) {
                if ("assertuserfailed".equals(e.getErrorCode())) {
                    // it turns out we were already logged out
                    endpointToConnection.remove(mediaWikiApiEndpoint);
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public ApiConnection getConnection(String mediaWikiApiEndpoint) {
        return endpointToConnection.get(mediaWikiApiEndpoint);
    }

    public boolean isLoggedIn(String mediaWikiApiEndpoint) {
        return endpointToConnection.get(mediaWikiApiEndpoint) != null;
    }

    public String getUsername(String mediaWikiApiEndpoint) {
        ApiConnection connection = endpointToConnection.get(mediaWikiApiEndpoint);
        if (connection != null) {
            return connection.getCurrentUser();
        } else {
            return null;
        }
    }

    private void setupConnection(ApiConnection connection) {
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
    }
}
