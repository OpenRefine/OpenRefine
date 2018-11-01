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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;

import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;

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

    private PreferenceStore prefStore;
    private ApiConnection connection;

    private static final ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        prefStore = ProjectManager.singleton.getPreferenceStore();
        connection = null;
        restoreSavedConnection();
    }

    public void login(String username, String password, boolean rememberCredentials) {
        if (rememberCredentials) {
            try {
                JSONArray array = new JSONArray();
                JSONObject obj = new JSONObject();
                obj.put("username", username);
                obj.put("password", password);
                array.put(obj);
                prefStore.put(PREFERENCE_STORE_KEY, array);
            } catch (JSONException e) {
                logger.error(e.getMessage());
            }
        }

        connection = ApiConnection.getWikidataApiConnection();
        try {
            connection.login(username, password);
        } catch (LoginFailedException e) {
            connection = null;
        }
    }

    public void restoreSavedConnection() {
        JSONObject savedCredentials = getStoredCredentials();
        if (savedCredentials != null) {
            connection = ApiConnection.getWikidataApiConnection();
            try {
                connection.login(savedCredentials.getString("username"), savedCredentials.getString("password"));
            } catch (LoginFailedException e) {
                connection = null;
            } catch (JSONException e) {
                connection = null;
            }
        }
    }

    public JSONObject getStoredCredentials() {
        JSONArray array = (JSONArray) prefStore.get(PREFERENCE_STORE_KEY);
        if (array != null && array.length() > 0) {
            try {
                return array.getJSONObject(0);
            } catch (JSONException e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    public void logout() {
        prefStore.put(PREFERENCE_STORE_KEY, new JSONArray());
        if (connection != null) {
            try {
                connection.logout();
                connection = null;
            } catch (IOException e) {
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
}
