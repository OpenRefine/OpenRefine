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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
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
 * Ideally, we should store only the cookies and not the password. But
 * Wikidata-Toolkit does not allow for that as cookies are kept private.
 * 
 * This class is also hard-coded for Wikidata: generalization to other Wikibase
 * instances should be feasible though.
 * 
 * @author Antonin Delpeuch
 */

public class ConnectionManager {
    
    final static Logger logger = LoggerFactory.getLogger("connection_mananger");

    public static final String PREFERENCE_STORE_KEY = "wikidata_credentials";
    public static final int CONNECT_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;

    private PreferenceStore prefStore;
    private BasicApiConnection connection;

    private static final ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    /**
     * Creates a connection manager, which attempts to restore any
     * previous connection (from the preferences).
     */
    private ConnectionManager() {
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
     * @param rememberCredentials
     *      whether to store these credentials in the preferences (unencrypted!)
     */
    public void login(String username, String password, boolean rememberCredentials) {
        if (rememberCredentials) {
            ArrayNode array = ParsingUtilities.mapper.createArrayNode();
            ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
            obj.put("username", username);
            obj.put("password", password);
            array.add(obj);
            prefStore.put(PREFERENCE_STORE_KEY, array);
        }

        connection = createNewConnection();
        try {
            connection.login(username, password);
        } catch (LoginFailedException e) {
            connection = null;
        }
    }

    /**
     * Restore any previously saved connection, from the preferences.
     */
    public void restoreSavedConnection() {
        ObjectNode savedCredentials = getStoredCredentials();
        if (savedCredentials != null) {
            connection = createNewConnection();
            try {
                connection.login(savedCredentials.get("username").asText(), savedCredentials.get("password").asText());
            } catch (LoginFailedException e) {
                connection = null;
            }
        }
    }

    public ObjectNode getStoredCredentials() {
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
     * Creates a fresh connection object with our
     * prefered settings.
     * @return
     */
    protected BasicApiConnection createNewConnection() {
        BasicApiConnection conn = BasicApiConnection.getWikidataApiConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        return conn;
    }
}
