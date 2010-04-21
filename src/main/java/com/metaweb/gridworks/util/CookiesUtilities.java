package com.metaweb.gridworks.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookiesUtilities {

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

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie cookie = getCookie(request, name);
        if (cookie != null) {
            Cookie delCookie = new Cookie(cookie.getName(), cookie.getValue());
            delCookie.setDomain(cookie.getDomain());
            delCookie.setPath(cookie.getPath());
            delCookie.setMaxAge(0);
            response.addCookie(delCookie);            
        }
    }
    
}
