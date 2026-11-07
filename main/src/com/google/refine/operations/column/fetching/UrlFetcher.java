package com.google.refine.operations.column.fetching;

import java.io.Serializable;

/**
 * Component of the Decorator pattern used by
 * {@link com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation}.
 * <p>
 * An implementation knows how to obtain the response for a URL. Behaviours such as caching are added by wrapping one
 * {@link UrlFetcher} inside another (see {@link CachingUrlFetcher}), so callers only ever depend on this interface.
 */
public interface UrlFetcher {

    /**
     * Fetches the given URL.
     *
     * @param urlString
     *            the URL to fetch
     * @return the response body; an {@link com.google.refine.expr.EvalError} if the fetch failed and the fetcher was
     *         configured to store errors; or {@code null} if the fetch failed and errors are to be ignored
     */
    Serializable fetch(String urlString);
}