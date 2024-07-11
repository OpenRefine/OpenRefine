package com.google.refine.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.common.net.UrlEscapers;

public class URLUtils {
    /**
     * URL Escape function that handles all url elements path, query, fragment
     *
     * @param s
     *            URL string to be escaped
     * @return string escaped url
     */
    public static String escapeURL(String s) {
        try {
            // Encode space initially to overcome URI exception and replae with escape sequence before calling escape
            String url = s.replace(" ", "+");
            URI uri = new URL(url).toURI();
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            String path = uri.getPath();
            String query = uri.getQuery();
            String fragment = uri.getFragment();

            String encodedPath = path == null ? "" : UrlEscapers.urlPathSegmentEscaper().escape(path.replace("+", " ")).replace("%2F", "/");
            String encodedQuery = query == null ? null : UrlEscapers.urlFormParameterEscaper().escape(query.replace("+", " "));
            String encodedFragment = fragment == null ? null : UrlEscapers.urlFragmentEscaper().escape(fragment.replace("+", " "));

            String encodedUrl = scheme + "://" + authority + encodedPath;
            if (encodedQuery != null) {
                encodedUrl += "?" + encodedQuery;
            }
            if (encodedFragment != null) {
                encodedUrl += "#" + encodedFragment;
            }
            return encodedUrl;
        }
        catch (URISyntaxException e) {
            return s; }
        catch (MalformedURLException e) {
            return s; }
    }
}
