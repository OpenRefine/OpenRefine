
package com.google.refine.translate;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

public class OpenAiCompatibleTranslationService implements TranslationService {

    public static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final Timeout requestTimeout;
    private final int maxRetries;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleTranslationService(
            String endpoint,
            String model,
            String apiKey,
            int timeoutMillis) {
        this(endpoint, model, apiKey, timeoutMillis, 3);
    }

    public OpenAiCompatibleTranslationService(
            String endpoint,
            String model,
            String apiKey,
            int timeoutMillis,
            int maxRetries) {
        this.endpoint = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint;
        this.model = model;
        this.apiKey = apiKey;
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }
        this.requestTimeout = Timeout.ofMilliseconds(timeoutMillis);
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Override
    public List<String> translate(List<String> texts, TranslationLanguage language) throws TranslationException {
        validateRequest(texts, language);

        ObjectNode requestBody = createRequestBody(texts, language);
        RetryableIOException lastIOException = null;
        String lastErrorMessage = null;

        int maxAttempts = maxRetries + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeRequest(requestBody, texts.size());
            } catch (TranslationException e) {
                throw e;
            } catch (SocketTimeoutException e) {
                lastIOException = new RetryableIOException("Translation request timed out", e, null);
                lastErrorMessage = "Translation request timed out";
                if (attempt == maxAttempts) {
                    break;
                }
                sleepBeforeRetry(attempt, lastIOException.retryAfterSeconds);
            } catch (IOException e) {
                lastIOException = e instanceof RetryableIOException ? (RetryableIOException) e
                        : new RetryableIOException(
                                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e, null);
                lastErrorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (attempt == maxAttempts) {
                    break;
                }
                sleepBeforeRetry(attempt, lastIOException.retryAfterSeconds);
            }
        }

        throw new TranslationException("Translation request failed after " + maxAttempts + " attempts: "
                + lastErrorMessage, lastIOException);
    }

    private void validateRequest(List<String> texts, TranslationLanguage language) throws TranslationException {
        if (texts == null || texts.isEmpty()) {
            throw new TranslationException("No text was provided for translation");
        }
        if (language == null) {
            throw new TranslationException("No target language was provided");
        }
        if (model == null || model.isBlank()) {
            throw new TranslationException("No translation model was provided");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new TranslationException("No API key was provided");
        }
        validateEndpoint();
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

    private ObjectNode createRequestBody(List<String> texts, TranslationLanguage language) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", 0);

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put(
                "content",
                "Translate every item in the JSON array into " + language.getDisplayName()
                        + ". Return only a JSON object with the key \"translations\" and an array with the same "
                        + "number of items in the same order. Do not add explanations.");

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        ArrayNode inputArray = userMessage.putArray("content");
        for (String text : texts) {
            inputArray.add(text);
        }

        return requestBody;
    }

    private List<String> executeRequest(ObjectNode requestBody, int expectedCount) throws IOException, TranslationException {
        HttpPost request = new HttpPost(endpoint);
        request.setConfig(RequestConfig.custom()
                .setResponseTimeout(requestTimeout)
                .setConnectTimeout(requestTimeout)
                .setConnectionRequestTimeout(requestTimeout)
                .build());
        request.setEntity(new StringEntity(
                objectMapper.writeValueAsString(requestBody),
                ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(1);
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableAutomaticRetries()
                .build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String retryAfter = response.getFirstHeader("Retry-After") == null
                    ? null
                    : response.getFirstHeader("Retry-After").getValue();
            String responseBody = response.getEntity() == null
                    ? ""
                    : new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            if (statusCode < 200 || statusCode >= 300) {
                String errorMessage = parseErrorMessage(responseBody, statusCode);
                if (isRetryableStatus(statusCode)) {
                    throw new RetryableIOException(errorMessage, null, retryAfter);
                }
                throw new TranslationException(errorMessage);
            }

            return parseTranslations(responseBody, expectedCount);
        } catch (SocketTimeoutException e) {
            throw new RetryableIOException("Translation request timed out", e, null);
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation request was interrupted", e);
        }
    }

    private List<String> parseTranslations(String responseBody, int expectedCount) throws TranslationException {
        List<String> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = extractMessageContent(root);
            if (content == null || content.isBlank()) {
                throw new TranslationException("The translation service returned an empty response");
            }

            JsonNode translations = objectMapper.readTree(content).path("translations");
            if (!translations.isArray() || translations.isEmpty()) {
                throw new TranslationException("The translation service did not return translations");
            }

            for (JsonNode translation : translations) {
                if (!translation.isTextual() || translation.asText().isEmpty() || translation.asText().isBlank()) {
                    throw new TranslationException("The translation service returned a non-text translation");
                }
                result.add(translation.asText());
            }
        } catch (IOException e) {
            throw new TranslationException("Unable to parse the translation response", e);
        }
        if (result.size() != expectedCount) {
            throw new TranslationException("The translation service returned the wrong number of translations");
        }
        return result;
    }

    private String extractMessageContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            if (content.isArray() && !content.isEmpty()) {
                JsonNode firstPart = content.get(0).path("text");
                if (firstPart.isTextual()) {
                    return firstPart.asText();
                }
            }
        }

        JsonNode output = root.path("output");
        if (output.isArray() && !output.isEmpty()) {
            JsonNode contentArray = output.get(0).path("content");
            if (contentArray.isArray() && !contentArray.isEmpty()) {
                JsonNode text = contentArray.get(0).path("text");
                if (text.isTextual()) {
                    return text.asText();
                }
            }
        }
        return null;
    }

    private String parseErrorMessage(String responseBody, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            if (error.isObject()) {
                JsonNode message = error.path("message");
                if (message.isTextual() && !message.asText().isBlank()) {
                    return message.asText();
                }
            }
        } catch (IOException e) {
            // Fall through to the generic HTTP message.
        }
        return "HTTP " + statusCode + " from the translation service";
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 409 || statusCode == 425 || statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry(int attempt, String retryAfter) throws TranslationException {
        long delayMillis = retryDelayMillis(retryAfter, attempt);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation request was interrupted", e);
        }
    }

    private long retryDelayMillis(String retryAfter, int attempt) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return (long) Math.pow(2, attempt - 1) * 1000;
        }

        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return Math.max(0, seconds * 1000);
        } catch (NumberFormatException e) {
            // Retry-After may also be an HTTP-date.
        }

        try {
            ZonedDateTime retryTime = ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
            return Math.max(0, retryTime.toInstant().toEpochMilli() - System.currentTimeMillis());
        } catch (DateTimeParseException e) {
            return (long) Math.pow(2, attempt - 1) * 1000;
        }
    }

    private static class RetryableIOException extends IOException {

        private static final long serialVersionUID = 4951937673938759265L;

        private final String retryAfterSeconds;

        private RetryableIOException(String message, Throwable cause, String retryAfterSeconds) {
            super(message, cause);
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
