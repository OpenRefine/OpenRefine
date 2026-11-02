package com.google.refine.operations.column.fetching;

import java.io.Serializable;
import java.time.Duration;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Decorator that adds response caching to any {@link UrlFetcher}.
 * <p>
 * Successful results (including stored errors) are cached. A {@code null} result from the wrapped fetcher is never
 * cached, so a failed URL is fetched again the next time it is requested.
 */
public class CachingUrlFetcher implements UrlFetcher {

    public static final long DEFAULT_MAX_ENTRIES = 2048;
    public static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(10);

    private final UrlFetcher delegate;
    private final Cache<String, Serializable> cache;

    public CachingUrlFetcher(UrlFetcher delegate) {
        this(delegate, DEFAULT_MAX_ENTRIES, DEFAULT_EXPIRATION);
    }

    public CachingUrlFetcher(UrlFetcher delegate, long maxEntries, Duration expiration) {
        Preconditions.checkNotNull(delegate, "delegate must not be null");
        Preconditions.checkArgument(maxEntries > 0, "maxEntries must be positive");
        Preconditions.checkNotNull(expiration, "expiration must not be null");
        this.delegate = delegate;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maxEntries)
                .expireAfterWrite(expiration)
                .build();
    }

    @Override
    public Serializable fetch(String urlString) {
        Serializable cachedResponse = cache.getIfPresent(urlString);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Serializable response = delegate.fetch(urlString);
        if (response != null) {
            cache.put(urlString, response);
        }
        return response;
    }
}
