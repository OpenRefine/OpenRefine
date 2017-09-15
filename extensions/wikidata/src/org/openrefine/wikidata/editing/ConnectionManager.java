package org.openrefine.wikidata.editing;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;

import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;



/**
 * Manages a connection to Wikidata, with login credentials stored
 * in the preferences.
 * 
 * Ideally, we should store only the cookies and not the password.
 * But Wikidata-Toolkit does not allow for that as cookies are kept
 * private.
 * 
 * This class is also hard-coded for Wikidata: generalization to other
 * Wikibase instances should be feasible though.
 * 
 * @author antonin
 */

public class ConnectionManager {
    public static final String PREFERENCE_STORE_KEY = "wikidata_credentials";
    
    private PreferenceStore prefStore;
    private ApiConnection connection;
    
    private static class ConnectionManagerHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }
    
    public static ConnectionManager getInstance() {
        return ConnectionManagerHolder.instance;
    }
    
    private ConnectionManager() {
        prefStore = ProjectManager.singleton.getPreferenceStore();
        connection = null;
    }
    
    public void login(String username, String password) {
        try {
            JSONArray array = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("username", username);
            obj.put("password", password);
            array.put(obj);
            prefStore.put(PREFERENCE_STORE_KEY, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        connection = ApiConnection.getWikidataApiConnection();
        try {
            connection.login(username, password);
        } catch (LoginFailedException e) {
            connection = null;
        }
    }
    
    public void restorePreviousLogin() {
        JSONArray array = (JSONArray) prefStore.get(PREFERENCE_STORE_KEY);
        if (array.length() > 0) {
            JSONObject obj;
            try {
                obj = array.getJSONObject(0);
                String username = obj.getString("username");
                String password = obj.getString("password");
                login(username, password);
            } catch (JSONException e) {
                e.printStackTrace();
            }      
        }
    }
    
    public void logout() {
        prefStore.put(PREFERENCE_STORE_KEY, new JSONArray());
        if (connection != null) {
            try {
                connection.logout();
                connection = null;
            } catch (IOException e) {
                e.printStackTrace();
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
