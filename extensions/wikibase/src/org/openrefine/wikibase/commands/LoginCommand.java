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

package org.openrefine.wikibase.commands;

import com.google.refine.commands.Command;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Handles login.
 * <p>
 * Both logging in with username/password or owner-only consumer are supported.
 * <p>
 * This command also manages cookies of login credentials.
 * <p>
 * Cookies for different MediaWiki API endpoint are stored, but only one connection is kept at the same time.
 */
public class LoginCommand extends Command {

    static final String WIKIBASE_COOKIE_PREFIX = "openrefine-wikibase-";

    static final String API_ENDPOINT = "wb-api-endpoint";

    static final String USERNAME = "wb-username";
    static final String PASSWORD = "wb-password";

    static final String CONSUMER_TOKEN = "wb-consumer-token";
    static final String CONSUMER_SECRET = "wb-consumer-secret";
    static final String ACCESS_TOKEN = "wb-access-token";
    static final String ACCESS_SECRET = "wb-access-secret";

    static final Pattern cookieKeyDisallowedCharacters = Pattern.compile("[^a-zA-Z0-9\\-!#$%&'*+.?\\^_`|~]");

    protected ConnectionManager manager = null;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        if (manager == null) {
            manager = ConnectionManager.getInstance();
        }

        String mediawikiApiEndpoint = removeCRLF(request.getParameter(API_ENDPOINT));
        if (isBlank(mediawikiApiEndpoint)) {
            CommandUtilities.respondError(response, "missing parameter '" + API_ENDPOINT + "'");
            return;
        }
        String mediawikiApiEndpointPrefix = sanitizeCookieKey(mediawikiApiEndpoint + '-');

        if ("true".equals(request.getParameter("logout"))) {
            manager.logout(mediawikiApiEndpoint);
            removeUsernamePasswordCookies(mediawikiApiEndpointPrefix, request, response);
            removeOwnerOnlyConsumerCookies(mediawikiApiEndpointPrefix, request, response);
            respond(request, response);
            return; // return directly
        }

        boolean remember = "on".equals(request.getParameter("remember-credentials"));

        // Credentials from parameters have higher priority than those from cookies.
        String username = request.getParameter(USERNAME);
        String password = request.getParameter(PASSWORD);
        String consumerToken = request.getParameter(CONSUMER_TOKEN);
        String consumerSecret = request.getParameter(CONSUMER_SECRET);
        String accessToken = request.getParameter(ACCESS_TOKEN);
        String accessSecret = request.getParameter(ACCESS_SECRET);

        if (isBlank(username) && isBlank(password) && isBlank(consumerToken)
                && isBlank(consumerSecret) && isBlank(accessToken) && isBlank(accessSecret)) {
            // In this case, we use cookies to login, and we will always remember the credentials in cookies.
            remember = true;
            Map<String, String> cookieMap = processCookiesWithPrefix(mediawikiApiEndpointPrefix, request.getCookies());
            username = cookieMap.get(USERNAME);
            consumerToken = cookieMap.get(CONSUMER_TOKEN);
            consumerSecret = cookieMap.get(CONSUMER_SECRET);
            accessToken = cookieMap.get(ACCESS_TOKEN);
            accessSecret = cookieMap.get(ACCESS_SECRET);

            if (isBlank(consumerToken) && isBlank(consumerSecret) && isBlank(accessToken) && isBlank(accessSecret)) {
                // Try logging in with the cookies of a password-based connection.
                List<Cookie> cookieList = new ArrayList<>();
                for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                    if (entry.getKey().startsWith(WIKIBASE_COOKIE_PREFIX)) {
                        String name = entry.getKey().substring(WIKIBASE_COOKIE_PREFIX.length());
                        Cookie newCookie = new Cookie(name, entry.getValue());
                        cookieList.add(newCookie);
                    }
                }

                if (cookieList.size() > 0 && isNotBlank(username)) {
                    removeOwnerOnlyConsumerCookies(mediawikiApiEndpointPrefix, request, response);
                    if (manager.login(mediawikiApiEndpoint, username, cookieList)) {
                        respond(request, response);
                        return;
                    } else {
                        removeUsernamePasswordCookies(mediawikiApiEndpointPrefix, request, response);
                    }
                }
            }
        }

        if (isNotBlank(username) && isNotBlank(password)) {
            // Once logged in with new credentials,
            // the old credentials in cookies should be cleared.
            if (manager.login(mediawikiApiEndpoint, username, password) && remember) {
                ApiConnection connection = manager.getConnection(mediawikiApiEndpoint);
                List<HttpCookie> cookies = ((BasicApiConnection) connection).getCookies();
                String prefix = mediawikiApiEndpointPrefix + WIKIBASE_COOKIE_PREFIX;
                for (HttpCookie cookie : cookies) {
                    setCookie(response, prefix + cookie.getName(), cookie.getValue());
                }

                // Though the cookies from the connection contain some cookies of username,
                // we cannot make sure that all Wikibase instances use the same cookie key
                // to retrieve the username. So we choose to set the username cookie with our own cookie key.
                setCookie(response, mediawikiApiEndpointPrefix + USERNAME, connection.getCurrentUser());
            } else {
                removeUsernamePasswordCookies(mediawikiApiEndpointPrefix, request, response);
            }
            removeOwnerOnlyConsumerCookies(mediawikiApiEndpointPrefix, request, response);
        } else if (isNotBlank(consumerToken) && isNotBlank(consumerSecret) && isNotBlank(accessToken) && isNotBlank(accessSecret)) {
            if (manager.login(mediawikiApiEndpoint, consumerToken, consumerSecret, accessToken, accessSecret) && remember) {
                setCookie(response, mediawikiApiEndpointPrefix + CONSUMER_TOKEN, consumerToken);
                setCookie(response, mediawikiApiEndpointPrefix + CONSUMER_SECRET, consumerSecret);
                setCookie(response, mediawikiApiEndpointPrefix + ACCESS_TOKEN, accessToken);
                setCookie(response, mediawikiApiEndpointPrefix + ACCESS_SECRET, accessSecret);
            } else {
                removeOwnerOnlyConsumerCookies(mediawikiApiEndpointPrefix, request, response);
            }
            removeUsernamePasswordCookies(mediawikiApiEndpointPrefix, request, response);
        }

        respond(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        respond(request, response);
    }

    protected void respond(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String mediawikiApiEndpoint = request.getParameter(API_ENDPOINT);
        if (isBlank(mediawikiApiEndpoint)) {
            CommandUtilities.respondError(response, "missing parameter '" + API_ENDPOINT + "'");
            return;
        }

        if (manager == null) {
            manager = ConnectionManager.getInstance();
        }

        Map<String, Object> jsonResponse = new HashMap<>();
        if (manager.isLoggedIn(mediawikiApiEndpoint)) {
            jsonResponse.put("logged_in", manager.isLoggedIn(mediawikiApiEndpoint));
            jsonResponse.put("username", manager.getUsername(mediawikiApiEndpoint));
            jsonResponse.put("mediawiki_api_endpoint", mediawikiApiEndpoint);
        } else {
            jsonResponse.put("logged_in", false);
            jsonResponse.put("username", null);
            jsonResponse.put("mediawiki_api_endpoint", mediawikiApiEndpoint);
        }

        respondJSON(response, jsonResponse);
    }

    /**
     * 1. Filters cookies with the given prefix 2. Removes the prefix
     */
    private static Map<String, String> processCookiesWithPrefix(String prefix, Cookie[] cookies) throws UnsupportedEncodingException {
        Map<String, String> result = new HashMap<>();
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            if (name.startsWith(prefix)) {
                result.put(name.substring(prefix.length()), getCookieValue(cookie));
            }
        }

        return result;
    }

    private static void removeUsernamePasswordCookies(String wikibaseApiEndpointPrefix, HttpServletRequest request,
            HttpServletResponse response) {
        String toRemovePrefix = wikibaseApiEndpointPrefix + WIKIBASE_COOKIE_PREFIX;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith(toRemovePrefix)) {
                removeCookie(response, cookie.getName());
            }
        }
        removeCookie(response, wikibaseApiEndpointPrefix + USERNAME);
    }

    private static void removeOwnerOnlyConsumerCookies(String wikibaseApiEndpointPrefix, HttpServletRequest request,
            HttpServletResponse response) {
        removeCookie(response, wikibaseApiEndpointPrefix + CONSUMER_TOKEN);
        removeCookie(response, wikibaseApiEndpointPrefix + CONSUMER_SECRET);
        removeCookie(response, wikibaseApiEndpointPrefix + ACCESS_TOKEN);
        removeCookie(response, wikibaseApiEndpointPrefix + ACCESS_SECRET);
    }

    static String getCookieValue(Cookie cookie) throws UnsupportedEncodingException {
        return URLDecoder.decode(cookie.getValue(), "utf-8");
    }

    private static void setCookie(HttpServletResponse response, String key, String value) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode(value, "utf-8");
        Cookie cookie = new Cookie(key, encodedValue);
        cookie.setMaxAge(60 * 60 * 24 * 365); // a year
        cookie.setPath("/");
        // set to false because OpenRefine doesn't require HTTPS
        cookie.setSecure(false);
        response.addCookie(cookie);
    }

    private static void removeCookie(HttpServletResponse response, String key) {
        Cookie cookie = new Cookie(key, "");
        cookie.setMaxAge(0); // 0 causes the cookie to be deleted
        cookie.setPath("/");
        cookie.setSecure(false);
        response.addCookie(cookie);
    }

    /**
     * To avoid HTTP response splitting.
     *
     * See https://lgtm.com/rules/3980077/
     */
    static String removeCRLF(String str) {
        if (str == null) {
            return "";
        } else {
            return str.replaceAll("[\n\r]", "");
        }
    }

    /**
     * Removes special characters from cookie keys, replacing them by hyphens.
     */
    static String sanitizeCookieKey(String key) {
        Matcher matcher = cookieKeyDisallowedCharacters.matcher(key);
        return matcher.replaceAll("-");
    }

    protected void setConnectionManager(ConnectionManager connectionManager) {
        this.manager = connectionManager;
    }
}
