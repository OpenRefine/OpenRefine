package org.freeyourmetadata.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Static methods that operate on URIs.
 * @author Ruben Verborgh
 */
public final class UriUtil {
    /** The empty URI **/
    public final static URI EMPTYURI = createUri("");
    
    /**
     * Private constructor to avoid instance creation.
     */
    private UriUtil() { }
    
    /**
     * Creates a URI from the specified string without throwing a <tt>URISyntaxException</tt>.
     * @param uri The URI in string format
     * @return The URI
     */
    public static URI createUri(String uri) {
        try {
            return new URI(uri);
        }
        catch (URISyntaxException e) {
            return EMPTYURI;
        }
    }
}
