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
package org.openrefine.wikidata.commands;

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
 * Manages a connection to Wikidata.
 * <p>
 * The connection can be either {@link BasicApiConnection} or {@link OAuthApiConnection}.
 * <p>
 * This class is also hard-coded for Wikidata,
 * it will be generalized to other Wikibase instances soon.
 *
 * @author Antonin Delpeuch
 * @author Lu Liu
 */

public class ConnectionManager {

    final static Logger logger = LoggerFactory.getLogger("connection_manager");

    /**
     * We used this key to read/write credentials from/to preferences in the past, which is insecure.
     * Now this key is kept only to delete those credentials in the preferences.
     */
    public static final String PREFERENCE_STORE_KEY = "wikidata_credentials";

    public static final int CONNECT_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;

    /**
     * For now, this class is hard-coded for Wikidata.
     * <p>
     * It will be generalized to work against other Wikibase instances in the future.
     */
    private static final String WIKIBASE_API_ENDPOINT = ApiConnection.URL_WIKIDATA_API;

    /**
     * The single {@link ApiConnection} instance managed by {@link ConnectionManager}.
     * <p>
     * Currently, only one connection is supported at the same time.
     */
    private ApiConnection connection;

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
     * @param username the username to log in with
     * @param password the password to log in with
     * @return true if logged in successfully, false otherwise
     */
    public boolean login(String username, String password) {
        connection = new BasicApiConnection(WIKIBASE_API_ENDPOINT);
        setupConnection(connection);
        try {
            ((BasicApiConnection) connection).login(username, password);
            return true;
        } catch (LoginFailedException e) {
            logger.error(e.getMessage());
            connection = null;
            return false;
        }
    }

    /**
     * Logs in to the Wikibase instance, using owner-only consumer.
     * <p>
     * If failed to login, the connection will be set to null.
     *
     * @param consumerToken  consumer token of an owner-only consumer
     * @param consumerSecret consumer secret of an owner-only consumer
     * @param accessToken    access token of an owner-only consumer
     * @param accessSecret   access secret of an owner-only consumer
     * @return true if logged in successfully, false otherwise
     */
    public boolean login(String consumerToken, String consumerSecret,
                         String accessToken, String accessSecret) {
        connection = new OAuthApiConnection(WIKIBASE_API_ENDPOINT,
                consumerToken, consumerSecret,
                accessToken, accessSecret);
        setupConnection(connection);
        try {
            // check if the credentials are valid
            connection.checkCredentials();
            return true;
        } catch (IOException | MediaWikiApiErrorException e) {
            logger.error(e.getMessage());
            connection = null;
            return false;
        }
    }


    /**
     * Logs in to the Wikibase instance, using cookies.
     * <p>
     * If failed to login, the connection will be set to null.
     *
     * @param username the username
     * @param cookies  the cookies used to login
     * @return true if logged in successfully, false otherwise
     */
    public boolean login(String username, List<Cookie> cookies) {
        cookies.forEach(cookie -> cookie.setPath("/"));
        Map<String, Object> map = new HashMap<>();
        map.put("baseUrl", WIKIBASE_API_ENDPOINT);
        map.put("cookies", cookies);
        map.put("username", username);
        map.put("loggedIn", true);
        map.put("tokens", Collections.emptyMap());
        map.put("connectTimeout", CONNECT_TIMEOUT);
        map.put("readTimeout", READ_TIMEOUT);
        try {
            BasicApiConnection newConnection = convertToBasicApiConnection(map);
            newConnection.checkCredentials();
            connection = newConnection;
            return true;
        } catch (IOException | MediaWikiApiErrorException e) {
            logger.error(e.getMessage());
            connection = null;
            return false;
        }
    }

    /**
     * For testability.
     */
    static BasicApiConnection convertToBasicApiConnection(Map<String, Object> map) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(map);
        return mapper.readValue(json, BasicApiConnection.class);
    }


    public void logout() {
        if (connection != null) {
            try {
                connection.logout();
                connection = null;
            } catch (IOException e) {
                logger.error(e.getMessage());
            } catch (MediaWikiApiErrorException e) {
                if ("assertuserfailed".equals(e.getErrorCode())) {
                    // it turns out we were already logged out
                    connection = null;
                } else {
                    logger.error(e.getMessage());
                }
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

    private void setupConnection(ApiConnection connection) {
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
    }
}
