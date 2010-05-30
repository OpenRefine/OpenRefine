package com.metaweb.gridworks.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookiesUtilities {

    public static final String DOMAIN = "127.0.0.1";
    public static final String PATH = "/";
    
    public static Cookie getCookie(HttpServletRequest request, String name) {
        if (name == null) throw new RuntimeException("cookie name cannot be null");
        Cookie cookie = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (name.equals(c.getName())) {
                    cookie = c;
                }
            }
        }
        return cookie;
    }

    public static void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int max_age) {
        Cookie c = new Cookie(name, value);
        c.setDomain(getDomain(request));
        c.setPath(PATH);
        c.setMaxAge(max_age);
        response.addCookie(c);            
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie c = new Cookie(name, "");
        c.setDomain(getDomain(request));
        c.setPath(PATH);
        c.setMaxAge(0);
        response.addCookie(c);            
    }
    
    public static String getDomain(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null) return DOMAIN;
        int index = host.indexOf(':');
        return (index > -1) ? host.substring(0,index) : host ;
    }
}
