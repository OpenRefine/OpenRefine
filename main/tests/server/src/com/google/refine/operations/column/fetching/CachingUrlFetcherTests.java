package com.google.refine.operations.column.fetching;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;

public class CachingUrlFetcherTests {

    static class CountingFetcher implements UrlFetcher {

        final Map<String, Integer> calls = new HashMap<>();
        final Map<String, Serializable> responses = new HashMap<>();

        @Override
        public Serializable fetch(String urlString) {
            calls.merge(urlString, 1, Integer::sum);
            return responses.get(urlString);
        }

        int callsFor(String url) {
            return calls.getOrDefault(url, 0);
        }
    }

    @Test
    public void repeatedRequestsAreServedFromCache() {
        CountingFetcher delegate = new CountingFetcher();
        delegate.responses.put("http://a", "body A");
        UrlFetcher fetcher = new CachingUrlFetcher(delegate);

        assertEquals(fetcher.fetch("http://a"), "body A");
        assertEquals(fetcher.fetch("http://a"), "body A");
        assertEquals(fetcher.fetch("http://a"), "body A");

        assertEquals(delegate.callsFor("http://a"), 1);
    }

    @Test
    public void differentUrlsAreCachedSeparately() {
        CountingFetcher delegate = new CountingFetcher();
        delegate.responses.put("http://a", "body A");
        delegate.responses.put("http://b", "body B");
        UrlFetcher fetcher = new CachingUrlFetcher(delegate);

        assertEquals(fetcher.fetch("http://a"), "body A");
        assertEquals(fetcher.fetch("http://b"), "body B");
        assertEquals(fetcher.fetch("http://a"), "body A");

        assertEquals(delegate.callsFor("http://a"), 1);
        assertEquals(delegate.callsFor("http://b"), 1);
    }

    @Test
    public void nullResultsAreNotCached() {
        CountingFetcher delegate = new CountingFetcher();
        UrlFetcher fetcher = new CachingUrlFetcher(delegate);

        assertNull(fetcher.fetch("http://down"));
        assertNull(fetcher.fetch("http://down"));

        assertEquals(delegate.callsFor("http://down"), 2);
    }

    @Test
    public void errorResultsAreCached() {
        CountingFetcher delegate = new CountingFetcher();
        EvalError error = new EvalError("request failed");
        delegate.responses.put("http://error", error);
        UrlFetcher fetcher = new CachingUrlFetcher(delegate);

        assertEquals(fetcher.fetch("http://error"), error);
        assertEquals(fetcher.fetch("http://error"), error);

        assertEquals(delegate.callsFor("http://error"), 1);
    }

    @Test
    public void entriesExpire() {
        CountingFetcher delegate = new CountingFetcher();
        delegate.responses.put("http://a", "body A");
        UrlFetcher fetcher = new CachingUrlFetcher(delegate, 10, Duration.ZERO);

        fetcher.fetch("http://a");
        fetcher.fetch("http://a");

        assertEquals(delegate.callsFor("http://a"), 2);
    }
}
