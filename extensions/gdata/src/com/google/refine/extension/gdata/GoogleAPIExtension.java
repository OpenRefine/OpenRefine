package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.fusiontables.FusiontablesScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

abstract public class GoogleAPIExtension {
    protected static final String SERVICE_APP_NAME = "OpenRefine-Google-Service";
    private static final String CLIENT_ID = "455686949425-d237cmorii0ge8if7it5r1qijce6caf0.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "wm5qVtjp3VDfuAx2P2qm6GJb"; 
    
    /** Global instance of the HTTP transport. */
    protected static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    protected static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private static final String[] SCOPES = {DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS, FusiontablesScopes.FUSIONTABLES};
    
    static public String getAuthorizationUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        String authorizedUrl = makeRedirectUrl(module, request);
              
        
        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(
                CLIENT_ID, 
                authorizedUrl, // execution continues at authorized on redirect
                Arrays.asList(SCOPES));
        
        return url.toString();

    }

    private static String makeRedirectUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        StringBuffer sb = new StringBuffer(module.getMountPoint().getMountPoint());
        sb.append("authorized?winname=");
        sb.append(ParsingUtilities.encode(request.getParameter("winname")));
        sb.append("&cb=");
        sb.append(ParsingUtilities.encode(request.getParameter("cb")));
    
        URL thisUrl = new URL(request.getRequestURL().toString());
        URL authorizedUrl = new URL(thisUrl, sb.toString());

        return authorizedUrl.toExternalForm();
    }

    static public String getTokenFromCode(ButterflyModule module, HttpServletRequest request) 
            throws IOException {
        String redirectUrl = makeRedirectUrl(module, request);
        StringBuffer fullUrlBuf = request.getRequestURL();
        if (request.getQueryString() != null) {
          fullUrlBuf.append('?').append(request.getQueryString());
        }
        AuthorizationCodeResponseUrl authResponse =
            new AuthorizationCodeResponseUrl(fullUrlBuf.toString());
        // check for user-denied error
        if (authResponse.getError() != null) {
          // authorization denied...
        } else {
          // request access token using authResponse.getCode()...
            String code = authResponse.getCode();
            GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(HTTP_TRANSPORT,
                    JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, code, redirectUrl).execute();
            String tokenAndExpiresInSeconds = response.getAccessToken() + "," + response.getExpiresInSeconds();
            return tokenAndExpiresInSeconds;
        }
        return null;
      }
    
    static public Drive getDriveService(String token) {
        GoogleCredential credential = new GoogleCredential().setAccessToken(token);
        
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setHttpRequestInitializer(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                credential.initialize(httpRequest);
                httpRequest.setConnectTimeout(3 * 60000);  // 3 minutes connect timeout
                httpRequest.setReadTimeout(3 * 60000);  // 3 minutes read timeout
            }
        })
          .setApplicationName(SERVICE_APP_NAME).build();
    }
    
    static boolean isSpreadsheetURL(String url) {
        try {
            return url.contains("spreadsheet") && getSpreadsheetID(new URL(url)) != null;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    static String getSpreadsheetID(URL url) {
        return getParamValue(url,"key");
    }
    
    static private String getParamValue(URL url, String key) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith(key+"=")) {
                    int offset = key.length()+1;
                    String tableId = part.substring(offset);
                    return tableId;
                }
            }
        }
        return null; 
    }
    
    /**
     * Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService(String token) throws IOException {
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                new GoogleCredential().setAccessToken(token))
                .setApplicationName(SERVICE_APP_NAME)
                .build();
    }

    public static String extractSpreadSheetId(String url)
         throws IllegalArgumentException {
      URL urlAsUrl;
      
      Matcher matcher = Pattern.compile("(?<=\\/d\\/).*(?=\\/.*)").matcher(url);
      if (matcher.find()) {
          return matcher.group(0);
      }
      
      try {
        urlAsUrl = new URL(url);
        String query = urlAsUrl.getQuery();
        if ( query != null ) {
       
          String[] parts = query.split("&");
        
          int offset = -1;
          int numParts = 0;
          String keyOrId = "";

          for (String part : parts) {
            if (part.startsWith("id=")) {
              offset = ("id=").length();
              keyOrId = part.substring(offset);
              numParts = 4;
              break;
            } else if (part.startsWith("key=")) {
              offset = ("key=").length();
              keyOrId = part.substring(offset);
              if (!keyOrId.isEmpty()) {
                return keyOrId;
              }
              numParts = 2;
              break;
            }
          }

          if (offset > -1) {
            String[] dottedParts = keyOrId.split("\\.");
            if (dottedParts.length == numParts) {
              return dottedParts[0] + "." + dottedParts[1];
            }
          }
        }
      } catch ( MalformedURLException e ) {
        // This is not a URL, maybe it is just an id
        String[] dottedParts = url.split("\\.");
        
        if (dottedParts.length == 4 || dottedParts.length == 2) {
          return dottedParts[0] + "." + dottedParts[1];
        }    
      }
      
      throw new IllegalArgumentException("Uknown URL format.");
    }
}
