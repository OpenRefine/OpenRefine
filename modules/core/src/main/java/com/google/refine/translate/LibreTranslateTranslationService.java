
package com.google.refine.translate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

public class LibreTranslateTranslationService implements TranslationService {

    public static final String DEFAULT_ENDPOINT = "http://localhost:5000/translate";

    private final String endpoint;
    private final Timeout requestTimeout;
    private final int maxRetries;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LibreTranslateTranslationService(String endpoint, int timeoutMillis) {
        this(endpoint, timeoutMillis, 3);
    }

    public LibreTranslateTranslationService(String endpoint, int timeoutMillis, int maxRetries) {
        this.endpoint = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint;
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }
        this.requestTimeout = Timeout.ofMilliseconds(timeoutMillis);
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Override
    public List<String> translate(
            List<String> texts,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage) throws TranslationException {
        if (texts == null || texts.isEmpty()) {
            throw new TranslationException("No text was provided for translation");
        }
        if (sourceLanguage == null) {
            throw new TranslationException("No source language was provided");
        }
        if (targetLanguage == null) {
            throw new TranslationException("No target language was provided");
        }
        validateEndpoint();

        List<String> translations = new ArrayList<>(texts.size());
        for (String text : texts) {
            translations.add(translateText(text, sourceLanguage, targetLanguage));
        }
        return translations;
    }

    private String translateText(
            String text,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage) throws TranslationException {
        ObjectNode requestBody = createRequestBody(text, sourceLanguage, targetLanguage);
        IOException lastException = null;
        int maxAttempts = maxRetries + 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeRequest(requestBody);
            } catch (TranslationException e) {
                throw e;
            } catch (IOException e) {
                lastException = e;
                if (attempt == maxAttempts) {
                    break;
                }
                sleepBeforeRetry(attempt);
            }
        }

        throw new TranslationException(
                "Translation request failed after " + maxAttempts + " attempts: " + lastException.getMessage(),
                lastException);
    }

    private void validateEndpoint() throws TranslationException {
        try {
            URI uri = new URI(endpoint);
            String scheme = uri.getScheme();
            if ((scheme == null || !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null) {
                throw new TranslationException("The translation endpoint must be an absolute HTTP or HTTPS URL");
            }
        } catch (URISyntaxException e) {
            throw new TranslationException("Invalid translation endpoint", e);
        }
    }

    private ObjectNode createRequestBody(
            String text,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("q", text);
        requestBody.put("source", sourceLanguage.getCode());
        requestBody.put("target", targetLanguage.getCode());
        requestBody.put("format", "text");
        return requestBody;
    }

    private String executeRequest(ObjectNode requestBody) throws IOException, TranslationException {
        HttpPost request = new HttpPost(endpoint);
        request.setConfig(RequestConfig.custom()
                .setResponseTimeout(requestTimeout)
                .setConnectTimeout(requestTimeout)
                .setConnectionRequestTimeout(requestTimeout)
                .build());
        request.setEntity(new StringEntity(
                objectMapper.writeValueAsString(requestBody),
                ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            if (statusCode < 200 || statusCode >= 300) {
                String message = "HTTP " + statusCode + " from the translation service";
                if (statusCode == 429 || statusCode >= 500) {
                    throw new RetryableIOException(message);
                }
                throw new TranslationException(message);
            }

            String responseBody = response.getEntity() == null
                    ? ""
                    : new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            return parseTranslation(responseBody);
        }
    }

    private String parseTranslation(String responseBody) throws TranslationException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode translation = root.path("translatedText");
            if (!translation.isTextual() || translation.asText().isBlank()) {
                throw new TranslationException("The translation service returned an empty response");
            }
            return translation.asText();
        } catch (IOException e) {
            throw new TranslationException("Unable to parse the translation response", e);
        }
    }

    private void sleepBeforeRetry(int attempt) throws TranslationException {
        try {
            Thread.sleep((long) Math.pow(2, attempt - 1) * 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation request was interrupted", e);
        }
    }

    private static class RetryableIOException extends IOException {

        private static final long serialVersionUID = 1L;

        private RetryableIOException(String message) {
            super(message);
        }
    }
}
