
package com.google.refine.translate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.HttpUrl;
import org.testng.annotations.Test;

public class OpenAiCompatibleTranslationServiceTests {

    private static final String TRANSLATION_RESPONSE = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"translations\\":[\\"hola\\",\\"adiós\\"]}"
                  }
                }
              ]
            }
            """;

    @Test
    public void translateSendsBatchAndParsesTranslations() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/chat/completions");
            server.enqueue(new MockResponse.Builder().body(TRANSLATION_RESPONSE).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    url.toString(), "test-model", "test-key", 1000);

            List<String> translations = service.translate(
                    List.of("hello", "goodbye"), TranslationLanguage.SPANISH);

            assertEquals(translations, List.of("hola", "adiós"));

            RecordedRequest request = server.takeRequest();
            assertEquals(request.getHeaders().get("Authorization"), "Bearer test-key");
            assertTrue(request.getBody().utf8().contains("test-model"));
            assertTrue(request.getBody().utf8().contains("Translate every item in the JSON array into Spanish"));
        }
    }

    @Test
    public void translateRetriesTransientErrors() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/chat/completions");
            server.enqueue(new MockResponse.Builder().code(503).body("temporary error").build());
            server.enqueue(new MockResponse.Builder().body(TRANSLATION_RESPONSE).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    url.toString(), "test-model", "test-key", 1000, 1);

            List<String> translations = service.translate(
                    List.of("hello", "goodbye"), TranslationLanguage.SPANISH);

            assertEquals(translations, List.of("hola", "adiós"));
            assertEquals(server.getRequestCount(), 2);
        }
    }

    @Test
    public void translateRetriesTimeouts() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder()
                    .body(TRANSLATION_RESPONSE)
                    .headersDelay(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build());
            server.enqueue(new MockResponse.Builder().body(TRANSLATION_RESPONSE).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 100, 1);

            assertEquals(
                    service.translate(List.of("hello", "goodbye"), TranslationLanguage.SPANISH),
                    List.of("hola", "adiós"));
            assertEquals(server.getRequestCount(), 2);
        }
    }

    @Test
    public void translateDoesNotRetryClientErrors() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/chat/completions");
            server.enqueue(new MockResponse.Builder().code(401)
                    .body("{\"error\":{\"message\":\"invalid key\"}}").build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    url.toString(), "test-model", "test-key", 1000, 3);

            try {
                service.translate(List.of("hello"), TranslationLanguage.SPANISH);
                fail("Expected a translation exception for an invalid API key");
            } catch (TranslationException e) {
                assertEquals(e.getMessage(), "invalid key");
            }
            assertEquals(server.getRequestCount(), 1);
        }
    }

    @Test
    public void translateRejectsMalformedTranslationContent() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/chat/completions");
            server.enqueue(new MockResponse.Builder().body(
                    "{\"choices\":[{\"message\":{\"content\":\"not json\"}}]}").build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    url.toString(), "test-model", "test-key", 1000, 0);

            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello"), TranslationLanguage.SPANISH));
        }
    }

    @Test
    public void translateRejectsEmptyInputWithoutSendingRequest() {
        OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                "https://example.invalid/chat/completions", "test-model", "test-key", 1000);

        assertThrows(
                TranslationException.class,
                () -> service.translate(List.of(), TranslationLanguage.SPANISH));
    }

    @Test
    public void translateRejectsNonHttpEndpoint() {
        OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                "ftp://example.invalid/chat/completions", "test-model", "test-key", 1000);

        assertThrows(
                TranslationException.class,
                () -> service.translate(List.of("hello"), TranslationLanguage.SPANISH));
    }

    @Test
    public void translateRejectsTooFewTranslations() throws Exception {
        assertResponseRejected("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"translations\\":[\\"hola\\"]}"
                      }
                    }
                  ]
                }
                """);
    }

    @Test
    public void translateRejectsTooManyTranslations() throws Exception {
        assertResponseRejected("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"translations\\":[\\"hola\\",\\"adiós\\",\\"hasta luego\\"]}"
                      }
                    }
                  ]
                }
                """);
    }

    @Test
    public void translateRejectsNonTextTranslations() throws Exception {
        assertResponseRejected("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"translations\\":[\\"hola\\", 42]}"
                      }
                    }
                  ]
                }
                """);
    }

    @Test
    public void translateRejectsEmptyTranslations() throws Exception {
        assertResponseRejected("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"translations\\":[\\"hola\\",\\"\\"]}"
                      }
                    }
                  ]
                }
                """);
    }

    @Test
    public void translateRejectsMalformedResponseBody() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder().body("not json").build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 1000);

            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello"), TranslationLanguage.SPANISH));
        }
    }

    @Test
    public void translateTimesOutSlowResponses() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder()
                    .body(TRANSLATION_RESPONSE)
                    .headersDelay(2, java.util.concurrent.TimeUnit.SECONDS)
                    .build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 100);

            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello"), TranslationLanguage.SPANISH));
            Thread.interrupted();
            assertEquals(server.getRequestCount(), 4);
        }
    }

    @Test
    public void translateHonorsNumericRetryAfterHeader() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder()
                    .code(429)
                    .setHeader("Retry-After", "0")
                    .body("rate limited")
                    .build());
            server.enqueue(new MockResponse.Builder().body(TRANSLATION_RESPONSE).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 1000, 1);

            assertEquals(
                    service.translate(List.of("hello", "goodbye"), TranslationLanguage.SPANISH),
                    List.of("hola", "adiós"));
            assertEquals(server.getRequestCount(), 2);
        }
    }

    @Test
    public void translateHonorsHttpDateRetryAfterHeader() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String retryAfter = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.withZone(
                    java.time.ZoneOffset.UTC).format(java.time.Instant.now());
            server.enqueue(new MockResponse.Builder()
                    .code(429)
                    .setHeader("Retry-After", retryAfter)
                    .body("rate limited")
                    .build());
            server.enqueue(new MockResponse.Builder().body(TRANSLATION_RESPONSE).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 1000, 1);

            assertEquals(
                    service.translate(List.of("hello", "goodbye"), TranslationLanguage.SPANISH),
                    List.of("hola", "adiós"));
            assertEquals(server.getRequestCount(), 2);
        }
    }

    @Test
    public void translateRetriesRateLimitWithoutRetryAfterHeader() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder()
                    .code(429)
                    .body("rate limited")
                    .build());
            server.enqueue(new MockResponse.Builder().body(TRANSLATION_RESPONSE).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 1000, 1);

            assertEquals(
                    service.translate(List.of("hello", "goodbye"), TranslationLanguage.SPANISH),
                    List.of("hola", "adiós"));
            assertEquals(server.getRequestCount(), 2);
        }
    }

    @Test
    public void translateRejectsMissingApiKey() {
        OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                "https://example.invalid/chat/completions", "test-model", "", 1000);

        assertThrows(
                TranslationException.class,
                () -> service.translate(List.of("hello"), TranslationLanguage.SPANISH));
    }

    private void assertResponseRejected(String responseBody) throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder().body(responseBody).build());

            OpenAiCompatibleTranslationService service = new OpenAiCompatibleTranslationService(
                    server.url("/chat/completions").toString(), "test-model", "test-key", 1000);

            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello", "goodbye"), TranslationLanguage.SPANISH));
            assertEquals(server.getRequestCount(), 1);
        }
    }
}
