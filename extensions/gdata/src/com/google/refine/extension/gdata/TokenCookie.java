package com.google.refine.extension.gdata;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.util.CookiesUtilities;

public class TokenCookie {

    private static final String COOKIE_NAME = "oauth2_token";
    private static int MAX_AGE = 30 * 24 * 60 * 60; // 30 days

    public static String getToken(HttpServletRequest request) {
        Cookie cookie = CookiesUtilities.getCookie(request, COOKIE_NAME);
        return (cookie == null) ? null : cookie.getValue();
    }

    public static void setToken(HttpServletRequest request,
            HttpServletResponse response, String token) {
        CookiesUtilities.setCookie(request, response, COOKIE_NAME, token, MAX_AGE);
    }

    public static void deleteToken(HttpServletRequest request,
            HttpServletResponse response) {
        CookiesUtilities.deleteCookie(request, response, COOKIE_NAME);
    }

}
