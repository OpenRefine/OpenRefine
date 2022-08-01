
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsRequestInitializer;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;

import edu.mit.simile.butterfly.ButterflyModule;

abstract public class GoogleAPIExtension {

    protected static final String SERVICE_APP_NAME = "OpenRefine-Google-Service";

    // For a production release, the second parameter (default value) can be set
    // for the following three properties (client_id, client_secret, and API key) to
    // the production values from the Google API console
    private static final String CLIENT_ID = System.getProperty("ext.gdata.clientid", "");
    private static final String CLIENT_SECRET = System.getProperty("ext.gdata.clientsecret", "");
    private static final String API_KEY = System.getProperty("ext.gdata.apikey", "");

    /** Global instance of the HTTP transport. */
    protected static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    protected static final JsonFactory JSON_FACTORY = new GsonFactory();

    private static final String[] SCOPES = { DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS };

    private static PreferenceStore prefStore = ProjectManager.singleton.getPreferenceStore();

    private static final String CONNECT_TIME_OUT_KEY = "googleConnectTimeOut";
    private static final String READ_TIME_OUT_KEY = "googleReadTimeOut";
    private static final int CONNECT_TIME_OUT_DEFAULT = 3 * 60000; // 3 minutes connect timeout
    private static final int READ_TIME_OUT_DEFAULT = 3 * 60000; // 3 minutes read timeout

    static public String getAuthorizationUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        String host = request.getHeader("Host");
        if (CLIENT_ID.equals("") || CLIENT_SECRET.equals("")) {
            return "https://github.com/OpenRefine/OpenRefine/wiki/Google-Extension#missing-credentials";
        }
        String authorizedUrl = makeRedirectUrl(module, request);
        String state = makeState(module, request);

        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(
                CLIENT_ID,
                authorizedUrl, // execution continues at authorized on redirect
                Arrays.asList(SCOPES));
        url.setState(state);

        return url.toString();

    }

    private static String makeState(ButterflyModule module, HttpServletRequest request) {
        String winname = request.getParameter("winname");
        String cb = request.getParameter("cb");
        String json = "{\"winname\":\"" + winname.replaceAll("\"", "\\\"")
                + "\",\"cb\":\"" + cb.replaceAll("\"", "\\\"") + "\"}";
        return new String(Base64.getEncoder().encode(json.getBytes()));
    }

    private static String makeRedirectUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        StringBuffer sb = new StringBuffer(module.getMountPoint().getMountPoint());
        sb.append("authorized");

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
        AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(fullUrlBuf.toString());
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
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).build().setAccessToken(token);

        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setHttpRequestInitializer(new HttpRequestInitializer() {

            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                credential.initialize(httpRequest);
                httpRequest.setConnectTimeout(3 * 60000);
                httpRequest.setReadTimeout(3 * 60000);
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
        return getParamValue(url, "key");
    }

    static private String getParamValue(URL url, String key) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith(key + "=")) {
                    int offset = key.length() + 1;
                    String tableId = part.substring(offset);
                    return tableId;
                }
            }
        }
        return null;
    }

    /**
     * Build and return an authorized Sheets API client service.
     * 
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService(String token) throws IOException {
        final Credential credential;
        if (token != null) {
            credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).build().setAccessToken(token);
        } else {
            credential = null;
        }
        int connectTimeout = getConnectTimeout();
        int readTimeout = getReadTimeout();

        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(SERVICE_APP_NAME)
                .setSheetsRequestInitializer(new SheetsRequestInitializer(API_KEY))
                .setHttpRequestInitializer(new HttpRequestInitializer() {

                    @Override
                    public void initialize(HttpRequest httpRequest) throws IOException {
                        if (credential != null) {
                            credential.initialize(httpRequest);
                        }
                        httpRequest.setConnectTimeout(connectTimeout);
                        httpRequest.setReadTimeout(readTimeout);
                    }
                }).build();
    }

    private static int getConnectTimeout() {
        return prefStore.get(CONNECT_TIME_OUT_KEY) == null ? CONNECT_TIME_OUT_DEFAULT
                : Integer.parseInt((String) prefStore.get(CONNECT_TIME_OUT_KEY));
    }

    private static int getReadTimeout() {
        return prefStore.get(READ_TIME_OUT_KEY) == null ? READ_TIME_OUT_DEFAULT
                : Integer.parseInt((String) prefStore.get(READ_TIME_OUT_KEY));
    }

    public static String extractSpreadSheetId(String url)
            throws IllegalArgumentException {
        URL urlAsUrl;

        Matcher matcher = Pattern.compile("(?<=/d/).*?(?=[/?#]|$)").matcher(url);
        if (matcher.find()) {
            return matcher.group(0);
        }

        try {
            urlAsUrl = new URL(url);
            String query = urlAsUrl.getQuery();
            if (query != null) {

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
        } catch (MalformedURLException e) {
            // This is not a URL, maybe it is just an id
            String[] dottedParts = url.split("\\.");

            if (dottedParts.length == 4 || dottedParts.length == 2) {
                return dottedParts[0] + "." + dottedParts[1];
            }
        }

        throw new IllegalArgumentException("Unknown URL format.");
    }
}
