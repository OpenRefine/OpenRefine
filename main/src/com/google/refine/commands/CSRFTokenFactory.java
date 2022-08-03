
package com.google.refine.commands;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;

import java.security.SecureRandom;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Generates CSRF tokens and checks their validity.
 * 
 * @author Antonin Delpeuch
 *
 */
public class CSRFTokenFactory {

    /**
     * Maps each token to the time it was generated
     */
    protected final LoadingCache<String, Instant> tokenCache;

    /**
     * Time to live for tokens, in seconds
     */
    protected final long timeToLive;

    /**
     * Length of the tokens to generate
     */
    protected final int tokenLength;

    /**
     * Random number generator used to create tokens
     */
    protected final SecureRandom rng;

    /**
     * Constructs a new CSRF token factory.
     * 
     * @param timeToLive
     *            Time to live for tokens, in seconds
     * @param tokenLength
     *            Length of the tokens generated
     */
    public CSRFTokenFactory(long timeToLive, int tokenLength) {
        tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(timeToLive, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, Instant>() {

                            @Override
                            public Instant load(String key) {
                                return Instant.now();
                            }

                        });
        this.timeToLive = timeToLive;
        this.rng = new SecureRandom();
        this.tokenLength = tokenLength;
    }

    /**
     * Generates a fresh CSRF token, which will remain valid for the configured amount of time.
     */
    public String getFreshToken() {
        // Generate a random token
        String token = RandomStringUtils.random(tokenLength, 0, 0, true, true, null, rng);
        // Put it in the cache
        try {
            tokenCache.get(token);
        } catch (ExecutionException e) {
            // cannot happen
        }
        return token;
    }

    /**
     * Checks that a given CSRF token is valid.
     * 
     * @param token
     *            the token to verify
     * @return true if the token is valid
     */
    public boolean validToken(String token) {
        Map<String, Instant> map = tokenCache.asMap();
        Instant cutoff = Instant.now().minusSeconds(timeToLive);
        return map.containsKey(token) && map.get(token).isAfter(cutoff);
    }
}
