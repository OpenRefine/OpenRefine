
package com.google.refine.translate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.testng.annotations.Test;

public class LibreTranslateTranslationServiceTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void translateSendsSourceAndTargetLanguages() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder()
                    .body("{\"translatedText\":\"Hola\"}")
                    .build());
            server.enqueue(new MockResponse.Builder()
                    .body("{\"translatedText\":\"你好\"}")
                    .build());

            LibreTranslateTranslationService service = new LibreTranslateTranslationService(
                    server.url("/translate").toString(), 1000);

            assertEquals(
                    service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH),
                    List.of("Hola"));
            assertEquals(
                    service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.MANDARIN),
                    List.of("你好"));

            JsonNode spanishRequest = OBJECT_MAPPER.readTree(server.takeRequest().getBody().utf8());
            assertEquals(spanishRequest.get("q").asText(), "hello");
            assertEquals(spanishRequest.get("source").asText(), "en");
            assertEquals(spanishRequest.get("target").asText(), "es");
            assertEquals(spanishRequest.get("format").asText(), "text");

            JsonNode mandarinRequest = OBJECT_MAPPER.readTree(server.takeRequest().getBody().utf8());
            assertEquals(mandarinRequest.get("q").asText(), "hello");
            assertEquals(mandarinRequest.get("source").asText(), "en");
            assertEquals(mandarinRequest.get("target").asText(), "zh");
        }
    }

    @Test
    public void translateRetriesServerErrors() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder().code(500).body("server error").build());
            server.enqueue(new MockResponse.Builder()
                    .setHeader("Retry-After", "0")
                    .code(500)
                    .body("server error")
                    .build());
            server.enqueue(new MockResponse.Builder()
                    .body("{\"translatedText\":\"Hola\"}")
                    .build());

            LibreTranslateTranslationService service = new LibreTranslateTranslationService(
                    server.url("/translate").toString(), 1000, 2);

            assertEquals(
                    service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH),
                    List.of("Hola"));
            assertEquals(server.getRequestCount(), 3);
        }
    }

    @Test
    public void translateDoesNotRetryClientErrors() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder().code(400).body("bad request").build());

            LibreTranslateTranslationService service = new LibreTranslateTranslationService(
                    server.url("/translate").toString(), 1000, 3);

            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH));
            assertEquals(server.getRequestCount(), 1);
        }
    }

    @Test
    public void translateRejectsMalformedAndEmptyResponses() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder().body("not json").build());
            server.enqueue(new MockResponse.Builder().body("{\"translatedText\":\"\"}").build());

            LibreTranslateTranslationService service = new LibreTranslateTranslationService(
                    server.url("/translate").toString(), 1000);

            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH));
            assertThrows(
                    TranslationException.class,
                    () -> service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH));
            assertEquals(server.getRequestCount(), 2);
        }
    }

    @Test
    public void translateRejectsInvalidEndpoint() {
        LibreTranslateTranslationService service = new LibreTranslateTranslationService("ftp://localhost/translate", 1000);

        assertThrows(
                TranslationException.class,
                () -> service.translate(List.of("hello"), TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH));
    }
}
