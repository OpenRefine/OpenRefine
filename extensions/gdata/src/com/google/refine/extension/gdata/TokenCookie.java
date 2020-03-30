package com.google.refine.extension.gdata;

import com.google.refine.util.CookiesUtilities;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
