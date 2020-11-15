package com.google.refine.util;

import static org.testng.Assert.assertEquals;

import java.net.MalformedURLException;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;

public class HttpClientTests extends RefineTest {

    @Test
    public void testReverseUrl() throws MalformedURLException {
        assertEquals(HttpClient.reverseURL("http://openrefine.org:80/a/b?q=foo"),
                "http://org.openrefine:80/a/b?q=foo");
        assertEquals(HttpClient.reverseURL("https://alpha.reconciliation.opencorporates.com"),
                "https://com.opencorporates.reconciliation.alpha");
    }
}
