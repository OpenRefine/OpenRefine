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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;

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

    public static final int CONNECT_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;

    /**
     * For now, this class is hard-coded for Wikidata.
     * <p>
     * It will be generalized to work against other Wikibase instances in the future.
     */
    private static final String API_ENDPOINT = ApiConnection.URL_WIKIDATA_API;

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
        ; // do nothing
    }

    /**
     * Logs in to the Wikibase instance, using username/password.
     *
     * @param username the username to log in with
     * @param password the password to log in with
     */
    public void login(String username, String password) {
        connection = new BasicApiConnection(API_ENDPOINT);
        setupConnection(connection);
        try {
            ((BasicApiConnection) connection).login(username, password);
        } catch (LoginFailedException e) {
            logger.error(e.getMessage());
            connection = null;
        }
    }

    /**
     * Logs in to the Wikibase instance, using owner-only consumer.
     *
     * @param consumerToken    consumer token of an owner-only consumer
     * @param consumerSecret consumer secret of an owner-only consumer
     * @param accessToken    access token of an owner-only consumer
     * @param accessSecret   access secret of an owner-only consumer
     */
    public void login(String consumerToken, String consumerSecret,
                      String accessToken, String accessSecret) {
        connection = new OAuthApiConnection(API_ENDPOINT,
                consumerToken, consumerSecret,
                accessToken, accessSecret);
        setupConnection(connection);
        try {
            // check if the credentials are valid
            connection.checkCredentials();
        } catch (IOException | MediaWikiApiErrorException e) {
            logger.error(e.getMessage());
            connection = null;
        }
    }

    public void logout() {
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

    private void setupConnection(ApiConnection connection) {
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
    }
}
