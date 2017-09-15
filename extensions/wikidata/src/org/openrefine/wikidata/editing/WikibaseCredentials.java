package org.openrefine.wikidata.editing;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

/**
 * This is just the necessary bits to store Wikidata credentials
 * in OpenRefine's preference store.
 * 
 * @author antonin
 *
 */
class WikibaseCredentials implements Jsonizable {
    
    private String username;
    private String password;
    
    public WikibaseCredentials() {
        username = null;
        password = null;
    }
    
    public WikibaseCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public boolean isNonNull() {
        return username != null && password != null && ! "null".equals(username) && ! "null".equals(password);
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("class");
        writer.value(this.getClass().getName());
        writer.key("username");
        writer.value(username);
        writer.key("password");
        writer.value(password);
        writer.endObject();
    }
    
    public static WikibaseCredentials load(JSONObject obj) throws JSONException {
        return new WikibaseCredentials(
                obj.getString("username"),
                obj.getString("password"));
    }
    
}

