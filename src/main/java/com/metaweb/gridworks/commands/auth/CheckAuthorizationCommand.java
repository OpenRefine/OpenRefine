package com.metaweb.gridworks.commands.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.OAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.oauth.Credentials;
import com.metaweb.gridworks.oauth.OAuthUtilities;
import com.metaweb.gridworks.oauth.Provider;
import com.metaweb.gridworks.util.IOUtils;

public class CheckAuthorizationCommand extends AuthorizationCommand {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        try {

            Provider provider = getProvider(request);
                        
            // this cookie should not be there, but this is good hygiene practice
            Credentials.deleteCredentials(request, response, provider, Credentials.Type.REQUEST);
            
            Credentials access_credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            if (access_credentials != null) {
                Gridworks.log(access_credentials.toString());
                
                OAuthConsumer consumer = OAuthUtilities.getConsumer(access_credentials, provider);
    
                HttpGet httpRequest = new HttpGet("http://" + provider.getHost() + "/api/service/user_info");
                httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Gridworks " + Gridworks.getVersion());
                
                // this is required by the Metaweb API to avoid XSS
                httpRequest.setHeader("X-Requested-With", "1");
                
                consumer.sign(request);

                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse httpResponse = httpClient.execute(httpRequest);
                OutputStream output = response.getOutputStream();
                InputStream input = httpResponse.getEntity().getContent();
                IOUtils.copy(input, output);
                input.close();
                output.close();
            }

            respond(response, "401 Unauthorized", "You don't have the right credentials");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
