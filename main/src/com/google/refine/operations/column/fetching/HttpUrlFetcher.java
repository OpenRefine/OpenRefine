package com.google.refine.operations.column.fetching;

import java.io.IOException;
import java.io.Serializable;

import org.apache.hc.core5.http.Header;

import com.google.refine.expr.EvalError;
import com.google.refine.operations.OnError;
import com.google.refine.util.HttpClient;

/**
 * Concrete component of the Decorator pattern: fetches a URL over HTTP and applies the "on error" policy chosen by the
 * user (store the error in the cell, or ignore it).
 */
public class HttpUrlFetcher implements UrlFetcher {

    private final HttpClient httpClient;
    private final Header[] headers;
    private final OnError onError;

    public HttpUrlFetcher(HttpClient httpClient, Header[] headers, OnError onError) {
        this.httpClient = httpClient;
        this.headers = headers;
        this.onError = onError;
    }

    @Override
    public Serializable fetch(String urlString) {
        try {
            return httpClient.getAsString(urlString, headers);
        } catch (IOException e) {
            return onError == OnError.StoreError ? new EvalError(e) : null;
        } catch (Exception e) {
            return onError == OnError.StoreError ? new EvalError(e.getMessage()) : null;
        }
    }
}