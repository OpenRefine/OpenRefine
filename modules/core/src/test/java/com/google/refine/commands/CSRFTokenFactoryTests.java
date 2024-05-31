
package com.google.refine.commands;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.Instant;

import org.testng.annotations.Test;

public class CSRFTokenFactoryTests {

    static class CSRFTokenFactoryStub extends CSRFTokenFactory {

        public CSRFTokenFactoryStub(long timeToLive, int tokenLength) {
            super(timeToLive, tokenLength);
        }

        public void tamperWithToken(String token, Instant newGenerationTime) {
            tokenCache.asMap().put(token, newGenerationTime);
        }
    }

    @Test
    public void testGenerateValidToken() {
        CSRFTokenFactory factory = new CSRFTokenFactory(10, 25);
        // Generate a fresh token
        String token = factory.getFreshToken();
        // Immediately after, the token is still valid
        assertTrue(factory.validToken(token));
        // The token has the right length
        assertEquals(25, token.length());
    }

    @Test
    public void testInvalidToken() {
        CSRFTokenFactory factory = new CSRFTokenFactory(10, 25);
        assertFalse(factory.validToken("bogusToken"));
    }

    @Test
    public void testOldToken() {
        CSRFTokenFactoryStub stub = new CSRFTokenFactoryStub(10, 25);
        // Generate a fresh token
        String token = stub.getFreshToken();
        // Manually change the generation time
        stub.tamperWithToken(token, Instant.now().minusSeconds(100));
        // The token should now be invalid
        assertFalse(stub.validToken(token));
    }
}
