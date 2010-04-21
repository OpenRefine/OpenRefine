package com.metaweb.gridworks.commands.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.oauth.OAuthUtilities;
import com.metaweb.gridworks.oauth.Provider;

public class AuthorizationCommand extends Command {

    private static final String PROVIDER_PARAM = "provider";

    protected void respond(HttpServletResponse response, String status, String message) throws IOException, JSONException {
        JSONWriter writer = new JSONWriter(response.getWriter());
        writer.object();
        writer.key("status"); writer.value(status);
        writer.key("message"); writer.value(message);
        writer.endObject();
    }
    
    protected Provider getProvider(HttpServletRequest request) {
        String provider_str = request.getParameter(PROVIDER_PARAM);
        Provider provider = OAuthUtilities.getProvider(request.getParameter(PROVIDER_PARAM));
        if (provider == null) throw new RuntimeException("Can't find OAuth provider '" + provider_str + "'");
        return provider;
    }
}
