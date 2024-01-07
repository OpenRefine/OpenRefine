
package com.google.refine.extension.gdata;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.refine.util.CookiesUtilities;

public class TokenCookie {

    private static final String COOKIE_NAME = "oauth2_token";

    public static String getToken(HttpServletRequest request) {
        Cookie cookie = CookiesUtilities.getCookie(request, COOKIE_NAME);
        return (cookie == null) ? null : cookie.getValue();
    }

    public static void setToken(HttpServletRequest request,
            HttpServletResponse response, String token, String expiresInSeconds) {
        CookiesUtilities.setCookie(request, response, COOKIE_NAME, token,
                Integer.parseInt(expiresInSeconds));
    }

    public static void deleteToken(HttpServletRequest request,
            HttpServletResponse response) {
        CookiesUtilities.deleteCookie(request, response, COOKIE_NAME);
    }

}
